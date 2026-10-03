import { obterEstado } from "../../state/pedidoWizardState.js";
import { formatarMoeda } from "../formatarMoeda.js";

export function montarEtapaRevisao(container: HTMLElement): void {
  const estado = obterEstado();

  const titulo = document.createElement("h2");
  titulo.textContent = "Confira antes de confirmar";

  const cliente = document.createElement("p");
  cliente.className = "revisao-cliente";
  cliente.textContent = `Cliente: ${estado.clienteNome ?? ""}`;

  const lista = document.createElement("ul");
  lista.className = "revisao-itens";
  for (const item of estado.itens) {
    const linha = document.createElement("li");

    const descricao = document.createElement("span");
    descricao.textContent = `${item.quantidade}x ${item.descricao}`;

    const subtotal = document.createElement("span");
    subtotal.textContent = formatarMoeda(item.precoUnitario * item.quantidade);

    linha.append(descricao, subtotal);
    lista.append(linha);
  }

  const valorTotal = estado.itens.reduce((soma, item) => soma + item.precoUnitario * item.quantidade, 0);
  const total = document.createElement("p");
  total.className = "revisao-total";
  total.textContent = `Total: ${formatarMoeda(valorTotal)}`;

  container.replaceChildren(titulo, cliente, lista, total);
}
