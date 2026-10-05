import type { BandeiraCartao, FormaPagamentoPresencial, PagamentoPresencial } from "../../api/pdvApi.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro, type OpcaoSelecao } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { BANDEIRAS } from "./bandeirasCartao.js";

const CENTAVOS_POR_REAL = 100;
/** Igual ao limite do servidor (PartesDoPagamento.MAXIMO_DE_PARTES). */
const MAXIMO_DE_PARTES = 5;

const FORMAS_DA_PARTE: readonly OpcaoSelecao[] = [
  { valor: "DINHEIRO", rotulo: "Dinheiro" },
  { valor: "CARTAO_CREDITO", rotulo: "Crédito" },
  { valor: "CARTAO_DEBITO", rotulo: "Débito" },
  { valor: "PIX", rotulo: "Pix (maquininha)" },
];

const ROTULO_FORMA_DA_PARTE: Record<FormaPagamentoPresencial, string> = {
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Crédito",
  CARTAO_DEBITO: "Débito",
  PIX: "Pix (maquininha)",
};

export interface PartesPagamento {
  elemento: HTMLElement;
  /** Partes já recebidas, na ordem em que foram lançadas. */
  partes: () => readonly PagamentoPresencial[];
  soma: () => number;
  atualizarTotal: (total: number) => void;
  /** Avisa quando uma parte entra ou sai (o restante e as formas disponíveis mudam). */
  aoMudar: (ouvinte: () => void) => void;
}

/**
 * Pagamento dividido (D36): o operador lança o que o cliente já pagou (dinheiro, ou cartão/Pix na
 * maquininha avulsa com o comprovante), e o restante fecha a venda do jeito normal.
 */
