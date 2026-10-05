import { conferirDinheiroDoDia, type DinheiroDoDia, type ResultadoFechamento } from "../../api/relatoriosApi.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { celula, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { deIso, paraIso } from "./periodoPainel.js";
import { criarCartao } from "./secoesRelatorio.js";

const DIA_COMPLETO = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
const HORA = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });
const UM_DIA_EM_MS = 24 * 60 * 60 * 1000;

/**
 * Aba "Dinheiro do dia" (D31): o gerente escolhe o dia e confere o dinheiro das gavetas com o que o
 * sistema registrou. O sistema sabe o valor dos produtos vendidos em dinheiro; o contado, menos o
 * valor inicial (e reposições/sangrias), tem que dar exatamente esse valor.
 */
export function montarAbaDinheiroDoDia(): HTMLElement[] {
  const area = document.createElement("div");
  area.className = "painel-relatorio";
  const seletor = criarSeletorDeDia((dia) => void carregar(area, dia));
  void carregar(area, paraIso(new Date()));
  return [seletor, area];
}

async function carregar(area: HTMLElement, dia: string): Promise<void> {
  area.replaceChildren(elementoCarregando("Conferindo o dinheiro do dia..."));
  try {
    area.replaceChildren(...montarConferencia(await conferirDinheiroDoDia(dia)));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível conferir o dinheiro deste dia.", "erro"));
  }
}

/** "◀ dia anterior · [data] · próximo dia ▶" — a conferência é sempre de um dia só. */
function criarSeletorDeDia(aoMudar: (dia: string) => void): HTMLElement {
  const data = document.createElement("input");
  data.type = "date";
  data.value = paraIso(new Date());
  data.max = paraIso(new Date());
  data.setAttribute("aria-label", "Dia da conferência");
  data.addEventListener("change", () => {
    if (data.value !== "") {
      aoMudar(data.value);
    }
  });
  const andar = (dias: number): void => {
    const novo = paraIso(new Date(deIso(data.value).getTime() + dias * UM_DIA_EM_MS));
    if (novo > data.max) {
      return;
    }
    data.value = novo;
    aoMudar(novo);
  };
  const anterior = criarBotao("◀ Dia anterior", () => andar(-1));
  const proximo = criarBotao("Próximo dia ▶", () => andar(1));
  const hoje = criarBotao("Hoje", () => {
    data.value = paraIso(new Date());
    aoMudar(data.value);
  });

  const explicacao = document.createElement("p");
  explicacao.className = "dinheiro-do-dia__explicacao";
  explicacao.textContent = "O sistema já sabe quanto foi vendido em dinheiro. O dinheiro contado nas gavetas, tirando o valor inicial, tem que dar esse mesmo valor — assim nenhum real se perde.";

  const controles = document.createElement("div");
  controles.className = "filtro-periodo__datas";
  controles.append(anterior, data, proximo, hoje);
  const barra = document.createElement("div");
  barra.className = "filtro-periodo";
  barra.append(explicacao, controles);
  return barra;
}

function montarConferencia(dia: DinheiroDoDia): HTMLElement[] {
  const titulo = document.createElement("h2");
  titulo.className = "dinheiro-do-dia__titulo";
  titulo.textContent = primeiraMaiuscula(DIA_COMPLETO.format(deIso(dia.dia)));
  if (dia.caixas.length === 0) {
    return [titulo, cartaoEstado("Nenhum caixa foi aberto neste dia.")];
  }
  return [titulo, criarVeredito(dia), criarConta(dia), criarTabelaCaixas(dia), criarTabelaProdutos(dia)];
}

/** O veredito do dia, em palavras e cor (nunca só a cor). */
function criarVeredito(dia: DinheiroDoDia): HTMLElement {
  const veredito = document.createElement("p");
  veredito.className = "resultado-caixa__situacao";
  const abertos = dia.caixasAbertos > 0
    ? ` · ${formatarInteiro(dia.caixasAbertos)} caixa(s) ainda aberto(s) — entram na conta quando fecharem`
    : "";
  if (dia.resultado === null) {
    veredito.textContent = `Nenhum caixa fechado ainda neste dia${abertos}`;
    return veredito;
  }
  const textos: Record<Exclude<ResultadoFechamento, "ABERTO">, [string, string]> = {
    BATEU: ["resultado-caixa__situacao--bateu", "✓ Dia certo — o dinheiro das gavetas bate com o vendido em dinheiro"],
    SOBROU: ["resultado-caixa__situacao--sobra", `▲ Dia sobrando ${formatarMoeda(dia.diferenca)}`],
    FALTOU: ["resultado-caixa__situacao--falta", `▼ Dia devendo ${formatarMoeda(-dia.diferenca)} — entrou menos dinheiro do que foi vendido`],
  };
  const [classe, texto] = dia.resultado === "ABERTO" ? textos.BATEU : textos[dia.resultado];
  veredito.classList.add(classe);
  veredito.textContent = texto + abertos;
  return veredito;
}

