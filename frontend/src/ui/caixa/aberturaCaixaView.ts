import { abrirCaixa, buscarFundoPadrao, type CaixaAberto, type Contagem } from "../../api/caixaApi.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { contagemDe, criarContagemCedulas } from "./contagemCedulas.js";

/**
 * Sem caixa aberto não há venda. A contagem já vem com o fundo de troco padrão definido pelo
 * gerente; o operador confere nota por nota e corrige o que for diferente na gaveta.
 */
export function criarAberturaCaixa(aoAbrir: (caixa: CaixaAberto) => void): HTMLElement {
  const area = document.createElement("div");
  area.append(elementoCarregando("Carregando o fundo de troco..."));
  buscarFundoPadrao()
    .then((fundoPadrao) => area.replaceChildren(montarFormulario(contagemDe(fundoPadrao), fundoPadrao.length > 0, aoAbrir)))
    .catch(() => area.replaceChildren(cartaoEstado("Não foi possível carregar o fundo de troco.", "erro")));
  return area;
}

function montarFormulario(
  fundoPadrao: Contagem, temPadrao: boolean, aoAbrir: (caixa: CaixaAberto) => void
): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Abrir o caixa";
  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = temPadrao
    ? "Confira as notas e moedas do fundo de troco que estão na gaveta. Corrija se alguma quantidade for diferente."
    : "Conte as notas e moedas do fundo de troco que estão na gaveta.";

  const contagem = criarContagemCedulas(fundoPadrao);
  const erro = criarMensagemErro();
  const abrir = document.createElement("button");
  abrir.type = "submit";
  abrir.className = "btn btn-primary";
  abrir.textContent = "Abrir caixa";

  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(abrir);

  const formulario = document.createElement("form");
  formulario.className = "caixa-painel";
  formulario.append(titulo, instrucao, contagem.elemento, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    abrir.disabled = true;
    abrirCaixa(contagem.contagem())
      .then(aoAbrir)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível abrir o caixa.");
        abrir.disabled = false;
      });
  });
  queueMicrotask(() => contagem.focar());
  return formulario;
}
