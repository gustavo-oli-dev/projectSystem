import type { BandeiraCartao, FormaPagamentoPresencial, PagamentoPresencial } from "../../api/pdvApi.js";
import { criarCampoSelecao, criarCampoTexto } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { BANDEIRAS } from "./bandeirasCartao.js";
import { criarPartesPagamento } from "./partesPagamentoView.js";

type FormaNoCaixa = "DINHEIRO" | "CARTAO_CREDITO" | "CARTAO_DEBITO" | "PIX_QR";

/**
 * Como a venda vai ser fechada:
 * - DINHEIRO: valor recebido e troco.
 * - MAQUININHA: o valor vai para a maquininha integrada; o resultado volta sozinho.
 * - CONTINGENCIA: maquininha sem conexão — o operador digita bandeira e autorização.
 * - PIX_QR: QR code na tela, pago pelo celular do cliente.
 */
export type ModoFechamento = "DINHEIRO" | "MAQUININHA" | "CONTINGENCIA" | "PIX_QR";

const FORMAS: ReadonlyArray<{ forma: FormaNoCaixa; rotulo: string }> = [
  { forma: "DINHEIRO", rotulo: "Dinheiro" },
  { forma: "CARTAO_CREDITO", rotulo: "Crédito" },
  { forma: "CARTAO_DEBITO", rotulo: "Débito" },
  { forma: "PIX_QR", rotulo: "Pix" },
];

const ROTULO_FINALIZAR: Record<ModoFechamento, string> = {
  DINHEIRO: "Finalizar venda",
  MAQUININHA: "Enviar para a maquininha",
  CONTINGENCIA: "Finalizar venda",
  PIX_QR: "Gerar QR code do Pix",
};

export interface PainelPagamento {
  elemento: HTMLElement;
  /** Chamar quando o total mudar, para recalcular o troco. */
  atualizarTotal: (total: number) => void;
  /** Partes já recebidas do pagamento dividido (vazio = uma forma só). */
  lerPartes: () => PagamentoPresencial[];
  modo: () => ModoFechamento;
  /** Crédito ou débito escolhido (para a maquininha). */
  formaCartao: () => FormaPagamentoPresencial;
  /** Dados do pagamento em dinheiro ou contingência. */
  lerPagamento: () => PagamentoPresencial;
  rotuloFinalizar: () => string;
  /** Avisa quando o modo muda (o botão de finalizar troca o texto). */
  aoMudarModo: (ouvinte: () => void) => void;
}