export function criarPartesPagamento(): PartesPagamento {
  const partes: PagamentoPresencial[] = [];
  let totalAtual = 0;
  let ouvinte: () => void = () => undefined;

  const abrir = document.createElement("button");
  abrir.type = "button";
  abrir.className = "link-discreto";
  abrir.textContent = "Dividir em mais de uma forma";

  const titulo = document.createElement("p");
  titulo.className = "partes-pagamento__titulo";
  titulo.textContent = "Já recebido";
  const lista = document.createElement("ul");
  lista.className = "partes-pagamento__lista";

  const forma = criarCampoSelecao("parte-forma", "Forma", FORMAS_DA_PARTE);
  const valor = criarCampoTexto("parte-valor", "Valor (R$)", "number", false);
  valor.entrada.step = "0.01";
  valor.entrada.min = "0.01";
  valor.entrada.inputMode = "decimal";
  const bandeira = criarCampoSelecao("parte-bandeira", "Bandeira", BANDEIRAS);
  const autorizacao = criarCampoTexto("parte-autorizacao", "Autorização (comprovante)", "text", false);
  autorizacao.entrada.maxLength = 20;
  autorizacao.entrada.autocomplete = "off";
  const adicionar = document.createElement("button");
  adicionar.type = "button";
  adicionar.className = "btn btn-ghost btn-pequeno";
  adicionar.textContent = "Adicionar parte";
  const campos = document.createElement("div");
  campos.className = "partes-pagamento__campos";
  campos.append(forma.container, valor.container, bandeira.container, autorizacao.container);
  const erro = criarMensagemErro();

  const desfazer = document.createElement("button");
  desfazer.type = "button";
  desfazer.className = "link-discreto";
  desfazer.textContent = "Desfazer a divisão";
  const rodape = document.createElement("div");
  rodape.className = "partes-pagamento__rodape";
  rodape.append(adicionar, desfazer);

  const restante = document.createElement("p");
  restante.className = "partes-pagamento__restante";
  restante.setAttribute("aria-live", "polite");

  const bloco = document.createElement("div");
  bloco.className = "partes-pagamento";
  bloco.hidden = true;
  bloco.append(titulo, lista, campos, erro, rodape, restante);

  const elemento = document.createElement("div");
  elemento.append(abrir, bloco);

  const soma = (): number => emCentavos(partes.reduce((acumulado, parte) => acumulado + (parte.valor ?? 0), 0));
  const formaEscolhida = (): FormaPagamentoPresencial => {
    // Valor vem de uma lista fixa de opções (FORMAS_DA_PARTE), então sempre é uma forma válida.
    return forma.selecao.value as FormaPagamentoPresencial;
  };

  function atualizarCampos(): void {
    const escolhida = formaEscolhida();
    bandeira.container.hidden = escolhida !== "CARTAO_CREDITO" && escolhida !== "CARTAO_DEBITO";
    autorizacao.container.hidden = escolhida === "DINHEIRO";
  }

  function renderizar(): void {
    lista.replaceChildren(...partes.map((parte, indice) => criarItem(parte, () => {
      partes.splice(indice, 1);
      mudou();
    })));
    titulo.hidden = partes.length === 0;
    adicionar.disabled = partes.length >= MAXIMO_DE_PARTES;
    const falta = totalAtual - soma();
    restante.classList.toggle("partes-pagamento__restante--excedeu", partes.length > 0 && falta <= 0);
    if (partes.length === 0) {
      restante.textContent = "";
    } else if (falta <= 0) {
      restante.textContent = "As partes já pagam a venda inteira: tire uma parte ou diminua o valor.";
    } else {
      restante.textContent = `Falta pagar ${formatarMoeda(falta)} — escolha abaixo como o cliente paga o restante.`;
    }
  }

  function mudou(): void {
    renderizar();
    ouvinte();
  }

  function lerParte(): PagamentoPresencial | string {
    const quantia = emCentavos(Number(valor.entrada.value));
    if (valor.entrada.value === "" || !Number.isFinite(quantia) || quantia <= 0) {
      return "Digite o valor desta parte.";
    }
    if (emCentavos(soma() + quantia) >= totalAtual) {
      return `Esta parte precisa ser menor que ${formatarMoeda(emCentavos(totalAtual - soma()))}: o restante fecha a venda.`;
    }
    const escolhida = formaEscolhida();
    const codigo = autorizacao.entrada.value.trim();
    if (escolhida !== "DINHEIRO" && codigo === "") {
      return "Digite o código de autorização impresso no comprovante.";
    }
    return {
      forma: escolhida,
      valor: quantia,
      valorRecebido: null,
      // Valor vem de uma lista fixa de opções (BANDEIRAS), então sempre é uma bandeira válida.
      bandeira: bandeira.container.hidden ? null : bandeira.selecao.value as BandeiraCartao,
      codigoAutorizacao: escolhida === "DINHEIRO" ? null : codigo,
    };
  }

  abrir.addEventListener("click", () => {
    abrir.hidden = true;
    bloco.hidden = false;
    valor.entrada.focus();
  });
  desfazer.addEventListener("click", () => {
    partes.length = 0;
    erro.hidden = true;
    bloco.hidden = true;
    abrir.hidden = false;
    mudou();
  });
  forma.selecao.addEventListener("change", atualizarCampos);
  adicionar.addEventListener("click", () => {
    erro.hidden = true;
    const parte = lerParte();
    if (typeof parte === "string") {
      mostrarErro(erro, null, parte);
      return;
    }
    partes.push(parte);
    valor.entrada.value = "";
    autorizacao.entrada.value = "";
    mudou();
  });

  atualizarCampos();
  renderizar();

  return {
    elemento,
    partes: () => [...partes],
    soma,
    atualizarTotal: (total) => {
      totalAtual = total;
      renderizar();
    },
    aoMudar: (novoOuvinte) => {
      ouvinte = novoOuvinte;
    },
  };
}

function emCentavos(valor: number): number {
  return Math.round(valor * CENTAVOS_POR_REAL) / CENTAVOS_POR_REAL;
}

/** "Crédito · VISA · aut. 123456" — usado na lista das partes, no recibo e no detalhe do pedido. */
export function descreverParte(
  parte: Pick<PagamentoPresencial, "forma" | "bandeira" | "codigoAutorizacao">
): string {
  const detalhes = [ROTULO_FORMA_DA_PARTE[parte.forma]];
  if (parte.bandeira !== null) {
    detalhes.push(parte.bandeira.replace("_", " "));
  }
  if (parte.codigoAutorizacao !== null) {
    detalhes.push(`aut. ${parte.codigoAutorizacao}`);
  }
  return detalhes.join(" · ");
}

function criarItem(parte: PagamentoPresencial, aoTirar: () => void): HTMLLIElement {
  const descricao = document.createElement("span");
  descricao.textContent = descreverParte(parte);
  const valor = document.createElement("strong");
  valor.textContent = formatarMoeda(parte.valor ?? 0);
  const tirar = document.createElement("button");
  tirar.type = "button";
  tirar.className = "link-tabela";
  tirar.textContent = "tirar";
  tirar.addEventListener("click", aoTirar);
  const item = document.createElement("li");
  item.className = "partes-pagamento__item";
  item.append(descricao, valor, tirar);
  return item;
}
