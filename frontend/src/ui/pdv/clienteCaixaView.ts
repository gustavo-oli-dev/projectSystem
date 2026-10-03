import { cadastrarCliente, listarClientes, type Cliente } from "../../api/clientesApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";

const MAXIMO_SUGESTOES = 6;
const DIGITOS_CPF = 11;

export interface SeletorClienteCaixa {
  elemento: HTMLElement;
  clienteId: () => string | null;
}

/**
 * Cliente da venda (opcional): busca por nome, CPF/CNPJ ou WhatsApp entre os cadastrados, ou
 * cadastro rápido. Ao escolher um cliente com CPF, o CPF na nota é preenchido.
 */
export function criarSeletorCliente(aoEscolherCpf: (cpf: string) => void): SeletorClienteCaixa {
  let escolhido: Cliente | null = null;
  let clientes: Cliente[] = [];

  const elemento = document.createElement("div");
  elemento.className = "cliente-caixa";

  if (!possui("CLIENTES_VER")) {
    return { elemento, clienteId: () => null };
  }

  const titulo = document.createElement("p");
  titulo.className = "subtitulo-bloco";
  titulo.textContent = "Cliente (opcional)";

  const busca = document.createElement("input");
  busca.type = "search";
  busca.placeholder = "Buscar por nome, CPF ou WhatsApp";
  busca.setAttribute("aria-label", "Buscar cliente");
  busca.autocomplete = "off";

  const sugestoes = document.createElement("div");
  sugestoes.className = "leitura__sugestoes";

  const escolhidoEl = document.createElement("div");
  escolhidoEl.className = "cliente-caixa__escolhido";

  const areaCadastro = document.createElement("div");

  const renderizar = (): void => {
    busca.hidden = escolhido !== null;
    sugestoes.replaceChildren();
    escolhidoEl.replaceChildren();
    if (escolhido === null) {
      return;
    }
    const dados = document.createElement("div");
    const nome = document.createElement("strong");
    nome.textContent = escolhido.nome;
    const detalhe = document.createElement("span");
    detalhe.textContent = `${escolhido.documento} · ${escolhido.telefoneWhatsapp}`;
    dados.append(nome, detalhe);
    const trocar = document.createElement("button");
    trocar.type = "button";
    trocar.className = "btn btn-ghost btn-pequeno";
    trocar.textContent = "Trocar";
    trocar.addEventListener("click", () => {
      escolhido = null;
      renderizar();
      busca.focus();
    });
    escolhidoEl.append(dados, trocar);
  };

  const escolher = (cliente: Cliente): void => {
    escolhido = cliente;
    busca.value = "";
    areaCadastro.replaceChildren();
    if (cliente.documento.replace(/\D/g, "").length === DIGITOS_CPF) {
      aoEscolherCpf(cliente.documento);
    }
    renderizar();
  };

  busca.addEventListener("input", () => {
    const termo = busca.value.trim().toLowerCase();
    const termoDigitos = termo.replace(/\D/g, "");
    if (termo.length < 2) {
      sugestoes.replaceChildren();
      return;
    }
    const achados = clientes.filter((cliente) =>
      cliente.nome.toLowerCase().includes(termo)
      || (termoDigitos.length >= 3 && (cliente.documento.includes(termoDigitos) || cliente.telefoneWhatsapp.includes(termoDigitos)))
    ).slice(0, MAXIMO_SUGESTOES);
    sugestoes.replaceChildren(...achados.map((cliente) => criarSugestao(cliente, () => escolher(cliente))));
  });

  elemento.append(titulo, busca, sugestoes, escolhidoEl);

  if (possui("CLIENTES_GERENCIAR")) {
    const novo = document.createElement("button");
    novo.type = "button";
    novo.className = "link-discreto";
    novo.textContent = "+ Cadastrar cliente novo";
    novo.addEventListener("click", () => {
      areaCadastro.replaceChildren(criarCadastroRapido(escolher, (cliente) => {
        clientes = [...clientes, cliente];
      }));
    });
    elemento.append(novo, areaCadastro);
  }

  listarClientes()
    .then((lista) => {
      clientes = lista;
    })
    .catch(() => {
      busca.placeholder = "Não foi possível carregar os clientes";
      busca.disabled = true;
    });

  return { elemento, clienteId: () => escolhido?.id ?? null };
}

function criarSugestao(cliente: Cliente, aoClicar: () => void): HTMLButtonElement {
  const nome = document.createElement("span");
  nome.textContent = cliente.nome;
  const detalhe = document.createElement("span");
  detalhe.className = "leitura__sugestao-detalhe";
  detalhe.textContent = cliente.documento;
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "leitura__sugestao";
  botao.append(nome, detalhe);
  botao.addEventListener("click", aoClicar);
  return botao;
}

/** Não é <form>: fica dentro da tela do caixa, e Enter aqui não deve finalizar a venda. */
function criarCadastroRapido(aoCadastrar: (cliente: Cliente) => void, aoIncluir: (cliente: Cliente) => void): HTMLElement {
  const nome = criarCampoTexto("caixa-cliente-nome", "Nome", "text", true);
  const documento = criarCampoTexto("caixa-cliente-documento", "CPF ou CNPJ", "text", true);
  const whatsapp = criarCampoTexto("caixa-cliente-whatsapp", "WhatsApp (com DDD)", "tel", true);
  const erro = criarMensagemErro();
  const salvar = document.createElement("button");
  salvar.type = "button";
  salvar.className = "btn btn-outline btn-pequeno";
  salvar.textContent = "Cadastrar e usar nesta venda";
  salvar.addEventListener("click", () => {
    erro.hidden = true;
    salvar.disabled = true;
    cadastrarCliente({
      nome: nome.entrada.value,
      documento: documento.entrada.value,
      telefoneWhatsapp: whatsapp.entrada.value.replace(/\D/g, ""),
    })
      .then((cliente) => {
        aoIncluir(cliente);
        aoCadastrar(cliente);
      })
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível cadastrar. Confira os dados.");
        salvar.disabled = false;
      });
  });
  const bloco = document.createElement("div");
  bloco.className = "cadastro-rapido";
  bloco.append(nome.container, documento.container, whatsapp.container, erro, salvar);
  return bloco;
}
