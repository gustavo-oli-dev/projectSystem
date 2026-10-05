import {
  cancelarConta,
  lancarContaAPagar,
  listarContasAPagar,
  listarContatos,
  pagarConta,
  type ContaAPagar,
  type Contato,
} from "../../api/comprasApi.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";

type Filtro = "abertas" | "vencidas" | "pagas" | "todas";

const FILTROS: ReadonlyArray<{ filtro: Filtro; rotulo: string }> = [
  { filtro: "abertas", rotulo: "A pagar" },
  { filtro: "vencidas", rotulo: "Vencidas" },
  { filtro: "pagas", rotulo: "Pagas" },
  { filtro: "todas", rotulo: "Todas" },
];
const DIAS_PARA_VENCER = 7;
const UM_DIA_EM_MS = 24 * 60 * 60 * 1000;
const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });
const SEM_CONTATO = "";

/**
 * Contas a pagar (D34): parcelas das notas de fornecedor (lançadas pela entrada por XML) e
 * despesas lançadas à mão. Quem tem CONTAS_PAGAR_GERENCIAR lança, paga e cancela.
 */
export async function montarContasAPagar(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Contas a pagar";
  const lancar = document.createElement("button");
  lancar.type = "button";
  lancar.className = "btn btn-primary";
  lancar.textContent = "Lançar conta";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, lancar);
  const formulario = document.createElement("div");
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, formulario, area);

  let filtro: Filtro = "abertas";
  const recarregar = async (): Promise<void> => {
    area.replaceChildren(elementoCarregando("Carregando contas..."));
    try {
      const contas = await listarContasAPagar();
      area.replaceChildren(criarResumo(contas), criarFiltros(filtro, (novo) => {
        filtro = novo;
        void recarregar();
      }), criarLista(contas.filter((conta) => passaNoFiltro(conta, filtro)), () => void recarregar()));
    } catch {
      area.replaceChildren(cartaoEstado("Não foi possível carregar as contas a pagar.", "erro"));
    }
  };
  lancar.addEventListener("click", () => {
    lancar.hidden = true;
    void abrirFormulario(formulario, () => {
      formulario.replaceChildren();
      lancar.hidden = false;
      void recarregar();
    });
  });
  await recarregar();
}

function passaNoFiltro(conta: ContaAPagar, filtro: Filtro): boolean {
  switch (filtro) {
    case "abertas":
      return conta.status === "ABERTA";
    case "vencidas":
      return conta.vencida;
    case "pagas":
      return conta.status === "PAGA";
    case "todas":
      return true;
  }
}

/** Os números que importam: o que venceu, o que vence logo, o total em aberto, o pago no mês. */
function criarResumo(contas: readonly ContaAPagar[]): HTMLElement {
  const hoje = new Date();
  const limite = new Date(hoje.getTime() + DIAS_PARA_VENCER * UM_DIA_EM_MS);
  const abertas = contas.filter((conta) => conta.status === "ABERTA");
  const vencidas = abertas.filter((conta) => conta.vencida);
  const vencendo = abertas.filter((conta) => !conta.vencida && dataDe(conta.vencimento) <= limite);
  const pagasNoMes = contas.filter((conta) => conta.pagaEm !== null
    && new Date(conta.pagaEm).getMonth() === hoje.getMonth() && new Date(conta.pagaEm).getFullYear() === hoje.getFullYear());
  const indicadores: Array<[string, number, string]> = [
    ["Vencidas", soma(vencidas), `${formatarInteiro(vencidas.length)} conta(s)`],
    [`Vencem em ${DIAS_PARA_VENCER} dias`, soma(vencendo), `${formatarInteiro(vencendo.length)} conta(s)`],
    ["Total a pagar", soma(abertas), `${formatarInteiro(abertas.length)} conta(s) em aberto`],
    ["Pago este mês", soma(pagasNoMes), `${formatarInteiro(pagasNoMes.length)} conta(s)`],
  ];
  const grade = document.createElement("div");
  grade.className = "indicadores contas__indicadores";
  grade.append(...indicadores.map(([rotulo, valor, nota]) => {
    const elementoRotulo = document.createElement("p");
    elementoRotulo.className = "indicador__rotulo";
    elementoRotulo.textContent = rotulo;
    const elementoValor = document.createElement("p");
    elementoValor.className = "indicador__valor";
    elementoValor.textContent = formatarMoeda(valor);
    const elementoNota = document.createElement("p");
    elementoNota.className = "indicador__nota";
    elementoNota.textContent = nota;
    const cartao = document.createElement("article");
    cartao.className = "indicador";
    cartao.append(elementoRotulo, elementoValor, elementoNota);
    return cartao;
  }));
  return grade;
}

function criarFiltros(ativo: Filtro, aoMudar: (filtro: Filtro) => void): HTMLElement {
  const grupo = document.createElement("div");
  grupo.className = "filtro-periodo__atalhos contas__filtros";
  grupo.setAttribute("role", "group");
  grupo.setAttribute("aria-label", "Mostrar");
  grupo.append(...FILTROS.map(({ filtro, rotulo }) => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "filtro-periodo__atalho";
    botao.textContent = rotulo;
    botao.setAttribute("aria-pressed", String(filtro === ativo));
    botao.addEventListener("click", () => aoMudar(filtro));
    return botao;
  }));
  return grupo;
}

