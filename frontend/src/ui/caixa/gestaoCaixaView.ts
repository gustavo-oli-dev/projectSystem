import {
  cadastrarPontoDeCaixa,
  definirPontoDeCaixaAtivo,
  listarCaixasAbertos,
  listarPontosDeCaixa,
  type CaixaAberto,
  type PontoCaixa,
} from "../../api/caixaApi.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
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
  const abrir = botao("Abrir caixas", "btn btn-primary", () => mostrarAbertura());
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, abrir);

  const areaPontos = document.createElement("div");
  areaPontos.className = "gestao-caixa__pontos";
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, areaPontos, area);

  const mostrarLista = (): void => {
    abrir.hidden = false;
    areaPontos.hidden = false;
    void carregarPontos(areaPontos);
    void carregarCaixas(area, {
      aoRepor: (caixa) => mostrarPainel(criarPainelReposicao(caixa, mostrarLista, mostrarLista)),
      aoSangria: (caixa) => mostrarPainel(criarPainelSangria(caixa, mostrarLista, mostrarLista)),
      aoFechar: (caixa) => mostrarPainel(criarPainelFechamento(caixa,
        (conferencia) => mostrarPainel(criarTelaCaixaFechado(conferencia, mostrarLista)), mostrarLista)),
    });
  };
  const mostrarPainel = (painel: HTMLElement): void => {
    abrir.hidden = true;
    areaPontos.hidden = true;
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
      area.replaceChildren(cartaoEstado("Nenhum caixa aberto agora. Use \"Abrir caixas\" para liberar os operadores para vender."));
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
  nome.textContent = `${caixa.pontoNome} · ${caixa.operadorNome}`;
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

/** "Caixas da loja": os caixas físicos (Caixa 01, 02...), recolhido — muda pouco, mas fica à mão. */
async function carregarPontos(area: HTMLElement): Promise<void> {
  try {
    area.replaceChildren(criarSecaoPontos(await listarPontosDeCaixa(), () => void carregarPontos(area)));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar os caixas da loja.", "erro"));
  }
}

function criarSecaoPontos(pontos: readonly PontoCaixa[], aoMudar: () => void): HTMLElement {
  const ativos = pontos.filter((ponto) => ponto.ativo).length;
  const resumo = document.createElement("summary");
  resumo.textContent = pontos.length === 0
    ? "Caixas da loja: nenhum cadastrado — cadastre o Caixa 01 para começar"
    : `Caixas da loja: ${ativos} em uso`;

  const erro = criarMensagemErro();
  const lista = document.createElement("ul");
  lista.className = "pontos-caixa__lista";
  lista.append(...pontos.map((ponto) => criarLinhaPonto(ponto, aoMudar, erro)));

  const corpo = document.createElement("div");
  corpo.className = "pontos-caixa";
  if (pontos.length > 0) {
    corpo.append(lista);
  }
  corpo.append(criarCadastroPonto(pontos, aoMudar, erro), erro);

  const detalhes = document.createElement("details");
  detalhes.className = "secao-recolhivel";
  detalhes.open = pontos.length === 0;
  detalhes.append(resumo, corpo);
  return detalhes;
}

/** "Caixa 01 · [Livre] ............ [Desativar]". Caixa aberto não é desativado no meio do turno. */
function criarLinhaPonto(ponto: PontoCaixa, aoMudar: () => void, erro: HTMLElement): HTMLLIElement {
  const nome = document.createElement("strong");
  nome.className = "pontos-caixa__nome";
  nome.textContent = ponto.nome;
  const [textoSituacao, modificador] = !ponto.ativo ? ["Desativado", "inativo"]
    : ponto.aberto ? ["Aberto agora", "aberta"] : ["Livre", "ativo"];
  const situacao = document.createElement("span");
  situacao.className = `selo selo--${modificador}`;
  situacao.textContent = textoSituacao;

  const item = document.createElement("li");
  item.className = "pontos-caixa__item";
  item.append(nome, situacao);
  if (!ponto.aberto) {
    item.append(botao(ponto.ativo ? "Desativar" : "Reativar", "btn btn-ghost btn-pequeno", () => {
      definirPontoDeCaixaAtivo(ponto.id, !ponto.ativo).then(aoMudar)
        .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível alterar o caixa."));
    }));
  }
  return item;
}

/** "Novo caixa nº [4] [Cadastrar caixa]" numa linha só (já sugere o próximo número). */
function criarCadastroPonto(pontos: readonly PontoCaixa[], aoMudar: () => void, erro: HTMLElement): HTMLElement {
  const proximoNumero = Math.max(0, ...pontos.map((ponto) => ponto.numero)) + 1;
  const rotulo = document.createElement("label");
  rotulo.htmlFor = "novo-caixa-numero";
  rotulo.textContent = "Novo caixa nº";
  const numero = document.createElement("input");
  numero.id = "novo-caixa-numero";
  numero.type = "number";
  numero.min = "1";
  numero.max = "999";
  numero.value = String(proximoNumero);

  const cadastrar = botao("Cadastrar caixa", "btn btn-ghost btn-pequeno", () => {
    erro.hidden = true;
    cadastrar.disabled = true;
    cadastrarPontoDeCaixa(Number(numero.value)).then(aoMudar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível cadastrar o caixa.");
        cadastrar.disabled = false;
      });
  });
  numero.addEventListener("keydown", (evento) => {
    if (evento.key === "Enter") {
      cadastrar.click();
    }
  });

  const linha = document.createElement("div");
  linha.className = "pontos-caixa__novo";
  linha.append(rotulo, numero, cadastrar);
  return linha;
}
