import type { EstadoWizard } from "../../state/pedidoWizardState.js";
import { formatarMoeda } from "../formatarMoeda.js";

/**
 * Painel lateral, visível em todas as etapas: dá contexto permanente (cliente, itens, total)
 * em vez de deixar a página com só um formulário estreito e o resto vazio.
 */
export function renderizarResumoPedido(container: HTMLElement, estado: EstadoWizard): void {
  const titulo = document.createElement("p");
  titulo.className = "wizard-resumo__titulo";
  titulo.textContent = "Resumo do pedido";

  const cliente = document.createElement("p");
  cliente.className = "wizard-resumo__cliente";
  cliente.textContent = estado.clienteNome ?? "Nenhum cliente selecionado ainda";

  const lista = document.createElement("div");
  lista.className = "wizard-resumo__itens";
  if (estado.itens.length === 0) {
    const vazio = document.createElement("p");
    vazio.className = "wizard-resumo__vazio";
    vazio.textContent = "Nenhum item adicionado ainda";
    lista.append(vazio);
  } else {
    for (const item of estado.itens) {
      lista.append(criarLinhaItem(item.descricao, item.quantidade, item.precoUnitario * item.quantidade));
    }
  }

  const valorTotal = estado.itens.reduce((soma, item) => soma + item.precoUnitario * item.quantidade, 0);
  const total = document.createElement("div");
  total.className = "wizard-resumo__total";

  const rotuloTotal = document.createElement("span");
  rotuloTotal.textContent = "Total";
  const valorTotalEl = document.createElement("span");
  valorTotalEl.textContent = formatarMoeda(valorTotal);
  total.append(rotuloTotal, valorTotalEl);

  container.replaceChildren(titulo, cliente, lista, total);
}

function criarLinhaItem(descricao: string, quantidade: number, subtotal: number): HTMLElement {
  const linha = document.createElement("div");
  linha.className = "wizard-resumo__linha";

  const descricaoEl = document.createElement("span");
  descricaoEl.textContent = `${quantidade}x ${descricao}`;

  const subtotalEl = document.createElement("span");
  subtotalEl.textContent = formatarMoeda(subtotal);

  linha.append(descricaoEl, subtotalEl);
  return linha;
}
