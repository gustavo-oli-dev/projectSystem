import {
  buscarFundoPadrao,
  definirFundoPadrao,
  detalharCaixa,
  listarCaixasParaConferencia,
  type CedulaContada,
  type ResumoCaixa,
} from "../../api/caixaApi.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { contagemDe, criarContagemCedulas } from "./contagemCedulas.js";
import { criarResultadoFechamento } from "./resultadoFechamentoView.js";

const DATA_HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

/**
 * Conferência de caixa (gerente/financeiro): lista dos caixas com a diferença de cada um e o
 * fundo de troco padrão com que todo caixa abre.
 */
export function montarConferenciaCaixas(container: HTMLElement): void {
  const titulo = document.createElement("h1");
  titulo.textContent = "Conferência de caixa";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const areaFundo = document.createElement("div");
  areaFundo.className = "conferencia-caixas__fundo";
  const areaLista = document.createElement("div");
  container.replaceChildren(cabecalho, areaFundo, areaLista);

  void carregarFundoPadrao(areaFundo);
  void carregarLista(areaLista);
}

async function carregarFundoPadrao(area: HTMLElement): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando o fundo de troco..."));
  try {
    area.replaceChildren(criarEditorFundo(await buscarFundoPadrao()));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar o fundo de troco padrão.", "erro"));
  }
}

/** Recolhido: muda pouco, mas fica à mão. O resumo mostra o total sem precisar abrir. */
function criarEditorFundo(fundoAtual: readonly CedulaContada[]): HTMLElement {
  const resumo = document.createElement("summary");
  const descreverResumo = (total: number): void => {
    resumo.textContent = total > 0
      ? `Fundo de troco padrão: ${formatarMoeda(total)} por caixa`
      : "Fundo de troco padrão: não definido";
  };
  descreverResumo(fundoAtual.reduce((soma, cedula) => soma + cedula.subtotal, 0));

  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = "Quantas notas e moedas cada caixa recebe ao abrir. Na abertura, o operador confere e corrige se a gaveta vier diferente.";
  const contagem = criarContagemCedulas(contagemDe(fundoAtual));
  const erro = criarMensagemErro();
  const confirmacao = document.createElement("p");
  confirmacao.className = "caixa-painel__confirmacao";
  confirmacao.setAttribute("role", "status");
  const salvar = document.createElement("button");
  salvar.type = "submit";
  salvar.className = "btn btn-primary";
  salvar.textContent = "Salvar fundo padrão";
  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(confirmacao, salvar);

  const formulario = document.createElement("form");
  formulario.className = "caixa-painel caixa-painel--embutido";
  formulario.append(instrucao, contagem.elemento, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    confirmacao.textContent = "";
    salvar.disabled = true;
    definirFundoPadrao(contagem.contagem())
      .then((salvo) => {
        descreverResumo(salvo.reduce((soma, cedula) => soma + cedula.subtotal, 0));
        confirmacao.textContent = "Fundo padrão salvo.";
      })
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível salvar o fundo padrão."))
      .finally(() => {
        salvar.disabled = false;
      });
  });

  const detalhes = document.createElement("details");
  detalhes.className = "secao-recolhivel";
  detalhes.append(resumo, formulario);
  return detalhes;
}

async function carregarLista(area: HTMLElement): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando os caixas..."));
  try {
    const caixas = await listarCaixasParaConferencia();
    if (caixas.length === 0) {
      area.replaceChildren(cartaoEstado("Nenhum caixa foi aberto ainda."));
      return;
    }
    area.replaceChildren(criarTabela(
      ["Operador", "Abertura", "Fechamento", "Fundo", "Vendas em dinheiro", "Esperado", "Contado", "Resultado", ""],
      caixas.map((caixa) => criarLinhaCaixa(caixa, () => void mostrarDetalhe(area, caixa.id))),
      "caixa(s)"
    ));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar os caixas.", "erro"));
  }
}

function criarLinhaCaixa(caixa: ResumoCaixa, aoAbrir: () => void): HTMLTableRowElement {
  const ver = document.createElement("button");
  ver.type = "button";
  ver.className = "btn btn-ghost btn-pequeno";
  ver.textContent = "Ver conferência";
  ver.addEventListener("click", aoAbrir);
  return criarLinha(
    celula(caixa.operadorNome),
    celula(DATA_HORA.format(new Date(caixa.abertaEm))),
    celula(caixa.fechadaEm === null ? "—" : DATA_HORA.format(new Date(caixa.fechadaEm))),
    celula(formatarMoeda(caixa.fundoInicial)),
    celula(formatarMoeda(caixa.vendasEmDinheiro)),
    celula(formatarMoeda(caixa.valorEsperado)),
    celula(caixa.valorContado === null ? "—" : formatarMoeda(caixa.valorContado)),
    celulaResultado(caixa.diferenca),
    celulaComConteudo(ver)
  );
}

function celulaResultado(diferenca: number | null): HTMLTableCellElement {
  if (diferenca === null) {
    return celulaSelo("Aberto", "aberta");
  }
  if (diferenca === 0) {
    return celulaSelo("Bateu", "concluido");
  }
  return diferenca > 0
    ? celulaSelo(`Sobra ${formatarMoeda(diferenca)}`, "pendente")
    : celulaSelo(`Falta ${formatarMoeda(-diferenca)}`, "rejeitado");
}

async function mostrarDetalhe(area: HTMLElement, caixaId: string): Promise<void> {
  const voltar = document.createElement("button");
  voltar.type = "button";
  voltar.className = "btn btn-ghost btn-pequeno";
  voltar.textContent = "← Voltar para a lista";
  voltar.addEventListener("click", () => void carregarLista(area));

  area.replaceChildren(voltar, elementoCarregando("Carregando a conferência..."));
  try {
    const conferencia = await detalharCaixa(caixaId);
    const titulo = document.createElement("h2");
    titulo.textContent = `Caixa de ${conferencia.operadorNome} · ${DATA_HORA.format(new Date(conferencia.abertaEm))}`;
    const painel = document.createElement("div");
    painel.className = "caixa-painel";
    painel.append(titulo, criarResultadoFechamento(conferencia));
    area.replaceChildren(voltar, painel);
  } catch {
    area.replaceChildren(voltar, cartaoEstado("Não foi possível carregar a conferência deste caixa.", "erro"));
  }
}