function criarLista(contas: readonly ContaAPagar[], aoMudar: () => void): HTMLElement {
  if (contas.length === 0) {
    return cartaoEstado("Nenhuma conta aqui.");
  }
  return criarTabela(
    ["Vencimento", "Descrição", "Fornecedor / contato", "Valor", "Situação", ""],
    contas.map((conta) => criarLinha(
      celula(DATA.format(dataDe(conta.vencimento))),
      celula(conta.descricao),
      celula(conta.contatoNome ?? "—"),
      celula(formatarMoeda(conta.valor)),
      celulaSituacao(conta),
      celulaComConteudo(criarAcoes(conta, aoMudar))
    )),
    "conta(s)"
  );
}

function celulaSituacao(conta: ContaAPagar): HTMLTableCellElement {
  if (conta.status === "PAGA") {
    return celulaSelo(`Paga em ${conta.pagaEm === null ? "—" : DATA.format(new Date(conta.pagaEm))}`, "concluido");
  }
  if (conta.status === "CANCELADA") {
    return celulaSelo("Cancelada", "cancelado");
  }
  return conta.vencida ? celulaSelo("Vencida", "rejeitado") : celulaSelo("A pagar", "pendente");
}

function criarAcoes(conta: ContaAPagar, aoMudar: () => void): HTMLElement {
  const grupo = document.createElement("div");
  grupo.className = "contas__acoes";
  if (conta.status !== "ABERTA") {
    return grupo;
  }
  const pagar = document.createElement("button");
  pagar.type = "button";
  pagar.className = "btn btn-ghost btn-pequeno";
  pagar.textContent = "Marcar como paga";
  pagar.addEventListener("click", () => {
    pagar.disabled = true;
    pagarConta(conta.id).then(aoMudar).catch((falha: unknown) => {
      avisarFalha(falha, "Não foi possível marcar a conta como paga.");
      pagar.disabled = false;
    });
  });
  const cancelar = document.createElement("button");
  cancelar.type = "button";
  cancelar.className = "btn btn-perigo btn-pequeno";
  cancelar.textContent = "Cancelar";
  cancelar.addEventListener("click", () => {
    if (!window.confirm(`Cancelar a conta "${conta.descricao}"?`)) {
      return;
    }
    cancelarConta(conta.id).then(aoMudar).catch((falha: unknown) => avisarFalha(falha, "Não foi possível cancelar a conta."));
  });
  grupo.append(pagar, cancelar);
  return grupo;
}

async function abrirFormulario(area: HTMLElement, aoTerminar: () => void): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando contatos..."));
  let contatos: Contato[] = [];
  try {
    contatos = (await listarContatos()).filter((contato) => contato.ativo);
  } catch {
    contatos = [];
  }
  const descricao = criarCampoTexto("conta-descricao", "Descrição (ex.: Conta de luz de outubro)", "text", true);
  descricao.entrada.maxLength = 200;
  const valor = criarCampoTexto("conta-valor", "Valor (R$)", "number", true);
  valor.entrada.min = "0.01";
  valor.entrada.step = "0.01";
  const vencimento = criarCampoTexto("conta-vencimento", "Vencimento", "date", true);
  const contato = criarCampoSelecao("conta-contato", "Fornecedor / contato (opcional)", [
    { valor: SEM_CONTATO, rotulo: "— Sem contato —" },
    ...contatos.map((opcao) => ({ valor: opcao.id, rotulo: opcao.nome })),
  ]);
  contato.selecao.required = false;

  const titulo = document.createElement("h2");
  titulo.textContent = "Lançar conta a pagar";
  const erro = criarMensagemErro();
  const salvar = document.createElement("button");
  salvar.type = "submit";
  salvar.className = "btn btn-primary";
  salvar.textContent = "Lançar";
  const voltar = document.createElement("button");
  voltar.type = "button";
  voltar.className = "btn btn-ghost";
  voltar.textContent = "Voltar";
  voltar.addEventListener("click", aoTerminar);
  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(voltar, salvar);

  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao contas__formulario";
  formulario.append(titulo, descricao.container, valor.container, vencimento.container, contato.container, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    salvar.disabled = true;
    lancarContaAPagar({
      contatoId: contato.selecao.value === SEM_CONTATO ? null : contato.selecao.value,
      descricao: descricao.entrada.value.trim(),
      valor: Number(valor.entrada.value),
      vencimento: vencimento.entrada.value,
    })
      .then(aoTerminar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível lançar a conta.");
        salvar.disabled = false;
      });
  });
  area.replaceChildren(formulario);
  queueMicrotask(() => descricao.entrada.focus());
}

/** Falha numa ação da linha (pagar/cancelar): avisa em vez de engolir o erro. */
function avisarFalha(falha: unknown, mensagemPadrao: string): void {
  window.alert(falha instanceof Error && falha.message !== "" ? falha.message : mensagemPadrao);
}

function soma(contas: readonly ContaAPagar[]): number {
  return contas.reduce((total, conta) => total + conta.valor, 0);
}

/** "2026-10-10" como data local (meio-dia evita escorregar de dia pelo fuso). */
function dataDe(iso: string): Date {
  return new Date(`${iso}T12:00:00`);
}
