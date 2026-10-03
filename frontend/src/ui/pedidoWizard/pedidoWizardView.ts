import { montarProgresso } from "./progressoView.js";
import { montarEtapaCliente } from "./etapaClienteView.js";
import { montarEtapaItens } from "./etapaItensView.js";
import { montarEtapaRevisao } from "./etapaRevisaoView.js";
import { montarEtapaPagamento } from "./etapaPagamentoView.js";
import { renderizarResumoPedido } from "./resumoPedidoView.js";
import { criarPedido, confirmarPedido } from "../../api/pedidosApi.js";
import {
  aoMudar,
  definirPedidoCriado,
  irParaEtapa,
  obterEstado,
  reiniciar,
  totalEtapas,
} from "../../state/pedidoWizardState.js";

const TITULOS_ETAPA = ["Cliente", "Itens", "Revisão", "Pagamento"];

export function montarWizardPedido(raiz: HTMLElement): void {
  reiniciar();

  const areaProgresso = document.createElement("div");
  const areaConteudo = document.createElement("div");
  areaConteudo.className = "wizard-conteudo";
  const areaErro = document.createElement("p");
  areaErro.className = "estado-erro";
  areaErro.hidden = true;
  const areaAcoes = document.createElement("div");
  areaAcoes.className = "wizard-acoes";

  const cartao = document.createElement("div");
  cartao.className = "wizard-card";
  cartao.append(areaProgresso, areaConteudo, areaErro, areaAcoes);

  const container = document.createElement("div");
  container.className = "wizard-container";
  container.append(cartao);

  const resumoLateral = document.createElement("aside");
  resumoLateral.className = "wizard-resumo";

  const layout = document.createElement("div");
  layout.className = "wizard-layout";
  layout.append(container, resumoLateral);

  const titulo = document.createElement("h1");
  titulo.textContent = "Novo pedido";

  raiz.replaceChildren(titulo, layout);

  aoMudar((estado) => renderizarResumoPedido(resumoLateral, estado));
  renderizarEtapaAtual();

  function renderizarEtapaAtual(): void {
    const estado = obterEstado();
    const titulo = TITULOS_ETAPA[estado.etapaAtual - 1] ?? "";
    montarProgresso(areaProgresso, estado.etapaAtual, totalEtapas(), titulo);
    areaErro.hidden = true;
    renderizarConteudo(estado.etapaAtual);
    renderizarAcoes(estado.etapaAtual);
    renderizarResumoPedido(resumoLateral, estado);
  }

  function renderizarConteudo(etapa: number): void {
    if (etapa === 1) {
      void montarEtapaCliente(areaConteudo);
    } else if (etapa === 2) {
      void montarEtapaItens(areaConteudo);
    } else if (etapa === 3) {
      montarEtapaRevisao(areaConteudo);
    } else if (etapa === 4) {
      montarEtapaPagamento(areaConteudo);
    }
  }

  function renderizarAcoes(etapa: number): void {
    areaAcoes.replaceChildren();

    if (etapa > 1 && etapa < totalEtapas()) {
      areaAcoes.append(criarBotaoVoltar(etapa));
    }

    if (etapa < totalEtapas()) {
      areaAcoes.append(criarBotaoProximo(etapa));
    } else {
      areaAcoes.append(criarBotaoNovoPedido());
    }
  }

  function criarBotaoVoltar(etapaAtual: number): HTMLButtonElement {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "btn btn-ghost";
    botao.textContent = "Voltar";
    botao.addEventListener("click", () => {
      irParaEtapa(etapaAtual - 1);
      renderizarEtapaAtual();
    });
    return botao;
  }

  function criarBotaoProximo(etapaAtual: number): HTMLButtonElement {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "btn btn-primary";
    botao.textContent = etapaAtual === 3 ? "Confirmar pedido" : "Continuar";
    botao.addEventListener("click", () => void avancar(etapaAtual, botao));
    return botao;
  }

  function criarBotaoNovoPedido(): HTMLButtonElement {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "btn btn-primary";
    botao.textContent = "Novo pedido";
    botao.addEventListener("click", () => {
      reiniciar();
      renderizarEtapaAtual();
    });
    return botao;
  }

  async function avancar(etapaAtual: number, botao: HTMLButtonElement): Promise<void> {
    const mensagemInvalida = validarEtapa(etapaAtual);
    if (mensagemInvalida !== null) {
      areaErro.textContent = mensagemInvalida;
      areaErro.hidden = false;
      return;
    }

    if (etapaAtual === 3) {
      await criarEConfirmarPedido(botao);
      return;
    }

    irParaEtapa(etapaAtual + 1);
    renderizarEtapaAtual();
  }

  function validarEtapa(etapa: number): string | null {
    const estado = obterEstado();
    if (etapa === 1 && estado.clienteId === null) {
      return "Selecione um cliente para continuar.";
    }
    if (etapa === 2 && estado.itens.length === 0) {
      return "Adicione ao menos um item para continuar.";
    }
    return null;
  }

  async function criarEConfirmarPedido(botao: HTMLButtonElement): Promise<void> {
    const estado = obterEstado();
    if (estado.clienteId === null) {
      return;
    }

    if (estado.pedidoCriado !== null) {
      irParaEtapa(4);
      renderizarEtapaAtual();
      return;
    }

    botao.disabled = true;
    try {
      const pedidoCriado = await criarPedido(
        estado.clienteId,
        estado.itens.map((item) => ({
          tipo: item.tipo,
          referenciaId: item.referenciaId,
          quantidade: item.quantidade,
        }))
      );
      const pedidoConfirmado = await confirmarPedido(pedidoCriado.id);
      definirPedidoCriado(pedidoConfirmado);
      irParaEtapa(4);
      renderizarEtapaAtual();
    } catch {
      areaErro.textContent = "Não foi possível confirmar o pedido. Tente novamente.";
      areaErro.hidden = false;
    } finally {
      botao.disabled = false;
    }
  }
}
