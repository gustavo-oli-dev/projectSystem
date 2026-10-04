import { listarCaixasAbertos, type CaixaAberto } from "../../api/caixaApi.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { criarAberturaCaixa } from "./aberturaCaixaView.js";
import {
  botao,
  criarPainelFechamento,
  criarPainelReposicao,
  criarPainelSangria,
  criarTelaCaixaFechado,
} from "./operacaoCaixaView.js";

const HORA = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

/**
 * Gestão de caixa (CAIXA_GERENCIAR), separada da venda: abrir o caixa de um operador, repor troco,
 * fazer sangria e fechar. Cada ação abre no lugar da lista; "Voltar para os caixas" retorna.
 */
export function montarGestaoCaixa(container: HTMLElement): void {
  const titulo = document.createElement("h1");
  titulo.textContent = "Gestão de caixa";
  const abrir = botao("Abrir caixa", "btn btn-primary", () => mostrarAbertura());
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, abrir);

  const area = document.createElement("div");
  container.replaceChildren(cabecalho, area);

  const mostrarLista = (): void => {
    abrir.hidden = false;
    void carregarCaixas(area, {
      aoRepor: (caixa) => mostrarPainel(criarPainelReposicao(caixa, mostrarLista, mostrarLista)),
      aoSangria: (caixa) => mostrarPainel(criarPainelSangria(caixa, mostrarLista, mostrarLista)),
      aoFechar: (caixa) => mostrarPainel(criarPainelFechamento(caixa,
        (conferencia) => mostrarPainel(criarTelaCaixaFechado(conferencia, mostrarLista)), mostrarLista)),
    });
  };
  const mostrarPainel = (painel: HTMLElement): void => {
    abrir.hidden = true;
    area.replaceChildren(painel);
  };
  const mostrarAbertura = (): void => mostrarPainel(criarAberturaCaixa(mostrarLista, mostrarLista));

  mostrarLista();
}

interface AcoesDosCaixas {
  aoRepor: (caixa: CaixaAberto) => void;
  aoSangria: (caixa: CaixaAberto) => void;
  aoFechar: (caixa: CaixaAberto) => void;
}

async function carregarCaixas(area: HTMLElement, acoes: AcoesDosCaixas): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando os caixas abertos..."));
  try {
    const caixas = await listarCaixasAbertos();
    if (caixas.length === 0) {
      area.replaceChildren(cartaoEstado("Nenhum caixa aberto agora. Use \"Abrir caixa\" para liberar um operador para vender."));
      return;
    }
    const grade = document.createElement("div");
    grade.className = "caixas-abertos";
    grade.append(...caixas.map((caixa) => criarCartaoCaixa(caixa, acoes)));
    area.replaceChildren(grade);
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar os caixas abertos.", "erro"));
  }
}

/** Um caixa aberto: de quem é, desde quando, quem abriu e o que já entrou/saiu fora das vendas. */
function criarCartaoCaixa(caixa: CaixaAberto, acoes: AcoesDosCaixas): HTMLElement {
  const nome = document.createElement("h2");
  nome.textContent = caixa.operadorNome;
  const situacao = document.createElement("p");
  situacao.className = "caixa-aberto__situacao";
  const marcador = document.createElement("span");
  marcador.className = "barra-caixa__marcador";
  marcador.setAttribute("aria-hidden", "true");
  const textoSituacao = document.createElement("span");
  textoSituacao.textContent = `Aberto às ${HORA.format(new Date(caixa.abertaEm))} por ${caixa.abertaPorNome}`;
  situacao.append(marcador, textoSituacao);

  const dados = document.createElement("dl");
  dados.className = "caixa-aberto__dados";
  for (const [rotulo, valor] of [
    ["Fundo de troco", formatarMoeda(caixa.fundoInicial)],
    ["Reposições", formatarMoeda(caixa.totalSuprimentos)],
    ["Sangrias", formatarMoeda(caixa.totalSangrias)],
  ] as const) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    const linha = document.createElement("div");
    linha.append(termo, definicao);
    dados.append(linha);
  }

  const botoes = document.createElement("div");
  botoes.className = "caixa-aberto__acoes";
  botoes.append(
    botao("Reposição de troco", "btn btn-ghost btn-pequeno", () => acoes.aoRepor(caixa)),
    botao("Sangria", "btn btn-ghost btn-pequeno", () => acoes.aoSangria(caixa)),
    botao("Fechar caixa", "btn btn-ghost btn-pequeno", () => acoes.aoFechar(caixa))
  );

  const cartao = document.createElement("section");
  cartao.className = "caixa-aberto";
  cartao.append(nome, situacao, dados, botoes);
  return cartao;
}