function criarConta(dia: DinheiroDoDia): HTMLElement {
  const linhas: Array<[string, string, boolean]> = [
    ["Vendido em dinheiro (registrado no sistema)", formatarMoeda(dia.vendidoEmDinheiro), false],
    ["Entrou nas gavetas (contado − valor inicial − reposições + sangrias)", formatarMoeda(dia.entrouNasGavetas), false],
    ["Resultado", textoResultado(dia.diferenca), true],
  ];
  const lista = document.createElement("dl");
  lista.className = "resultado-caixa__conta";
  for (const [rotulo, valor, forte] of linhas) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    const linha = document.createElement("div");
    linha.className = forte ? "resultado-caixa__linha resultado-caixa__linha--forte" : "resultado-caixa__linha";
    linha.append(termo, definicao);
    lista.append(linha);
  }
  return criarCartao("Sistema × gavetas (caixas fechados)", lista);
}

function criarTabelaCaixas(dia: DinheiroDoDia): HTMLElement {
  const tabela = criarTabela(
    ["Caixa", "Operador", "Horário", "Valor inicial", "Contado", "Entrou na gaveta", "Vendido em dinheiro", "Resultado"],
    dia.caixas.map((caixa) => criarLinha(
      celula(caixa.pontoNome),
      celula(caixa.operadorNome),
      celula(`${HORA.format(new Date(caixa.abertaEm))} – ${caixa.fechadaEm === null ? "aberto" : HORA.format(new Date(caixa.fechadaEm))}`),
      celula(formatarMoeda(caixa.valorInicial)),
      celula(valorOuTraco(caixa.contado)),
      celula(valorOuTraco(caixa.entrouNaGaveta)),
      celula(valorOuTraco(caixa.vendidoEmDinheiro)),
      celulaResultado(caixa.resultado, caixa.diferenca)
    )),
    "caixa(s)"
  );
  return criarCartao("Cada caixa do dia", tabela);
}

/** A "prova" do valor: o que o sistema registrou como vendido em dinheiro, produto por produto. */
function criarTabelaProdutos(dia: DinheiroDoDia): HTMLElement {
  if (dia.produtosEmDinheiro.length === 0) {
    return criarCartao("Produtos vendidos em dinheiro", cartaoEstado("Nenhum produto vendido em dinheiro neste dia."));
  }
  const tabela = criarTabela(["Produto", "Quantidade", "Valor"], dia.produtosEmDinheiro.map((produto) => criarLinha(
    celula(produto.descricao),
    celula(formatarInteiro(produto.quantidade)),
    celula(formatarMoeda(produto.valor))
  )), "produto(s)");
  return criarCartao("Produtos vendidos em dinheiro", tabela);
}

function celulaResultado(resultado: ResultadoFechamento, diferenca: number | null): HTMLTableCellElement {
  if (resultado === "ABERTO" || diferenca === null) {
    return celulaSelo("Aberto", "aberta");
  }
  if (diferenca === 0) {
    return celulaSelo("Certo", "concluido");
  }
  return diferenca > 0
    ? celulaSelo(`Sobrando ${formatarMoeda(diferenca)}`, "pendente")
    : celulaSelo(`Devendo ${formatarMoeda(-diferenca)}`, "rejeitado");
}

function textoResultado(diferenca: number): string {
  if (diferenca === 0) {
    return "Certo";
  }
  return diferenca > 0 ? `Sobrando ${formatarMoeda(diferenca)}` : `Devendo ${formatarMoeda(-diferenca)}`;
}

function valorOuTraco(valor: number | null): string {
  return valor === null ? "—" : formatarMoeda(valor);
}

function primeiraMaiuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function criarBotao(rotulo: string, aoClicar: () => void): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-ghost btn-pequeno";
  botao.textContent = rotulo;
  botao.addEventListener("click", aoClicar);
  return botao;
}
