import { criarCobranca, type Cobranca, type MeioCobranca } from "../../api/cobrancasApi.js";
import { definirCobrancaCriada, obterEstado } from "../../state/pedidoWizardState.js";
import { elementoErro } from "../estadoCarregamento.js";
import { formatarMoeda } from "../formatarMoeda.js";

export function montarEtapaPagamento(container: HTMLElement): void {
  const estado = obterEstado();
  const pedido = estado.pedidoCriado;

  const titulo = document.createElement("h2");
  titulo.textContent = "Como o cliente vai pagar?";

  if (pedido === null) {
    container.replaceChildren(titulo, elementoErro("Pedido não foi criado. Volte e tente novamente."));
    return;
  }

  if (estado.cobrancaCriada !== null) {
    container.replaceChildren(titulo, ...montarResultadoCobranca(estado.cobrancaCriada));
    return;
  }

  const valor = document.createElement("p");
  valor.className = "pagamento-valor";
  valor.textContent = `Total a cobrar: ${formatarMoeda(pedido.valorTotal)}`;

  const erro = elementoErro("");
  erro.hidden = true;

  const opcoes = document.createElement("div");
  opcoes.className = "pagamento-opcoes";
  opcoes.append(
    criarBotaoMeioPagamento("PIX", "Pix", pedido.id, container, erro),
    criarBotaoMeioPagamento("BOLETO", "Boleto", pedido.id, container, erro)
  );

  container.replaceChildren(titulo, valor, opcoes, erro);
}

function criarBotaoMeioPagamento(
  meio: MeioCobranca,
  rotulo: string,
  pedidoId: string,
  container: HTMLElement,
  erro: HTMLParagraphElement
): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-outline pagamento-opcao";
  botao.textContent = rotulo;

  botao.addEventListener("click", () => void gerarCobranca(meio, pedidoId, botao, container, erro));

  return botao;
}

async function gerarCobranca(
  meio: MeioCobranca,
  pedidoId: string,
  botao: HTMLButtonElement,
  container: HTMLElement,
  erro: HTMLParagraphElement
): Promise<void> {
  erro.hidden = true;
  botao.disabled = true;
  try {
    const cobranca = await criarCobranca(pedidoId, meio);
    definirCobrancaCriada(cobranca);
    montarEtapaPagamento(container);
  } catch {
    erro.textContent = "Não foi possível gerar a cobrança. Tente novamente.";
    erro.hidden = false;
  } finally {
    botao.disabled = false;
  }
}

function montarResultadoCobranca(cobranca: Cobranca): HTMLElement[] {
  const mensagem = document.createElement("p");
  mensagem.className = "pagamento-sucesso";
  mensagem.textContent = "Cobrança gerada com sucesso.";

  const elementos: HTMLElement[] = [mensagem];

  if (cobranca.qrCodeCopiaECola !== null) {
    elementos.push(...criarBlocoCodigo("Pix copia e cola:", cobranca.qrCodeCopiaECola));
  }

  if (cobranca.linhaDigitavelBoleto !== null) {
    elementos.push(...criarBlocoCodigo("Linha digitável do boleto:", cobranca.linhaDigitavelBoleto));
  }

  return elementos;
}

function criarBlocoCodigo(rotuloTexto: string, valor: string): HTMLElement[] {
  const rotulo = document.createElement("p");
  rotulo.textContent = rotuloTexto;

  const codigo = document.createElement("code");
  codigo.className = "pagamento-codigo";
  codigo.textContent = valor;

  return [rotulo, codigo];
}