export function criarPainelPagamento(): PainelPagamento {
  let formaEscolhida: FormaNoCaixa = "DINHEIRO";
  let emContingencia = false;
  let totalAtual = 0;
  let ouvinteModo: () => void = () => undefined;
  const partes = criarPartesPagamento();
  // Troco e maquininha valem para o que falta depois das partes já recebidas.
  const restante = (): number => totalAtual - partes.soma();

  const botoes = FORMAS.map((opcao) => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "forma-pagamento";
    botao.textContent = opcao.rotulo;
    botao.setAttribute("role", "radio");
    botao.addEventListener("click", () => escolher(opcao.forma));
    return { opcao, botao };
  });
  const grupoFormas = document.createElement("div");
  grupoFormas.className = "formas-pagamento";
  grupoFormas.setAttribute("role", "radiogroup");
  grupoFormas.setAttribute("aria-label", "Forma de pagamento");
  grupoFormas.append(...botoes.map((item) => item.botao));

  // Dinheiro: valor recebido + caixa de troco (recebido − total), sempre visível.
  const recebido = criarCampoTexto("pdv-recebido", "Valor recebido (R$)", "number", false);
  recebido.entrada.step = "0.01";
  recebido.entrada.min = "0";
  recebido.entrada.inputMode = "decimal";
  const rotuloTroco = document.createElement("span");
  rotuloTroco.className = "caixa-troco__rotulo";
  rotuloTroco.textContent = "Troco";
  const valorTroco = document.createElement("strong");
  valorTroco.className = "caixa-troco__valor";
  const caixaTroco = document.createElement("div");
  caixaTroco.className = "caixa-troco";
  caixaTroco.setAttribute("aria-live", "polite");
  caixaTroco.append(rotuloTroco, valorTroco);
  recebido.entrada.addEventListener("input", () => atualizarTroco());
  const blocoDinheiro = document.createElement("div");
  blocoDinheiro.className = "bloco-dinheiro";
  blocoDinheiro.append(recebido.container, caixaTroco);

  // Cartão: maquininha integrada; contingência escondida atrás de um link.
  const avisoMaquininha = document.createElement("p");
  avisoMaquininha.className = "nota-campo";
  avisoMaquininha.textContent = "Ao clicar em \"Enviar para a maquininha\", o valor aparece nela. O cliente passa o cartão e a venda se confirma sozinha.";
  const linkContingencia = document.createElement("button");
  linkContingencia.type = "button";
  linkContingencia.className = "link-discreto";
  const bandeira = criarCampoSelecao("pdv-bandeira", "Bandeira", BANDEIRAS);
  const autorizacao = criarCampoTexto("pdv-autorizacao", "Código de autorização (no comprovante)", "text", false);
  autorizacao.entrada.maxLength = 20;
  autorizacao.entrada.autocomplete = "off";
  const camposContingencia = document.createElement("div");
  camposContingencia.className = "contingencia";
  const avisoContingencia = document.createElement("p");
  avisoContingencia.className = "nota-campo";
  avisoContingencia.textContent = "Contingência: passe o cartão na maquininha (modo avulso) e digite os dados do comprovante.";
  camposContingencia.append(avisoContingencia, bandeira.container, autorizacao.container);
  linkContingencia.addEventListener("click", () => {
    emContingencia = !emContingencia;
    atualizarVisibilidade();
  });
  const blocoCartao = document.createElement("div");
  blocoCartao.append(avisoMaquininha, linkContingencia, camposContingencia);

  const blocoPix = document.createElement("p");
  blocoPix.className = "nota-campo";
  blocoPix.textContent = "O QR code aparece na tela para o cliente pagar pelo celular. A venda se confirma sozinha quando o Pix cair.";

  const elemento = document.createElement("div");
  elemento.className = "painel-pagamento";
  elemento.append(partes.elemento, grupoFormas, blocoDinheiro, blocoCartao, blocoPix);

  // Pix com QR na tela cobra a venda inteira no Mercado Pago: não entra no pagamento dividido.
  const botaoPixNaTela = botoes.find(({ opcao }) => opcao.forma === "PIX_QR")?.botao;
  partes.aoMudar(() => {
    const dividido = partes.partes().length > 0;
    if (botaoPixNaTela !== undefined) {
      botaoPixNaTela.hidden = dividido;
    }
    if (dividido && formaEscolhida === "PIX_QR") {
      escolher("DINHEIRO");
    }
    atualizarTroco();
  });

  function modoAtual(): ModoFechamento {
    if (formaEscolhida === "DINHEIRO" || formaEscolhida === "PIX_QR") {
      return formaEscolhida;
    }
    return emContingencia ? "CONTINGENCIA" : "MAQUININHA";
  }

  function escolher(forma: FormaNoCaixa): void {
    formaEscolhida = forma;
    botoes.forEach(({ opcao, botao }) => {
      const ativa = opcao.forma === forma;
      botao.classList.toggle("forma-pagamento--ativa", ativa);
      botao.setAttribute("aria-checked", String(ativa));
    });
    atualizarVisibilidade();
  }

  function atualizarVisibilidade(): void {
    const cartao = formaEscolhida === "CARTAO_CREDITO" || formaEscolhida === "CARTAO_DEBITO";
    blocoDinheiro.hidden = formaEscolhida !== "DINHEIRO";
    blocoCartao.hidden = !cartao;
    blocoPix.hidden = formaEscolhida !== "PIX_QR";
    avisoMaquininha.hidden = emContingencia;
    camposContingencia.hidden = !emContingencia;
    linkContingencia.textContent = emContingencia
      ? "Voltar para a maquininha integrada"
      : "Maquininha sem conexão? Lançar manualmente";
    ouvinteModo();
  }

  function atualizarTroco(): void {
    const digitado = recebido.entrada.value;
    const valor = Number(digitado);
    const aPagar = restante();
    caixaTroco.classList.toggle("caixa-troco--falta", digitado !== "" && valor < aPagar);
    if (digitado === "") {
      valorTroco.textContent = formatarMoeda(0);
    } else if (valor < aPagar) {
      valorTroco.textContent = `Falta ${formatarMoeda(aPagar - valor)}`;
    } else {
      valorTroco.textContent = formatarMoeda(valor - aPagar);
    }
  }

  escolher("DINHEIRO");
  atualizarTroco();

  return {
    elemento,
    atualizarTotal: (total) => {
      totalAtual = total;
      partes.atualizarTotal(total);
      atualizarTroco();
    },
    lerPartes: () => [...partes.partes()],
    modo: modoAtual,
    formaCartao: () => (formaEscolhida === "CARTAO_DEBITO" ? "CARTAO_DEBITO" : "CARTAO_CREDITO"),
    lerPagamento: () => (formaEscolhida === "DINHEIRO"
      ? {
        forma: "DINHEIRO",
        valor: null,
        valorRecebido: recebido.entrada.value === "" ? null : Number(recebido.entrada.value),
        bandeira: null,
        codigoAutorizacao: null,
      }
      : {
        forma: formaEscolhida === "CARTAO_DEBITO" ? "CARTAO_DEBITO" : "CARTAO_CREDITO",
        valor: null,
        valorRecebido: null,
        // Valor vem de uma lista fixa de opções (BANDEIRAS), então sempre é uma bandeira válida.
        bandeira: bandeira.selecao.value as BandeiraCartao,
        codigoAutorizacao: autorizacao.entrada.value.trim() || null,
      }),
    rotuloFinalizar: () => ROTULO_FINALIZAR[modoAtual()],
    aoMudarModo: (ouvinte) => {
      ouvinteModo = ouvinte;
    },
  };
}
