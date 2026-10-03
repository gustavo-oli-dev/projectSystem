import { cadastrarCliente, listarClientes, type Cliente, type NovoCliente } from "../../api/clientesApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";

interface Campo {
  container: HTMLDivElement;
  entrada: HTMLInputElement;
}

export async function montarListaClientes(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Clientes";

  const botaoNovo = document.createElement("button");
  botaoNovo.type = "button";
  botaoNovo.className = "btn btn-primary btn-pequeno";
  botaoNovo.hidden = !possui("CLIENTES_GERENCIAR");
  botaoNovo.textContent = "+ Novo cliente";

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, botaoNovo);

  const areaFormulario = document.createElement("div");
  const areaLista = document.createElement("div");

  container.replaceChildren(cabecalho, areaFormulario, areaLista);

  botaoNovo.addEventListener("click", () => {
    if (areaFormulario.childElementCount > 0) {
      areaFormulario.replaceChildren();
      return;
    }
    areaFormulario.append(
      criarFormulario(async (novoCliente) => {
        await cadastrarCliente(novoCliente);
        areaFormulario.replaceChildren();
        await carregar(areaLista);
      })
    );
  });

  await carregar(areaLista);
}

async function carregar(areaLista: HTMLElement): Promise<void> {
  areaLista.replaceChildren(elementoCarregando("Carregando clientes..."));
  try {
    const clientes = await listarClientes();
    renderizarLista(areaLista, clientes);
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os clientes.", "erro"));
  }
}

function renderizarLista(areaLista: HTMLElement, clientes: Cliente[]): void {
  if (clientes.length === 0) {
    areaLista.replaceChildren(cartaoEstado("Nenhum cliente cadastrado ainda."));
    return;
  }

  const linhas = clientes.map((cliente) =>
    criarLinha(celula(cliente.nome), celula(cliente.documento), celula(cliente.telefoneWhatsapp))
  );

  areaLista.replaceChildren(criarTabela(["Nome", "Documento", "WhatsApp"], linhas, "cliente(s)"));
}

function criarFormulario(aoSalvar: (cliente: NovoCliente) => Promise<void>): HTMLFormElement {
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao";

  const nome = criarCampo("cliente-nome", "Nome", "text", true);
  const documento = criarCampo("cliente-documento", "CPF ou CNPJ", "text", true);
  const telefone = criarCampo("cliente-telefone", "Telefone do WhatsApp", "text", true);

  const erro = document.createElement("p");
  erro.className = "estado-erro";
  erro.hidden = true;

  const botaoSalvar = document.createElement("button");
  botaoSalvar.type = "submit";
  botaoSalvar.className = "btn btn-primary btn-pequeno";
  botaoSalvar.textContent = "Salvar";

  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(botaoSalvar);

  formulario.append(nome.container, documento.container, telefone.container, erro, acoes);

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    botaoSalvar.disabled = true;

    aoSalvar({
      nome: nome.entrada.value,
      documento: documento.entrada.value,
      telefoneWhatsapp: telefone.entrada.value,
    })
      .catch(() => {
        erro.textContent = "Não foi possível salvar o cliente. Confira o CPF/CNPJ e tente novamente.";
        erro.hidden = false;
      })
      .finally(() => {
        botaoSalvar.disabled = false;
      });
  });

  return formulario;
}

function criarCampo(id: string, rotuloTexto: string, tipo: string, obrigatorio: boolean): Campo {
  const container = document.createElement("div");
  container.className = "campo-formulario";

  const rotulo = document.createElement("label");
  rotulo.htmlFor = id;
  rotulo.textContent = rotuloTexto;

  const entrada = document.createElement("input");
  entrada.id = id;
  entrada.type = tipo;
  entrada.required = obrigatorio;

  container.append(rotulo, entrada);
  return { container, entrada };
}
