import { listarClientes, type Cliente } from "../../api/clientesApi.js";
import { definirCliente, obterEstado } from "../../state/pedidoWizardState.js";
import { elementoCarregando, elementoErro, elementoVazio } from "../estadoCarregamento.js";

export async function montarEtapaCliente(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h2");
  titulo.textContent = "Quem é o cliente?";

  container.replaceChildren(titulo, elementoCarregando("Carregando clientes..."));

  try {
    const clientes = await listarClientes();
    renderizarEtapa(container, titulo, clientes);
  } catch {
    container.replaceChildren(titulo, elementoErro("Não foi possível carregar os clientes."));
  }
}

function renderizarEtapa(container: HTMLElement, titulo: HTMLElement, clientes: Cliente[]): void {
  if (clientes.length === 0) {
    container.replaceChildren(titulo, elementoVazio("Nenhum cliente cadastrado ainda."));
    return;
  }

  const busca = document.createElement("input");
  busca.type = "search";
  busca.placeholder = "Buscar por nome ou documento";
  busca.className = "campo-busca";

  const lista = document.createElement("div");
  lista.className = "lista-selecionavel";
  renderizarLista(lista, clientes);

  busca.addEventListener("input", () => {
    const termo = busca.value.trim().toLowerCase();
    const filtrados = clientes.filter(
      (cliente) => cliente.nome.toLowerCase().includes(termo) || cliente.documento.includes(termo)
    );
    renderizarLista(lista, filtrados);
  });

  container.replaceChildren(titulo, busca, lista);
}

function renderizarLista(lista: HTMLElement, clientes: Cliente[]): void {
  if (clientes.length === 0) {
    lista.replaceChildren(elementoVazio("Nenhum cliente encontrado."));
    return;
  }

  lista.replaceChildren(...clientes.map((cliente) => criarItemCliente(cliente, lista)));
}

function criarItemCliente(cliente: Cliente, lista: HTMLElement): HTMLButtonElement {
  const item = document.createElement("button");
  item.type = "button";
  item.className = "item-selecionavel";
  if (obterEstado().clienteId === cliente.id) {
    item.classList.add("item-selecionavel--selecionado");
  }

  const nome = document.createElement("span");
  nome.className = "item-selecionavel__titulo";
  nome.textContent = cliente.nome;

  const documento = document.createElement("span");
  documento.className = "item-selecionavel__detalhe";
  documento.textContent = cliente.documento;

  item.append(nome, documento);
  item.addEventListener("click", () => {
    definirCliente(cliente.id, cliente.nome);
    lista.querySelectorAll(".item-selecionavel").forEach((elemento) => {
      elemento.classList.remove("item-selecionavel--selecionado");
    });
    item.classList.add("item-selecionavel--selecionado");
  });

  return item;
}
