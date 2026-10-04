import {
  fecharCaixa,
  registrarSangria,
  registrarSuprimento,
  type CaixaAberto,
  type ConferenciaCaixa,
} from "../../api/caixaApi.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { criarContagemCedulas } from "./contagemCedulas.js";
import { criarResultadoFechamento } from "./resultadoFechamentoView.js";

const MOTIVO_PADRAO_REPOSICAO = "Reposição de troco";
const MOTIVO_PADRAO_SANGRIA = "Retirada para o cofre";
const ROTULO_VOLTAR = "Voltar para os caixas";

/** Reposição de troco no caixa escolhido: as cédulas trocadas que entram na gaveta. */
export function criarPainelReposicao(caixa: CaixaAberto, aoConcluir: () => void, aoVoltar: () => void): HTMLElement {
  const contagem = criarContagemCedulas({});
  const motivo = criarCampoTexto("caixa-motivo-reposicao", "Motivo", "text", true);
  motivo.entrada.value = MOTIVO_PADRAO_REPOSICAO;
  motivo.entrada.maxLength = 200;
  return montarPainel({
    titulo: `Reposição de troco · caixa de ${caixa.operadorNome}`,
    instrucao: "Conte as notas e moedas trocadas que estão entrando na gaveta.",
    campos: [contagem.elemento, motivo.container],
    rotuloConfirmar: "Registrar reposição",
    focar: contagem.focar,
    aoVoltar,
    enviar: () => registrarSuprimento(caixa.id, contagem.contagem(), motivo.entrada.value.trim()).then(aoConcluir),
    mensagemFalha: "Não foi possível registrar a reposição.",
  });
}

export function criarPainelSangria(caixa: CaixaAberto, aoConcluir: () => void, aoVoltar: () => void): HTMLElement {
  const valor = criarCampoTexto("caixa-valor-sangria", "Valor retirado (R$)", "number", true);
  valor.entrada.min = "0.01";
  valor.entrada.step = "0.01";
  valor.entrada.inputMode = "decimal";
  const motivo = criarCampoTexto("caixa-motivo-sangria", "Motivo", "text", true);
  motivo.entrada.value = MOTIVO_PADRAO_SANGRIA;
  motivo.entrada.maxLength = 200;
  return montarPainel({
    titulo: `Sangria · caixa de ${caixa.operadorNome}`,
    instrucao: "Dinheiro retirado da gaveta durante o turno (levado ao cofre, por exemplo).",
    campos: [linhaDeCampos(valor.container, motivo.container)],
    rotuloConfirmar: "Registrar sangria",
    focar: () => valor.entrada.focus(),
    aoVoltar,
    enviar: () => registrarSangria(caixa.id, Number(valor.entrada.value), motivo.entrada.value.trim()).then(aoConcluir),
    mensagemFalha: "Não foi possível registrar a sangria.",
  });
}

/**
 * Fechamento cego: quem fecha conta a gaveta sem ver quanto deveria dar. O resultado (bateu,
 * sobrou ou faltou) só aparece depois de a contagem ser enviada.
 */
export function criarPainelFechamento(
  caixa: CaixaAberto, aoFechar: (conferencia: ConferenciaCaixa) => void, aoVoltar: () => void
): HTMLElement {
  const contagem = criarContagemCedulas({});
  const observacao = criarCampoTexto("caixa-observacao-fechamento", "Observação (opcional)", "text", false);
  observacao.entrada.maxLength = 500;
  return montarPainel({
    titulo: `Fechar o caixa de ${caixa.operadorNome}`,
    instrucao: "Conte todas as notas e moedas da gaveta, inclusive o fundo de troco. Depois de fechado, o operador não consegue mais vender neste caixa.",
    campos: [contagem.elemento, observacao.container],
    rotuloConfirmar: "Fechar caixa",
    focar: contagem.focar,
    aoVoltar,
    enviar: () => fecharCaixa(caixa.id, contagem.contagem(), textoOuNulo(observacao.entrada.value)).then(aoFechar),
    mensagemFalha: "Não foi possível fechar o caixa.",
  });
}

/** Depois do fechamento: a conferência completa e a volta para a lista de caixas. */
export function criarTelaCaixaFechado(conferencia: ConferenciaCaixa, aoVoltar: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = `Caixa de ${conferencia.operadorNome} fechado`;
  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(botao(ROTULO_VOLTAR, "btn btn-primary", aoVoltar));
  const painel = document.createElement("div");
  painel.className = "caixa-painel";
  painel.append(titulo, criarResultadoFechamento(conferencia), acoes);
  return painel;
}

interface OpcoesPainel {
  titulo: string;
  instrucao: string;
  campos: HTMLElement[];
  rotuloConfirmar: string;
  focar: () => void;
  aoVoltar: () => void;
  enviar: () => Promise<unknown>;
  mensagemFalha: string;
}

function montarPainel(opcoes: OpcoesPainel): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = opcoes.titulo;
  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = opcoes.instrucao;

  const erro = criarMensagemErro();
  const confirmar = document.createElement("button");
  confirmar.type = "submit";
  confirmar.className = "btn btn-primary";
  confirmar.textContent = opcoes.rotuloConfirmar;

  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(botao(ROTULO_VOLTAR, "btn btn-ghost", opcoes.aoVoltar), confirmar);

  const formulario = document.createElement("form");
  formulario.className = "caixa-painel";
  formulario.append(titulo, instrucao, ...opcoes.campos, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    confirmar.disabled = true;
    opcoes.enviar().catch((falha: unknown) => {
      mostrarErro(erro, falha, opcoes.mensagemFalha);
      confirmar.disabled = false;
    });
  });
  queueMicrotask(opcoes.focar);
  return formulario;
}

function linhaDeCampos(...campos: HTMLElement[]): HTMLElement {
  const linha = document.createElement("div");
  linha.className = "caixa-painel__linha-campos";
  linha.append(...campos);
  return linha;
}

export function botao(rotulo: string, classe: string, aoClicar: () => void): HTMLButtonElement {
  const elemento = document.createElement("button");
  elemento.type = "button";
  elemento.className = classe;
  elemento.textContent = rotulo;
  elemento.addEventListener("click", aoClicar);
  return elemento;
}
