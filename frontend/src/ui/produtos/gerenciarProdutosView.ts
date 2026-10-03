import { cadastrarProduto, listarProdutos, type NovoProduto, type Produto } from "../../api/produtosApi.js";
import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { montarEdicaoProduto } from "./edicaoProdutoView.js";
import { criarImagemPrincipal } from "./imagemProduto.js";
import { situacaoEstoque } from "./situacaoEstoque.js";

/** "#/gerenciar-produtos" → lista; "#/gerenciar-produtos/<id>" → edição do produto. */
export async function montarGerenciarProdutos(container: HTMLElement, produtoId: string | null): Promise<void> {
  if (produtoId !== null) {
    await montarEdicaoProduto(container, produtoId);
    return;
  }

  const titulo = document.createElement("h1");
  titulo.textContent = "Gerenciar produtos";

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const areaFormulario = document.createElement("div");
  const areaLista = document.createElement("div");

  if (possui("CATALOGO_GERENCIAR")) {
    const botaoNovo = document.createElement("button");
    botaoNovo.type = "button";
    botaoNovo.className = "btn btn-primary btn-pequeno";
    botaoNovo.textContent = "+ Novo produto";
    botaoNovo.addEventListener("click", () => alternarFormulario(areaFormulario));
    cabecalho.append(botaoNovo);
  }

  container.replaceChildren(cabecalho, areaFormulario, areaLista);
  await carregar(areaLista);
}

function alternarFormulario(areaFormulario: HTMLElement): void {
  if (areaFormulario.childElementCount > 0) {
    areaFormulario.replaceChildren();
    return;
  }
  areaFormulario.append(
    criarFormularioNovoProduto(async (novo) => {
      const criado = await cadastrarProduto(novo);
      // Logo depois de criar, abre a edição para adicionar fotos e estoque.
      navegarPara("gerenciar-produtos", criado.id);
    })
  );
}

async function carregar(areaLista: HTMLElement): Promise<void> {
  areaLista.replaceChildren(elementoCarregando("Carregando produtos..."));
  try {
    renderizarLista(areaLista, await listarProdutos());
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
  }
}

function renderizarLista(areaLista: HTMLElement, produtos: Produto[]): void {
  if (produtos.length === 0) {
    areaLista.replaceChildren(cartaoEstado("Nenhum produto cadastrado ainda."));
    return;
  }

  const linhas = produtos.map((produto) => {
    const situacao = situacaoEstoque(produto.quantidadeEmEstoque);

    const botaoEditar = document.createElement("button");
    botaoEditar.type = "button";
    botaoEditar.className = "btn btn-outline btn-pequeno";
    botaoEditar.textContent = possui("CATALOGO_GERENCIAR") ? "Editar" : "Estoque";
    botaoEditar.addEventListener("click", () => navegarPara("gerenciar-produtos", produto.id));

    return criarLinha(
      celulaComConteudo(criarImagemPrincipal(produto, "miniatura-produto")),
      celula(produto.nome),
      celula(formatarMoeda(produto.precoUnitario)),
      celula(`${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`),
      celulaSelo(situacao.rotulo, situacao.modificador),
      celulaSelo(produto.ativo ? "À venda" : "Fora de venda", produto.ativo ? "ativo" : "inativo"),
      celulaComConteudo(botaoEditar)
    );
  });

  areaLista.replaceChildren(
    criarTabela(["", "Produto", "Preço", "Estoque", "Situação", "Venda", ""], linhas, "produto(s)")
  );
}

function criarFormularioNovoProduto(aoSalvar: (produto: NovoProduto) => Promise<void>): HTMLFormElement {
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao";

  const nome = criarCampoTexto("produto-nome", "Nome", "text", true);
  const descricao = criarCampoTexto("produto-descricao", "Descrição", "text", false);
  const preco = criarCampoTexto("produto-preco", "Preço de venda (R$)", "number", true);
  preco.entrada.step = "0.01";
  preco.entrada.min = "0";
  const ncm = criarCampoTexto("produto-ncm", "NCM (8 dígitos — classificação fiscal)", "text", true);
  ncm.entrada.inputMode = "numeric";
  ncm.entrada.maxLength = 8;
  const unidade = criarCampoTexto("produto-unidade", "Unidade (ex.: UN, KG, CX)", "text", true);
  const codigoBarras = criarCampoTexto("produto-codigo-barras", "Código de barras (opcional)", "text", false);
  codigoBarras.entrada.inputMode = "numeric";
  codigoBarras.entrada.maxLength = 14;

  const erro = criarMensagemErro();

  const botaoSalvar = document.createElement("button");
  botaoSalvar.type = "submit";
  botaoSalvar.className = "btn btn-primary btn-pequeno";
  botaoSalvar.textContent = "Criar e adicionar fotos";

  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(botaoSalvar);

  formulario.append(
    nome.container, descricao.container, preco.container, ncm.container,
    unidade.container, codigoBarras.container, erro, acoes
  );

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    botaoSalvar.disabled = true;
    aoSalvar({
      nome: nome.entrada.value,
      descricao: textoOuNulo(descricao.entrada.value),
      ncm: ncm.entrada.value.trim(),
      unidadeMedida: unidade.entrada.value,
      precoUnitario: Number(preco.entrada.value),
      codigoBarras: textoOuNulo(codigoBarras.entrada.value),
    })
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível criar o produto. Confira os dados."))
      .finally(() => {
        botaoSalvar.disabled = false;
      });
  });

  return formulario;
}
