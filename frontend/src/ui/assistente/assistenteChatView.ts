import { perguntarAoAssistente } from "../../api/assistenteApi.js";
import { criarIcone } from "../icones.js";
import { criarPainelWhatsApp } from "./whatsappAssistenteView.js";

type Autor = "voce" | "assistente";

const TAMANHO_MAXIMO_PERGUNTA = 1000;
const SUGESTOES: readonly string[] = [
  "Como está o faturamento deste mês?",
  "Quantos pedidos estão aguardando emissão?",
  "Quais cobranças estão pendentes?",
];

/**
 * Botão da topbar que abre o chat do assistente do gestor numa gaveta lateral. A conversa vive só
 * enquanto a página está aberta; o backend registra cada pergunta para auditoria.
 */
export function criarBotaoAssistente(): HTMLElement {
  const gaveta = criarGaveta();

  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-outline btn-pequeno topbar__acao";
  botao.setAttribute("aria-expanded", "false");
  botao.append(criarIcone("assistente"), criarTexto("span", "Assistente"));

  const alternar = (aberta: boolean): void => {
    gaveta.elemento.classList.toggle("gaveta--aberta", aberta);
    botao.setAttribute("aria-expanded", String(aberta));
    if (aberta) {
      gaveta.campo.focus();
    }
  };
  botao.addEventListener("click", () => alternar(!gaveta.elemento.classList.contains("gaveta--aberta")));
  gaveta.botaoFechar.addEventListener("click", () => alternar(false));

  const container = document.createElement("div");
  container.className = "assistente";
  container.append(botao, gaveta.elemento);
  return container;
}

interface Gaveta {
  elemento: HTMLElement;
  campo: HTMLTextAreaElement;
  botaoFechar: HTMLButtonElement;
}

function criarGaveta(): Gaveta {
  const titulo = criarTexto("h2", "Assistente do gestor");
  titulo.className = "gaveta__titulo";

  const subtitulo = criarTexto("p", "Somente leitura. Responde só sobre o que o seu perfil de acesso pode ver.");
  subtitulo.className = "gaveta__subtitulo";

  const botaoFechar = document.createElement("button");
  botaoFechar.type = "button";
  botaoFechar.className = "gaveta__fechar";
  botaoFechar.setAttribute("aria-label", "Fechar assistente");
  botaoFechar.append(criarIcone("fechar"));

  const identificacao = document.createElement("div");
  identificacao.append(titulo, subtitulo);

  const painelWhatsApp = criarPainelWhatsApp();
  painelWhatsApp.hidden = true;

  const botaoWhatsApp = document.createElement("button");
  botaoWhatsApp.type = "button";
  botaoWhatsApp.className = "btn btn-outline btn-pequeno";
  botaoWhatsApp.textContent = "Usar no WhatsApp";
  botaoWhatsApp.setAttribute("aria-expanded", "false");
  botaoWhatsApp.addEventListener("click", () => {
    painelWhatsApp.hidden = !painelWhatsApp.hidden;
    botaoWhatsApp.setAttribute("aria-expanded", String(!painelWhatsApp.hidden));
  });

  const acoesCabecalho = document.createElement("div");
  acoesCabecalho.className = "gaveta__acoes";
  acoesCabecalho.append(botaoWhatsApp, botaoFechar);

  const cabecalho = document.createElement("div");
  cabecalho.className = "gaveta__cabecalho";
  cabecalho.append(identificacao, acoesCabecalho);

  const historico = document.createElement("div");
  historico.className = "gaveta__historico";
  historico.setAttribute("aria-live", "polite");

  const campo = document.createElement("textarea");
  campo.className = "compositor__campo";
  campo.rows = 2;
  campo.maxLength = TAMANHO_MAXIMO_PERGUNTA;
  campo.placeholder = "Pergunte, por exemplo: quanto faturamos esta semana?";
  campo.setAttribute("aria-label", "Pergunta ao assistente");

  const botaoEnviar = document.createElement("button");
  botaoEnviar.type = "submit";
  botaoEnviar.className = "btn btn-primary";
  botaoEnviar.textContent = "Perguntar";

  const formulario = document.createElement("form");
  formulario.className = "compositor";
  formulario.append(campo, botaoEnviar);

  const enviar = (pergunta: string): void => {
    const texto = pergunta.trim();
    if (texto === "" || botaoEnviar.disabled) {
      return;
    }
    campo.value = "";
    void conversar(texto, historico, botaoEnviar);
  };

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    enviar(campo.value);
  });
  campo.addEventListener("keydown", (evento) => {
    if (evento.key === "Enter" && !evento.shiftKey) {
      evento.preventDefault();
      enviar(campo.value);
    }
  });

  historico.append(criarSugestoes(enviar));

  const elemento = document.createElement("aside");
  elemento.className = "gaveta";
  elemento.setAttribute("aria-label", "Assistente do gestor");
  elemento.append(cabecalho, painelWhatsApp, historico, formulario);

  return { elemento, campo, botaoFechar };
}

function criarSugestoes(enviar: (pergunta: string) => void): HTMLElement {
  const lista = document.createElement("div");
  lista.className = "gaveta__sugestoes";
  for (const sugestao of SUGESTOES) {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "gaveta__sugestao";
    botao.textContent = sugestao;
    botao.addEventListener("click", () => enviar(sugestao));
    lista.append(botao);
  }
  return lista;
}

async function conversar(pergunta: string, historico: HTMLElement, botaoEnviar: HTMLButtonElement): Promise<void> {
  historico.querySelector(".gaveta__sugestoes")?.remove();
  adicionarBalao(historico, "voce", pergunta);
  const pensando = adicionarBalao(historico, "assistente", "Consultando...");
  pensando.classList.add("balao--pensando");
  botaoEnviar.disabled = true;

  try {
    const resposta = await perguntarAoAssistente(pergunta);
    pensando.remove();
    adicionarBalao(historico, "assistente", resposta);
  } catch (falha) {
    pensando.remove();
    const mensagem = falha instanceof Error && falha.message !== "" ? falha.message : "Não consegui responder agora.";
    adicionarBalao(historico, "assistente", mensagem).classList.add("balao--erro");
  } finally {
    botaoEnviar.disabled = false;
  }
}

function adicionarBalao(historico: HTMLElement, autor: Autor, texto: string): HTMLElement {
  const conteudo = criarTexto("p", texto);
  conteudo.className = "balao__texto";

  const balao = document.createElement("div");
  balao.className = autor === "voce" ? "balao balao--atendente" : "balao balao--bot";
  balao.append(conteudo);
  historico.append(balao);
  historico.scrollTop = historico.scrollHeight;
  return balao;
}

function criarTexto<K extends "span" | "p" | "h2">(tag: K, texto: string): HTMLElementTagNameMap[K] {
  const elemento = document.createElement(tag);
  elemento.textContent = texto;
  return elemento;
}
