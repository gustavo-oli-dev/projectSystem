import {
  abrirCaixa,
  buscarFundoPadrao,
  listarOperadoresDeCaixa,
  type CedulaContada,
  type OperadorCaixa,
} from "../../api/caixaApi.js";
import { criarCampoSelecao, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { contagemDe, criarContagemCedulas } from "./contagemCedulas.js";
import { botao } from "./operacaoCaixaView.js";

/**
 * Abertura feita por quem gerencia o caixa: escolhe o operador (só quem pode vender e ainda não
 * está com caixa aberto) e confere o fundo de troco, que já vem com o padrão definido.
 */
export function criarAberturaCaixa(aoAbrir: () => void, aoVoltar: () => void): HTMLElement {
  const area = document.createElement("div");
  area.append(elementoCarregando("Carregando operadores e fundo de troco..."));
  Promise.all([listarOperadoresDeCaixa(), buscarFundoPadrao()])
    .then(([operadores, fundoPadrao]) => {
      const disponiveis = operadores.filter((operador) => !operador.caixaAberto);
      if (disponiveis.length === 0) {
        area.replaceChildren(
          cartaoEstado("Todos os funcionários que podem vender já estão com o caixa aberto."),
          botao("Voltar para os caixas", "btn btn-ghost", aoVoltar)
        );
        return;
      }
      area.replaceChildren(montarFormulario(disponiveis, fundoPadrao, aoAbrir, aoVoltar));
    })
    .catch(() => area.replaceChildren(cartaoEstado("Não foi possível carregar os dados para abrir o caixa.", "erro")));
  return area;
}

function montarFormulario(
  operadores: readonly OperadorCaixa[], fundoPadrao: readonly CedulaContada[], aoAbrir: () => void, aoVoltar: () => void
): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Abrir caixa";
  const operador = criarCampoSelecao("caixa-operador", "Operador do caixa", operadores.map((opcao) => ({
    valor: opcao.id,
    rotulo: opcao.nome,
  })));
  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = fundoPadrao.length > 0
    ? "Confira as notas e moedas do fundo de troco que vão para a gaveta. Corrija se alguma quantidade for diferente."
    : "Conte as notas e moedas do fundo de troco que vão para a gaveta.";

  const contagem = criarContagemCedulas(contagemDe(fundoPadrao));
  const erro = criarMensagemErro();
  const abrir = document.createElement("button");
  abrir.type = "submit";
  abrir.className = "btn btn-primary";
  abrir.textContent = "Abrir caixa";

  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(botao("Voltar para os caixas", "btn btn-ghost", aoVoltar), abrir);

  const formulario = document.createElement("form");
  formulario.className = "caixa-painel";
  formulario.append(titulo, operador.container, instrucao, contagem.elemento, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    abrir.disabled = true;
    abrirCaixa(operador.selecao.value, contagem.contagem())
      .then(aoAbrir)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível abrir o caixa.");
        abrir.disabled = false;
      });
  });
  queueMicrotask(() => operador.selecao.focus());
  return formulario;
}
