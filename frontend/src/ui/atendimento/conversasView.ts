import { listarClientes } from "../../api/clientesApi.js";
import {
  assumirConversa,
  devolverAoBot,
  encerrarConversa,
  enviarMensagem,
  listarConversasAbertas,
  listarMensagens,
  type Conversa,
  type Mensagem,
} from "../../api/conversasApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";

const INTERVALO_ATUALIZACAO_MS = 5_000;
const ROTULO_ORIGEM: Record<string, string> = { CLIENTE: "Cliente", BOT: "Bot", ATENDENTE: "Atendente" };

/** Um único timer por vez: trocar de conversa (ou de tela) encerra o anterior. */
let atualizacaoAgendada: number | undefined;

interface Contexto {
  painel: HTMLElement;
  nomesClientes: Map<string, string>;
}

export async function montarConversas(container: HTMLElement): Promise<void> {
  pararAtualizacao();

  const titulo = document.createElement("h1");
  titulo.textContent = "Conversas";

  const subtitulo = document.createElement("p");
  subtitulo.className = "subtitulo-pagina";
  subtitulo.textContent = "WhatsApp atendido pelo bot. Assuma uma conversa quando precisar responder você mesmo.";

  const painel = document.createElement("div");
  container.replaceChildren(titulo, subtitulo, painel);
  painel.append(elementoCarregando("Carregando conversas..."));

  try {
    const [conversas, clientes] = await Promise.all([
      listarConversasAbertas(),
      possui("CLIENTES_VER") ? listarClientes() : Promise.resolve([]),
    ]);
    const contexto: Contexto = {
      painel,
      nomesClientes: new Map(clientes.map((cliente) => [cliente.id, cliente.nome])),
    };
    renderizarCaixaDeEntrada(contexto, ordenarPorPrioridade(conversas));
  } catch {
    painel.replaceChildren(cartaoEstado("Não foi possível carregar as conversas.", "erro"));
  }
}

/** Quem está esperando atendente humano aparece primeiro. */
function ordenarPorPrioridade(conversas: Conversa[]): Conversa[] {
  return [...conversas].sort((a, b) => Number(aguardandoAtendente(b)) - Number(aguardandoAtendente(a)));
}

function aguardandoAtendente(conversa: Conversa): boolean {
  return conversa.modo === "HUMANO" && conversa.atendenteId === null;
}

function renderizarCaixaDeEntrada(contexto: Contexto, conversas: Conversa[]): void {
  if (conversas.length === 0) {
    contexto.painel.replaceChildren(
      cartaoEstado("Nenhuma conversa aberta. As mensagens chegam aqui quando a W-API estiver conectada.")
    );
    return;
  }

  const lista = document.createElement("div");
  lista.className = "inbox__lista";

  const conversaAberta = document.createElement("section");
  conversaAberta.className = "inbox__conversa";

  const inbox = document.createElement("div");
  inbox.className = "inbox";
  inbox.append(lista, conversaAberta);
  contexto.painel.replaceChildren(inbox);

  const selecionar = (conversa: Conversa, item: HTMLButtonElement): void => {
    lista.querySelectorAll(".inbox__item").forEach((elemento) => elemento.classList.remove("inbox__item--ativo"));
    item.classList.add("inbox__item--ativo");
    void abrirConversa(contexto, conversaAberta, conversa);
  };

  const itens = conversas.map((conversa) => {
    const item = criarItemLista(contexto, conversa);
    item.addEventListener("click", () => selecionar(conversa, item));
    return item;
  });
  lista.append(...itens);

  const primeiraConversa = conversas[0];
  const primeiroItem = itens[0];
  if (primeiraConversa !== undefined && primeiroItem !== undefined) {
    selecionar(primeiraConversa, primeiroItem);
  }
}

function criarItemLista(contexto: Contexto, conversa: Conversa): HTMLButtonElement {
  const nome = document.createElement("span");
  nome.className = "inbox__item-nome";
  nome.textContent = nomeDaConversa(contexto, conversa);

  const telefone = document.createElement("span");
  telefone.className = "inbox__item-telefone";
  telefone.textContent = conversa.telefoneWhatsapp;

  const item = document.createElement("button");
  item.type = "button";
  item.className = "inbox__item";
  item.append(nome, criarSeloModo(conversa), telefone);
  return item;
}

function criarSeloModo(conversa: Conversa): HTMLSpanElement {
  const selo = document.createElement("span");
  if (aguardandoAtendente(conversa)) {
    selo.className = "selo selo--aguardando_atendente";
    selo.textContent = "Aguardando atendente";
  } else if (conversa.modo === "BOT") {
    selo.className = "selo selo--bot";
    selo.textContent = "Bot";
  } else {
    selo.className = "selo selo--humano";
    selo.textContent = "Atendente";
  }
  return selo;
}

function nomeDaConversa(contexto: Contexto, conversa: Conversa): string {
  if (conversa.clienteId === null) {
    return "Contato não identificado";
  }
  return contexto.nomesClientes.get(conversa.clienteId) ?? "Cliente";
}

async function abrirConversa(contexto: Contexto, area: HTMLElement, conversa: Conversa): Promise<void> {
  pararAtualizacao();

  const historico = document.createElement("div");
  historico.className = "inbox__historico";

  const erro = document.createElement("p");
  erro.className = "aviso-erro";
  erro.setAttribute("role", "alert");
  erro.hidden = true;

  area.replaceChildren(criarCabecalhoConversa(contexto, conversa, erro), historico, erro);
  if (possui("CONVERSAS_ATENDER")) {
    area.append(criarCompositor(conversa, historico, erro));
  }

  await atualizarHistorico(conversa.id, historico);
  atualizacaoAgendada = window.setInterval(() => {
    if (!historico.isConnected) {
      pararAtualizacao();
      return;
    }
    void atualizarHistorico(conversa.id, historico);
  }, INTERVALO_ATUALIZACAO_MS);
}

