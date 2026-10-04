import {
  fecharCaixa,
  registrarSangria,
  registrarSuprimento,
  type CaixaAberto,
  type ConferenciaCaixa,
} from "../../api/caixaApi.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { criarContagemCedulas } from "./contagemCedulas.js";
import { criarResultadoFechamento } from "./resultadoFechamentoView.js";

const MOTIVO_PADRAO_REPOSICAO = "Reposição de troco";
const MOTIVO_PADRAO_SANGRIA = "Retirada para o cofre";
const HORA = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

export interface AcoesCaixa {
  aoRepor: () => void;
  aoSangria: () => void;
  aoFechar: () => void;
}

/** Faixa no topo do caixa: desde quando está aberto, fundo inicial e as ações do turno. */
export function criarBarraCaixa(caixa: CaixaAberto, acoes: AcoesCaixa): HTMLElement {
  const situacao = document.createElement("p");
  situacao.className = "barra-caixa__situacao";
  const marcador = document.createElement("span");
  marcador.className = "barra-caixa__marcador";
  marcador.setAttribute("aria-hidden", "true");
  const texto = document.createElement("span");
  texto.textContent = `Caixa aberto às ${HORA.format(new Date(caixa.abertaEm))} · fundo de troco ${formatarMoeda(caixa.fundoInicial)}`;
  situacao.append(marcador, texto);

  const botoes = document.createElement("div");
  botoes.className = "barra-caixa__acoes";
  botoes.append(
    botao("Reposição de troco", "btn btn-ghost btn-pequeno", acoes.aoRepor),
    botao("Sangria", "btn btn-ghost btn-pequeno", acoes.aoSangria),
    botao("Fechar caixa", "btn btn-ghost btn-pequeno", acoes.aoFechar)
  );

  const barra = document.createElement("div");
  barra.className = "barra-caixa";
  barra.append(situacao, botoes);
  return barra;
}

export function criarPainelReposicao(aoConcluir: (caixa: CaixaAberto) => void, aoVoltar: () => void): HTMLElement {
  const contagem = criarContagemCedulas({});
  const motivo = criarCampoTexto("caixa-motivo-reposicao", "Motivo", "text", true);
  motivo.entrada.value = MOTIVO_PADRAO_REPOSICAO;
  motivo.entrada.maxLength = 200;
  return montarPainel({
    titulo: "Reposição de troco",
    instrucao: "Conte as notas e moedas trocadas que estão entrando na gaveta.",
    campos: [contagem.elemento, motivo.container],
    rotuloConfirmar: "Registrar reposição",
    focar: contagem.focar,
    aoVoltar,
    enviar: () => registrarSuprimento(contagem.contagem(), motivo.entrada.value.trim()).then(aoConcluir),
    mensagemFalha: "Não foi possível registrar a reposição.",
  });
}

export function criarPainelSangria(aoConcluir: (caixa: CaixaAberto) => void, aoVoltar: () => void): HTMLElement {
  const valor = criarCampoTexto("caixa-valor-sangria", "Valor retirado (R$)", "number", true);
  valor.entrada.min = "0.01";
  valor.entrada.step = "0.01";
  valor.entrada.inputMode = "decimal";
  const motivo = criarCampoTexto("caixa-motivo-sangria", "Motivo", "text", true);
  motivo.entrada.value = MOTIVO_PADRAO_SANGRIA;
  motivo.entrada.maxLength = 200;
  return montarPainel({
    titulo: "Sangria",
    instrucao: "Dinheiro retirado da gaveta durante o turno (levado ao cofre, por exemplo).",
    campos: [linhaDeCampos(valor.container, motivo.container)],
    rotuloConfirmar: "Registrar sangria",
    focar: () => valor.entrada.focus(),
    aoVoltar,
    enviar: () => registrarSangria(Number(valor.entrada.value), motivo.entrada.value.trim()).then(aoConcluir),
    mensagemFalha: "Não foi possível registrar a sangria.",
  });
}

/**
 * Fechamento cego: o operador conta a gaveta sem ver quanto deveria dar. O resultado (bateu,
 * sobrou ou faltou) só aparece depois de a contagem ser enviada.
 */
export function criarPainelFechamento(aoFechar: (conferencia: ConferenciaCaixa) => void, aoVoltar: () => void): HTMLElement {
  const contagem = criarContagemCedulas({});
  const observacao = criarCampoTexto("caixa-observacao-fechamento", "Observação (opcional)", "text", false);
  observacao.entrada.maxLength = 500;
  return montarPainel({
    titulo: "Fechar o caixa",
    instrucao: "Conte todas as notas e moedas da gaveta, inclusive o fundo de troco. Depois de fechar, o caixa não aceita mais vendas.",
    campos: [contagem.elemento, observacao.container],
    rotuloConfirmar: "Fechar caixa",
    focar: contagem.focar,
    aoVoltar,
    enviar: () => fecharCaixa(contagem.contagem(), textoOuNulo(observacao.entrada.value)).then(aoFechar),
    mensagemFalha: "Não foi possível fechar o caixa.",
  });
}

/** Tela final do turno: a conferência completa e o botão para abrir um novo caixa. */
export function criarTelaCaixaFechado(conferencia: ConferenciaCaixa, aoAbrirNovo: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Caixa fechado";
  const novo = botao("Abrir um novo caixa", "btn btn-primary", aoAbrirNovo);
  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(novo);
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
  const voltar = botao("Voltar para a venda", "btn btn-ghost", opcoes.aoVoltar);

  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(voltar, confirmar);

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

function botao(rotulo: string, classe: string, aoClicar: () => void): HTMLButtonElement {
  const elemento = document.createElement("button");
  elemento.type = "button";
  elemento.className = classe;
  elemento.textContent = rotulo;
  elemento.addEventListener("click", aoClicar);
  return elemento;
}

function linhaDeCampos(...campos: HTMLElement[]): HTMLElement {
  const linha = document.createElement("div");
  linha.className = "caixa-painel__linha-campos";
  linha.append(...campos);
  return linha;
}
