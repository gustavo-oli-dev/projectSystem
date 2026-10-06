import {
  ativarProduto,
  atualizarProduto,
  buscarProduto,
  desativarProduto,
  type AlteracaoProduto,
  type Produto,
} from "../../api/produtosApi.js";
import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { criarSecaoEmbalagens } from "./embalagensProdutoView.js";
import { criarSecaoEstoque } from "./estoqueProdutoView.js";
import { criarSecaoFotos } from "./fotosProdutoView.js";
import { criarSecaoTributacao } from "./tributacaoProdutoView.js";
import { criarSecao } from "./secaoEdicao.js";

/**
 * Edição de um produto: dados e preço, fotos, estoque e situação de venda. Quem só tem permissão
 * de estoque vê apenas a parte de estoque.
 */
export async function montarEdicaoProduto(container: HTMLElement, produtoId: string): Promise<void> {
  const voltar = criarLinkVoltar();
  container.replaceChildren(voltar, elementoCarregando("Carregando produto..."));

  let produto: Produto;
  try {
    produto = await buscarProduto(produtoId);
  } catch {
    container.replaceChildren(voltar, cartaoEstado("Não foi possível carregar este produto.", "erro"));
    return;
  }

  const recarregar = (): Promise<void> => montarEdicaoProduto(container, produtoId);

  const titulo = document.createElement("h1");
  titulo.textContent = produto.nome;

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const coluna = document.createElement("div");
  coluna.className = "edicao-produto";

  if (possui("CATALOGO_GERENCIAR")) {
    cabecalho.append(criarBotaoSituacao(produto, recarregar));
    coluna.append(criarSecaoDados(produto, recarregar), criarSecaoEmbalagens(produto, recarregar),
      criarSecaoFotos(produto, recarregar));
  }
  if (possui("CATALOGO_GERENCIAR") || possui("FISCAL_GERENCIAR")) {
    coluna.append(criarSecaoTributacao(produto, recarregar));
  }
  coluna.append(criarSecaoEstoque(produto, recarregar));

  container.replaceChildren(voltar, cabecalho, coluna);
}

function criarSecaoDados(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const nome = criarCampoTexto("edicao-nome", "Nome", "text", true);
  nome.entrada.value = produto.nome;
  const descricao = criarCampoTexto("edicao-descricao", "Descrição", "text", false);
  descricao.entrada.value = produto.descricao ?? "";
  const preco = criarCampoTexto("edicao-preco", "Preço de venda (R$)", "number", true);
  preco.entrada.step = "0.01";
  preco.entrada.min = "0";
  preco.entrada.value = produto.precoUnitario.toFixed(2);
  const custo = criarCampoTexto("edicao-custo", "Custo (R$) — para calcular o lucro", "number", false);
  custo.entrada.step = "0.01";
  custo.entrada.min = "0";
  custo.entrada.value = produto.custoUnitario === null ? "" : produto.custoUnitario.toFixed(2);
  const estoqueMinimo = criarCampoTexto("edicao-estoque-minimo", "Estoque mínimo (avisa para repor)", "number", false);
  estoqueMinimo.entrada.step = "1";
  estoqueMinimo.entrada.min = "0";
  estoqueMinimo.entrada.placeholder = "Padrão: 5";
  estoqueMinimo.entrada.value = produto.estoqueMinimo === null ? "" : String(produto.estoqueMinimo);
  const codigoBarras = criarCampoTexto("edicao-codigo-barras", "Código de barras (opcional)", "text", false);
  codigoBarras.entrada.inputMode = "numeric";
  codigoBarras.entrada.maxLength = 14;
  codigoBarras.entrada.value = produto.codigoBarras ?? "";

  const fiscal = document.createElement("p");
  fiscal.className = "nota-campo";
  fiscal.textContent = `NCM ${produto.ncm} · unidade ${produto.unidadeMedida} (dados fiscais não mudam depois de cadastrados)`;

  const erro = criarMensagemErro();
  const botaoSalvar = document.createElement("button");
  botaoSalvar.type = "submit";
  botaoSalvar.className = "btn btn-primary btn-pequeno";
  botaoSalvar.textContent = "Salvar alterações";

  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(botaoSalvar);

  const formulario = document.createElement("form");
  formulario.className = "formulario-grade";
  formulario.append(
    nome.container, descricao.container, preco.container, custo.container, codigoBarras.container,
    estoqueMinimo.container, fiscal, erro, acoes
  );
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    botaoSalvar.disabled = true;
    const alteracao: AlteracaoProduto = {
      nome: nome.entrada.value,
      descricao: textoOuNulo(descricao.entrada.value),
      precoUnitario: Number(preco.entrada.value),
      codigoBarras: textoOuNulo(codigoBarras.entrada.value),
      custoUnitario: custo.entrada.value === "" ? null : Number(custo.entrada.value),
      estoqueMinimo: estoqueMinimo.entrada.value === "" ? null : Number(estoqueMinimo.entrada.value),
    };
    atualizarProduto(produto.id, alteracao)
      .then(recarregar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível salvar. Confira os dados.");
        botaoSalvar.disabled = false;
      });
  });

  return criarSecao("Dados e preço", formulario);
}

function criarBotaoSituacao(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = produto.ativo ? "btn btn-perigo btn-pequeno" : "btn btn-primary btn-pequeno";
  botao.textContent = produto.ativo ? "Tirar de venda" : "Colocar à venda";
  botao.addEventListener("click", () => {
    if (produto.ativo && !window.confirm(`Tirar "${produto.nome}" de venda? Ele some da loja e do bot.`)) {
      return;
    }
    botao.disabled = true;
    const acao = produto.ativo ? desativarProduto(produto.id) : ativarProduto(produto.id);
    acao.then(recarregar).catch(() => {
      botao.disabled = false;
    });
  });
  return botao;
}

function criarLinkVoltar(): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "link-voltar";
  botao.textContent = "← Gerenciar produtos";
  botao.addEventListener("click", () => navegarPara("gerenciar-produtos"));
  return botao;
}
