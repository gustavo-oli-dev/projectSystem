import {
  alterarContato,
  cadastrarContato,
  definirContatoAtivo,
  listarContatos,
  type Contato,
  type DadosContato,
  type TipoContato,
} from "../../api/comprasApi.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";

const ROTULO_TIPO: Record<TipoContato, string> = {
  FORNECEDOR: "Fornecedor",
  TRANSPORTADORA: "Transportadora / frete",
  OUTRO: "Outro",
};

/**
 * Contatos (D34): fornecedores, transportadoras e outros. O fornecedor de uma nota lançada por
 * XML entra aqui sozinho (pelo CNPJ).
 */
export async function montarContatos(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Contatos";
  const novo = document.createElement("button");
  novo.type = "button";
  novo.className = "btn btn-primary";
  novo.textContent = "Novo contato";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, novo);
  const formulario = document.createElement("div");
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, formulario, area);

  const recarregar = async (): Promise<void> => {
    area.replaceChildren(elementoCarregando("Carregando contatos..."));
    try {
      const contatos = await listarContatos();
      area.replaceChildren(contatos.length === 0
        ? cartaoEstado("Nenhum contato ainda. Fornecedores de notas lançadas por XML entram aqui sozinhos.")
        : criarLista(contatos, (contato) => abrirFormulario(contato), () => void recarregar()));
    } catch {
      area.replaceChildren(cartaoEstado("Não foi possível carregar os contatos.", "erro"));
    }
  };
  const abrirFormulario = (contato: Contato | null): void => {
    novo.hidden = true;
    formulario.replaceChildren(criarFormulario(contato, () => {
      formulario.replaceChildren();
      novo.hidden = false;
      void recarregar();
    }));
  };
  novo.addEventListener("click", () => abrirFormulario(null));
  await recarregar();
}

function criarLista(contatos: readonly Contato[], aoEditar: (contato: Contato) => void, aoMudar: () => void): HTMLElement {
  return criarTabela(
    ["Nome", "Tipo", "CNPJ / CPF", "Telefone", "E-mail", "Situação", ""],
    contatos.map((contato) => {
      const editar = document.createElement("button");
      editar.type = "button";
      editar.className = "btn btn-ghost btn-pequeno";
      editar.textContent = "Editar";
      editar.addEventListener("click", () => aoEditar(contato));
      const alternar = document.createElement("button");
      alternar.type = "button";
      alternar.className = contato.ativo ? "btn btn-perigo btn-pequeno" : "btn btn-ghost btn-pequeno";
      alternar.textContent = contato.ativo ? "Desativar" : "Reativar";
      alternar.addEventListener("click", () => {
        alternar.disabled = true;
        definirContatoAtivo(contato.id, !contato.ativo).then(aoMudar).catch((falha: unknown) => {
          window.alert(falha instanceof Error && falha.message !== "" ? falha.message : "Não foi possível alterar o contato.");
          alternar.disabled = false;
        });
      });
      const acoes = document.createElement("div");
      acoes.className = "contas__acoes";
      acoes.append(editar, alternar);
      return criarLinha(
        celula(contato.nome),
        celula(ROTULO_TIPO[contato.tipo]),
        celula(formatarDocumento(contato.documento)),
        celula(contato.telefone ?? "—"),
        celula(contato.email ?? "—"),
        contato.ativo ? celulaSelo("Ativo", "ativo") : celulaSelo("Desativado", "inativo"),
        celulaComConteudo(acoes)
      );
    }),
    "contato(s)"
  );
}

function criarFormulario(contato: Contato | null, aoTerminar: () => void): HTMLElement {
  const tipo = criarCampoSelecao("contato-tipo", "Tipo", Object.entries(ROTULO_TIPO).map(([valor, rotulo]) => ({ valor, rotulo })));
  tipo.selecao.value = contato?.tipo ?? "FORNECEDOR";
  const nome = criarCampoTexto("contato-nome", "Nome / razão social", "text", true);
  nome.entrada.maxLength = 150;
  nome.entrada.value = contato?.nome ?? "";
  const documento = criarCampoTexto("contato-documento", "CNPJ ou CPF (opcional)", "text", false);
  documento.entrada.maxLength = 18;
  documento.entrada.value = contato?.documento ?? "";
  const telefone = criarCampoTexto("contato-telefone", "Telefone (opcional)", "tel", false);
  telefone.entrada.maxLength = 30;
  telefone.entrada.value = contato?.telefone ?? "";
  const email = criarCampoTexto("contato-email", "E-mail (opcional)", "email", false);
  email.entrada.maxLength = 150;
  email.entrada.value = contato?.email ?? "";
  const observacao = criarCampoTexto("contato-observacao", "Observação (opcional)", "text", false);
  observacao.entrada.maxLength = 500;
  observacao.entrada.value = contato?.observacao ?? "";

  const titulo = document.createElement("h2");
  titulo.textContent = contato === null ? "Novo contato" : `Editar ${contato.nome}`;
  const erro = criarMensagemErro();
  const salvar = document.createElement("button");
  salvar.type = "submit";
  salvar.className = "btn btn-primary";
  salvar.textContent = "Salvar";
  const voltar = document.createElement("button");
  voltar.type = "button";
  voltar.className = "btn btn-ghost";
  voltar.textContent = "Voltar";
  voltar.addEventListener("click", aoTerminar);
  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(voltar, salvar);

  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao contas__formulario";
  formulario.append(titulo, tipo.container, nome.container, documento.container, telefone.container,
    email.container, observacao.container, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    salvar.disabled = true;
    const dados: DadosContato = {
      tipo: tipo.selecao.value as TipoContato,
      nome: nome.entrada.value.trim(),
      documento: textoOuNulo(documento.entrada.value),
      telefone: textoOuNulo(telefone.entrada.value),
      email: textoOuNulo(email.entrada.value),
      observacao: textoOuNulo(observacao.entrada.value),
    };
    (contato === null ? cadastrarContato(dados) : alterarContato(contato.id, dados))
      .then(aoTerminar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível salvar o contato.");
        salvar.disabled = false;
      });
  });
  queueMicrotask(() => nome.entrada.focus());
  return formulario;
}

/** 11222333000181 → 11.222.333/0001-81; 11144477735 → 111.444.777-35. */
function formatarDocumento(documento: string | null): string {
  if (documento === null) {
    return "—";
  }
  if (documento.length === 14) {
    return `${documento.slice(0, 2)}.${documento.slice(2, 5)}.${documento.slice(5, 8)}/${documento.slice(8, 12)}-${documento.slice(12)}`;
  }
  if (documento.length === 11) {
    return `${documento.slice(0, 3)}.${documento.slice(3, 6)}.${documento.slice(6, 9)}-${documento.slice(9)}`;
  }
  return documento;
}