function criarCabecalhoConversa(contexto: Contexto, conversa: Conversa, erro: HTMLElement): HTMLElement {
  const nome = document.createElement("p");
  nome.className = "inbox__cabecalho-nome";
  nome.textContent = nomeDaConversa(contexto, conversa);

  const situacao = document.createElement("p");
  situacao.className = "inbox__cabecalho-situacao";
  situacao.textContent = descreverSituacao(conversa);

  const identificacao = document.createElement("div");
  identificacao.append(nome, situacao);

  const acoes = document.createElement("div");
  acoes.className = "barra-acoes";

  const acao = (rotulo: string, classe: string, executar: () => Promise<unknown>): HTMLButtonElement => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = `btn btn-pequeno ${classe}`;
    botao.textContent = rotulo;
    botao.addEventListener("click", () => {
      botao.disabled = true;
      erro.hidden = true;
      executar()
        .then(() => montarConversas(contexto.painel.parentElement ?? contexto.painel))
        .catch((falha: unknown) => {
          erro.textContent = falha instanceof Error ? falha.message : "Não foi possível concluir a ação.";
          erro.hidden = false;
          botao.disabled = false;
        });
    });
    return botao;
  };

  if (possui("CONVERSAS_ATENDER")) {
    if (conversa.modo === "BOT" || aguardandoAtendente(conversa)) {
      acoes.append(acao("Assumir conversa", "btn-primary", () => assumirConversa(conversa.id)));
    }
    if (conversa.modo === "HUMANO") {
      acoes.append(acao("Devolver ao bot", "btn-outline", () => devolverAoBot(conversa.id)));
    }
    acoes.append(acao("Encerrar", "btn-ghost", () => encerrarConversa(conversa.id)));
  }

  const cabecalho = document.createElement("div");
  cabecalho.className = "inbox__cabecalho";
  cabecalho.append(identificacao, acoes);
  return cabecalho;
}

function descreverSituacao(conversa: Conversa): string {
  if (aguardandoAtendente(conversa)) {
    return `Aguardando atendente — ${conversa.motivoTransferencia ?? "transferida pelo bot"}`;
  }
  if (conversa.modo === "BOT") {
    return `${conversa.telefoneWhatsapp} · respondida pelo bot`;
  }
  return `${conversa.telefoneWhatsapp} · em atendimento humano`;
}

async function atualizarHistorico(conversaId: string, historico: HTMLElement): Promise<void> {
  try {
    const mensagens = await listarMensagens(conversaId);
    const estavaNoFim = historico.scrollHeight - historico.scrollTop - historico.clientHeight < 40;
    historico.replaceChildren(...mensagens.map(criarBalao));
    if (mensagens.length === 0) {
      historico.append(cartaoEstado("Nenhuma mensagem ainda."));
    }
    if (estavaNoFim || historico.dataset["carregado"] !== "sim") {
      historico.scrollTop = historico.scrollHeight;
      historico.dataset["carregado"] = "sim";
    }
  } catch {
    historico.replaceChildren(cartaoEstado("Não foi possível carregar as mensagens.", "erro"));
  }
}

function criarBalao(mensagem: Mensagem): HTMLElement {
  const autor = document.createElement("span");
  autor.className = "balao__autor";
  autor.textContent = ROTULO_ORIGEM[mensagem.origem] ?? mensagem.origem;

  const texto = document.createElement("p");
  texto.className = "balao__texto";
  texto.textContent = mensagem.conteudo ?? "[anexo]";

  const hora = document.createElement("span");
  hora.className = "balao__hora";
  hora.textContent = new Date(mensagem.enviadaEm).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });

  const balao = document.createElement("div");
  balao.className = `balao balao--${mensagem.origem.toLowerCase()}`;
  balao.append(autor, texto, hora);
  return balao;
}

function criarCompositor(conversa: Conversa, historico: HTMLElement, erro: HTMLElement): HTMLElement {
  const campo = document.createElement("textarea");
  campo.className = "compositor__campo";
  campo.rows = 2;
  campo.placeholder = conversa.modo === "BOT"
    ? "Responder (ao enviar, você assume a conversa e o bot para)"
    : "Escreva uma mensagem";
  campo.setAttribute("aria-label", "Mensagem");

  const botao = document.createElement("button");
  botao.type = "submit";
  botao.className = "btn btn-primary";
  botao.textContent = "Enviar";

  const formulario = document.createElement("form");
  formulario.className = "compositor";
  formulario.append(campo, botao);

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    const conteudo = campo.value.trim();
    if (conteudo === "") {
      return;
    }
    botao.disabled = true;
    erro.hidden = true;
    enviarMensagem(conversa.id, conteudo)
      .then(() => {
        campo.value = "";
        return atualizarHistorico(conversa.id, historico);
      })
      .catch((falha: unknown) => {
        erro.textContent = falha instanceof Error ? falha.message : "Não foi possível enviar a mensagem.";
        erro.hidden = false;
      })
      .finally(() => {
        botao.disabled = false;
      });
  });

  campo.addEventListener("keydown", (evento) => {
    if (evento.key === "Enter" && !evento.shiftKey) {
      evento.preventDefault();
      formulario.requestSubmit();
    }
  });

  return formulario;
}

function pararAtualizacao(): void {
  if (atualizacaoAgendada !== undefined) {
    window.clearInterval(atualizacaoAgendada);
    atualizacaoAgendada = undefined;
  }
}
