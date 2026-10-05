import {
  abrirCaixas,
  buscarFundoPadrao,
  listarOperadoresDeCaixa,
  listarPontosDeCaixa,
  type CedulaContada,
  type OperadorCaixa,
  type PontoCaixa,
} from "../../api/caixaApi.js";
import { criarMensagemErro, criarSelecao, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { contagemDe, criarContagemCedulas } from "./contagemCedulas.js";
import { botao } from "./operacaoCaixaView.js";

const OPCAO_SEM_OPERADOR = "";

interface LinhaCaixa {
  elemento: HTMLElement;
  ponto: PontoCaixa;
  marcado: HTMLInputElement;
  operador: HTMLSelectElement;
}

/**
 * Abertura feita por quem gerencia o caixa: marca 1 ou mais caixas (só os livres), escolhe o
 * operador de cada um e confere o fundo de troco — o mesmo para cada gaveta, já com o padrão.
 */
export function criarAberturaCaixa(aoAbrir: () => void, aoVoltar: () => void): HTMLElement {
  const area = document.createElement("div");
  area.append(elementoCarregando("Carregando caixas, operadores e fundo de troco..."));
  Promise.all([listarPontosDeCaixa(), listarOperadoresDeCaixa(), buscarFundoPadrao()])
    .then(([pontos, operadores, fundoPadrao]) => {
      const livres = pontos.filter((ponto) => ponto.ativo && !ponto.aberto);
      const disponiveis = operadores.filter((operador) => !operador.caixaAberto);
      const aviso = livres.length === 0
        ? (pontos.length === 0
          ? "Nenhum caixa cadastrado. Cadastre os caixas da loja (Caixa 01, 02...) em \"Caixas da loja\"."
          : "Todos os caixas da loja já estão abertos.")
        : disponiveis.length === 0 ? "Todos os funcionários que podem vender já estão em um caixa aberto." : null;
      if (aviso !== null) {
        area.replaceChildren(cartaoEstado(aviso), botao("Voltar para os caixas", "btn btn-ghost", aoVoltar));
        return;
      }
      area.replaceChildren(montarFormulario(livres, disponiveis, fundoPadrao, aoAbrir, aoVoltar));
    })
    .catch(() => area.replaceChildren(cartaoEstado("Não foi possível carregar os dados para abrir o caixa.", "erro")));
  return area;
}

function montarFormulario(
  pontos: readonly PontoCaixa[], operadores: readonly OperadorCaixa[], fundoPadrao: readonly CedulaContada[],
  aoAbrir: () => void, aoVoltar: () => void
): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Abrir caixas";

  const resumo = document.createElement("p");
  resumo.className = "abertura-caixas__resumo";
  // A contagem avisa a cada mudança (inclusive ao ser criada, antes do resumo existir).
  let resumoPronto = false;
  const contagem = criarContagemCedulas(contagemDe(fundoPadrao), () => {
    if (resumoPronto) {
      atualizarResumo();
    }
  });

  const linhas = pontos.map((ponto) => criarLinhaCaixa(ponto, operadores, () => atualizarResumo()));
  const atualizarResumo = (): void => {
    const marcados = linhas.filter((linha) => linha.marcado.checked).length;
    resumo.textContent = marcados === 0
      ? "Marque os caixas que vão abrir."
      : `${marcados} caixa(s) × ${formatarMoeda(contagem.total())} = ${formatarMoeda(marcados * contagem.total())} em fundo de troco`;
  };

  const tituloCaixas = document.createElement("h3");
  tituloCaixas.className = "abertura-caixas__titulo";
  tituloCaixas.textContent = "Caixas e operadores";
  const listaCaixas = document.createElement("div");
  listaCaixas.className = "abertura-caixas__lista";
  listaCaixas.append(...linhas.map((linha) => linha.elemento));

  const tituloFundo = document.createElement("h3");
  tituloFundo.className = "abertura-caixas__titulo";
  tituloFundo.textContent = "Fundo de troco de cada gaveta";
  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = fundoPadrao.length > 0
    ? "Já vem com o fundo padrão. Confira as notas e moedas e corrija se a gaveta for diferente — vale para cada caixa marcado."
    : "Conte as notas e moedas que vão em cada gaveta — vale para cada caixa marcado.";

  const erro = criarMensagemErro();
  const abrir = document.createElement("button");
  abrir.type = "submit";
  abrir.className = "btn btn-primary";
  abrir.textContent = "Abrir caixas";

  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(resumo, botao("Voltar para os caixas", "btn btn-ghost", aoVoltar), abrir);

  const formulario = document.createElement("form");
  formulario.className = "caixa-painel";
  formulario.append(titulo, tituloCaixas, listaCaixas, tituloFundo, instrucao, contagem.elemento, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    const escolhidos = linhas.filter((linha) => linha.marcado.checked);
    if (escolhidos.length === 0) {
      mostrarErro(erro, null, "Marque ao menos um caixa para abrir.");
      return;
    }
    const semOperador = escolhidos.find((linha) => linha.operador.value === OPCAO_SEM_OPERADOR);
    if (semOperador !== undefined) {
      mostrarErro(erro, null, `Escolha o operador do ${semOperador.ponto.nome}.`);
      return;
    }
    abrir.disabled = true;
    abrirCaixas(escolhidos.map((linha) => ({ pontoCaixaId: linha.ponto.id, operadorId: linha.operador.value })), contagem.contagem())
      .then(aoAbrir)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível abrir os caixas.");
        abrir.disabled = false;
      });
  });
  resumoPronto = true;
  atualizarResumo();
  return formulario;
}

/** "☐ Caixa 01   [operador ▾]" — o operador só fica liberado com o caixa marcado. */
function criarLinhaCaixa(ponto: PontoCaixa, operadores: readonly OperadorCaixa[], aoMudar: () => void): LinhaCaixa {
  const id = `abrir-${ponto.id}`;
  const marcado = document.createElement("input");
  marcado.type = "checkbox";
  marcado.id = id;
  const nome = document.createElement("label");
  nome.htmlFor = id;
  nome.className = "abertura-caixas__nome";
  nome.textContent = ponto.nome;

  const operador = criarSelecao([
    { valor: OPCAO_SEM_OPERADOR, rotulo: "Escolha o operador" },
    ...operadores.map((opcao) => ({ valor: opcao.id, rotulo: opcao.nome })),
  ]);
  operador.disabled = true;
  operador.setAttribute("aria-label", `Operador do ${ponto.nome}`);
  marcado.addEventListener("change", () => {
    operador.disabled = !marcado.checked;
    if (marcado.checked) {
      operador.focus();
    }
    aoMudar();
  });

  const linha = document.createElement("div");
  linha.className = "abertura-caixas__linha";
  linha.append(marcado, nome, operador);
  return { elemento: linha, ponto, marcado, operador };
}
