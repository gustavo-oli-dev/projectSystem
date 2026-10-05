import { baixarListaDeCompra, listarReposicao, type SugestaoReposicao } from "../../api/produtosApi.js";
import { navegarPara } from "../../router.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarInteiro } from "../formatarNumero.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";

/**
 * Reposição (D33): produtos que chegaram no estoque mínimo e quanto comprar — o suficiente para
 * 30 dias de venda no ritmo dos últimos 30, mantendo o mínimo. Vira lista de compra em CSV.
 */
export async function montarReposicao(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Reposição de estoque";
  const exportar = document.createElement("button");
  exportar.type = "button";
  exportar.className = "btn btn-ghost";
  exportar.textContent = "Baixar lista de compra (CSV)";
  exportar.addEventListener("click", () => {
    exportar.disabled = true;
    baixarListaDeCompra()
      .then((arquivo) => salvar(arquivo, "lista-de-compra.csv"))
      .catch(() => {
        exportar.textContent = "Falhou — tentar de novo";
      })
      .finally(() => {
        exportar.disabled = false;
      });
  });
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, exportar);

  const area = document.createElement("div");
  container.replaceChildren(cabecalho, area);
  area.append(elementoCarregando("Calculando o que comprar..."));
  try {
    const sugestoes = await listarReposicao();
    exportar.hidden = sugestoes.length === 0;
    area.replaceChildren(sugestoes.length === 0
      ? cartaoEstado("Nenhum produto no estoque mínimo. Tudo abastecido.")
      : montarLista(sugestoes));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível calcular a reposição.", "erro"));
  }
}

function montarLista(sugestoes: readonly SugestaoReposicao[]): HTMLElement {
  const explicacao = document.createElement("p");
  explicacao.className = "caixa-painel__instrucao";
  explicacao.textContent = "Comprar = vendido nos últimos 30 dias + estoque mínimo − estoque atual. Produto sem mínimo definido usa o padrão de 5 unidades — defina o mínimo de cada um no cadastro do produto.";
  const tabela = criarTabela(
    ["Produto", "Código de barras", "Estoque", "Mínimo", "Vendidos em 30 dias", "Comprar", ""],
    sugestoes.map((sugestao) => criarLinha(
      celula(sugestao.nome),
      celula(sugestao.codigoBarras ?? "—"),
      sugestao.estoque <= 0
        ? celulaSelo(`Esgotado (${sugestao.estoque})`, "esgotado")
        : celulaSelo(`${formatarInteiro(sugestao.estoque)} ${sugestao.unidadeMedida}`, "estoque_baixo"),
      celula(sugestao.minimoDefinido ? formatarInteiro(sugestao.estoqueMinimo) : `${formatarInteiro(sugestao.estoqueMinimo)} (padrão)`),
      celula(formatarInteiro(sugestao.vendidosEm30Dias)),
      celulaQuantidade(sugestao),
      celulaComConteudo(criarLinkProduto(sugestao.produtoId))
    )),
    "produto(s) para repor"
  );
  const painel = document.createElement("div");
  painel.className = "caixa-painel";
  painel.append(explicacao, tabela);
  return painel;
}

function celulaQuantidade(sugestao: SugestaoReposicao): HTMLTableCellElement {
  const valor = document.createElement("strong");
  valor.textContent = `${formatarInteiro(sugestao.quantidadeSugerida)} ${sugestao.unidadeMedida}`;
  const elemento = document.createElement("td");
  elemento.append(valor);
  return elemento;
}

function criarLinkProduto(produtoId: string): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-ghost btn-pequeno";
  botao.textContent = "Abrir produto";
  botao.addEventListener("click", () => navegarPara("gerenciar-produtos", produtoId));
  return botao;
}

function salvar(arquivo: Blob, nome: string): void {
  const endereco = URL.createObjectURL(arquivo);
  const link = document.createElement("a");
  link.href = endereco;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(endereco);
}
