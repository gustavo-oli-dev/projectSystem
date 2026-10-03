import type { BandeiraCartao, FormaVendaBalcao, PagamentoPresencial } from "../../api/pdvApi.js";
import { criarCampoTexto, criarCampoSelecao, type OpcaoSelecao } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";

interface OpcaoForma {
  forma: FormaVendaBalcao;
  rotulo: string;
}

const FORMAS: readonly OpcaoForma[] = [
  { forma: "DINHEIRO", rotulo: "Dinheiro" },
  { forma: "CARTAO_CREDITO", rotulo: "Crédito" },
  { forma: "CARTAO_DEBITO", rotulo: "Débito" },
  { forma: "PIX_QR", rotulo: "Pix (QR na tela)" },
  { forma: "PIX", rotulo: "Pix (maquininha)" },
];

const BANDEIRAS: readonly OpcaoSelecao[] = [
  { valor: "VISA", rotulo: "Visa" },
  { valor: "MASTERCARD", rotulo: "Mastercard" },
  { valor: "ELO", rotulo: "Elo" },
  { valor: "AMERICAN_EXPRESS", rotulo: "American Express" },
  { valor: "HIPERCARD", rotulo: "Hipercard" },
  { valor: "OUTRA", rotulo: "Outra" },
];

export interface PainelPagamento {
  elemento: HTMLElement;
  /** Chamar quando o total mudar, para recalcular o troco. */
  atualizarTotal: (total: number) => void;
  /** Pix com QR na tela segue outro fluxo (cobrança no Mercado Pago), sem dados presenciais. */
  pixNaTela: () => boolean;
  lerPagamento: () => PagamentoPresencial;
  limpar: () => void;
}

/**
 * Forma de pagamento do caixa. Maquininha ainda não integrada (fornecedor a definir, D19): o
 * operador passa o cartão na maquininha e digita a autorização do comprovante.
 */
export function criarPainelPagamento(): PainelPagamento {
  let formaEscolhida: FormaVendaBalcao = "DINHEIRO";
  let totalAtual = 0;

  const botoes = FORMAS.map((opcao) => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "forma-pagamento";
    botao.textContent = opcao.rotulo;
    botao.addEventListener("click", () => escolher(opcao.forma));
    return { opcao, botao };
  });

  const grupoFormas = document.createElement("div");
  grupoFormas.className = "formas-pagamento";
  grupoFormas.setAttribute("role", "radiogroup");
  grupoFormas.append(...botoes.map((item) => item.botao));

  const recebido = criarCampoTexto("pdv-recebido", "Valor recebido (R$)", "number", false);
  recebido.entrada.step = "0.01";
  recebido.entrada.min = "0";
  const troco = document.createElement("p");
  troco.className = "troco";
  recebido.entrada.addEventListener("input", () => atualizarTroco());

  const bandeira = criarCampoSelecao("pdv-bandeira", "Bandeira", BANDEIRAS);
  const autorizacao = criarCampoTexto("pdv-autorizacao", "Código de autorização (no comprovante)", "text", false);
  autorizacao.entrada.maxLength = 20;
  autorizacao.entrada.autocomplete = "off";
  const avisoMaquininha = document.createElement("p");
  avisoMaquininha.className = "nota-campo";
  avisoMaquininha.textContent = "Maquininha ainda não integrada: passe o valor na maquininha e digite aqui o código de autorização.";

  const blocoPixNaTela = document.createElement("p");
  blocoPixNaTela.className = "nota-campo";
  blocoPixNaTela.textContent = "Ao finalizar, o QR code aparece na tela para o cliente pagar pelo celular. A venda se confirma sozinha quando o Pix cair.";

  const blocoDinheiro = document.createElement("div");
  blocoDinheiro.append(recebido.container, troco);
  const blocoMaquininha = document.createElement("div");
  blocoMaquininha.append(avisoMaquininha, bandeira.container, autorizacao.container);

  const elemento = document.createElement("div");
  elemento.className = "painel-pagamento";
  elemento.append(grupoFormas, blocoDinheiro, blocoMaquininha, blocoPixNaTela);

  function escolher(forma: FormaVendaBalcao): void {
    formaEscolhida = forma;
    botoes.forEach(({ opcao, botao }) => {
      const ativa = opcao.forma === forma;
      botao.classList.toggle("forma-pagamento--ativa", ativa);
      botao.setAttribute("aria-checked", String(ativa));
    });
    blocoDinheiro.hidden = forma !== "DINHEIRO";
    blocoMaquininha.hidden = forma === "DINHEIRO" || forma === "PIX_QR";
    blocoPixNaTela.hidden = forma !== "PIX_QR";
    bandeira.container.hidden = forma === "PIX";
    autorizacao.container.querySelector("label")?.replaceChildren(
      forma === "PIX" ? "Identificador do Pix (no comprovante)" : "Código de autorização (no comprovante)");
  }

  function atualizarTroco(): void {
    const valor = Number(recebido.entrada.value);
    troco.textContent = recebido.entrada.value === "" || valor < totalAtual
      ? ""
      : `Troco: ${formatarMoeda(valor - totalAtual)}`;
  }

  escolher("DINHEIRO");

  return {
    elemento,
    atualizarTotal: (total) => {
      totalAtual = total;
      atualizarTroco();
    },
    pixNaTela: () => formaEscolhida === "PIX_QR",
    lerPagamento: () => ({
      forma: formaEscolhida === "PIX_QR" ? "PIX" : formaEscolhida,
      valorRecebido: formaEscolhida === "DINHEIRO" && recebido.entrada.value !== "" ? Number(recebido.entrada.value) : null,
      bandeira: formaEscolhida === "CARTAO_CREDITO" || formaEscolhida === "CARTAO_DEBITO"
        ? bandeira.selecao.value as BandeiraCartao
        : null,
      codigoAutorizacao: formaEscolhida === "DINHEIRO" ? null : autorizacao.entrada.value.trim() || null,
    }),
    limpar: () => {
      recebido.entrada.value = "";
      autorizacao.entrada.value = "";
      troco.textContent = "";
      escolher("DINHEIRO");
    },
  };
}
