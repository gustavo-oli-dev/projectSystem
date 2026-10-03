import { confirmarCodigoWhatsApp, desvincularWhatsApp, enviarCodigoWhatsApp } from "../../api/assistenteApi.js";
import { definirSessao, sessaoAtual } from "../../state/sessaoState.js";

const TAMANHO_CODIGO = 6;

/**
 * Painel "Usar no WhatsApp" dentro do chat do assistente: a pessoa informa o próprio número,
 * recebe um código lá e confirma aqui. Depois disso, o assistente responde por WhatsApp também.
 */
export function criarPainelWhatsApp(): HTMLElement {
  const painel = document.createElement("section");
  painel.className = "painel-whatsapp";
  renderizar(painel);
  return painel;
}

function renderizar(painel: HTMLElement): void {
  const telefone = sessaoAtual()?.telefoneWhatsapp ?? null;
  painel.replaceChildren(telefone === null ? criarEtapaNumero(painel) : criarVinculado(painel, telefone));
}

function criarVinculado(painel: HTMLElement, telefone: string): HTMLElement {
  const texto = criarParagrafo(`Ativo no WhatsApp ${formatarTelefone(telefone)}. Mande suas perguntas para o WhatsApp interno (o mesmo que enviou o código), nunca para o número de clientes.`);
  const erro = criarErro();

  const botaoRemover = criarBotao("Desvincular", "btn btn-perigo btn-pequeno");
  botaoRemover.addEventListener("click", () => {
    void executar(botaoRemover, erro, async () => {
      definirSessao(await desvincularWhatsApp());
      renderizar(painel);
    });
  });

  return montarBloco(texto, erro, botaoRemover);
}

function criarEtapaNumero(painel: HTMLElement): HTMLElement {
  const explicacao = criarParagrafo("Converse com o assistente pelo seu WhatsApp. O código chega pelo WhatsApp interno da empresa — salve esse contato, é por ele que você vai perguntar.");
  const campo = criarEntrada("tel", "(85) 98888-7777", "Seu número de WhatsApp");
  const erro = criarErro();
  const botao = criarBotao("Enviar código", "btn btn-primary btn-pequeno");

  const formulario = criarFormulario(campo, botao);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    void executar(botao, erro, async () => {
      await enviarCodigoWhatsApp(campo.value);
      painel.replaceChildren(criarEtapaCodigo(painel, campo.value));
    });
  });

  return montarBloco(explicacao, formulario, erro);
}

function criarEtapaCodigo(painel: HTMLElement, telefoneDigitado: string): HTMLElement {
  const explicacao = criarParagrafo(`Digite o código que chegou no WhatsApp ${telefoneDigitado}.`);
  const campo = criarEntrada("text", "000000", "Código de 6 dígitos");
  campo.inputMode = "numeric";
  campo.maxLength = TAMANHO_CODIGO;
  campo.autocomplete = "one-time-code";
  const erro = criarErro();
  const botao = criarBotao("Confirmar", "btn btn-primary btn-pequeno");

  const voltar = criarBotao("Trocar número", "link-voltar");
  voltar.addEventListener("click", () => renderizar(painel));

  const formulario = criarFormulario(campo, botao);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    void executar(botao, erro, async () => {
      definirSessao(await confirmarCodigoWhatsApp(campo.value.trim()));
      renderizar(painel);
    });
  });

  return montarBloco(explicacao, formulario, erro, voltar);
}

async function executar(botao: HTMLButtonElement, erro: HTMLElement, acao: () => Promise<void>): Promise<void> {
  botao.disabled = true;
  erro.hidden = true;
  try {
    await acao();
  } catch (falha) {
    erro.textContent = falha instanceof Error && falha.message !== "" ? falha.message : "Não foi possível concluir.";
    erro.hidden = false;
    botao.disabled = false;
  }
}

/** 5585988887777 → (85) 98888-7777 */
function formatarTelefone(numero: string): string {
  const semDdi = numero.startsWith("55") ? numero.slice(2) : numero;
  const ddd = semDdi.slice(0, 2);
  const resto = semDdi.slice(2);
  const corte = resto.length - 4;
  return `(${ddd}) ${resto.slice(0, corte)}-${resto.slice(corte)}`;
}

function criarFormulario(campo: HTMLInputElement, botao: HTMLButtonElement): HTMLFormElement {
  const formulario = document.createElement("form");
  formulario.className = "painel-whatsapp__formulario";
  formulario.append(campo, botao);
  return formulario;
}

function criarEntrada(tipo: string, exemplo: string, rotulo: string): HTMLInputElement {
  const entrada = document.createElement("input");
  entrada.type = tipo;
  entrada.required = true;
  entrada.placeholder = exemplo;
  entrada.className = "painel-whatsapp__campo";
  entrada.setAttribute("aria-label", rotulo);
  return entrada;
}

function criarBotao(texto: string, classe: string): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = classe.includes("btn-primary") ? "submit" : "button";
  botao.className = classe;
  botao.textContent = texto;
  return botao;
}

function criarParagrafo(texto: string): HTMLParagraphElement {
  const paragrafo = document.createElement("p");
  paragrafo.className = "painel-whatsapp__texto";
  paragrafo.textContent = texto;
  return paragrafo;
}

function criarErro(): HTMLParagraphElement {
  const erro = document.createElement("p");
  erro.className = "estado-erro";
  erro.setAttribute("role", "alert");
  erro.hidden = true;
  return erro;
}

function montarBloco(...filhos: HTMLElement[]): HTMLElement {
  const bloco = document.createElement("div");
  bloco.className = "painel-whatsapp__bloco";
  bloco.append(...filhos);
  return bloco;
}
