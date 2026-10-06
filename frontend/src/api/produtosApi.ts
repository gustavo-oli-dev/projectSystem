import { httpClient } from "./httpClient.js";

export interface FotoProduto {
  id: string;
  url: string;
}

export interface Produto {
  id: string;
  nome: string;
  descricao: string | null;
  ncm: string;
  unidadeMedida: string;
  precoUnitario: number;
  ativo: boolean;
  codigoBarras: string | null;
  quantidadeEmEstoque: number;
  /** null = sem mínimo definido (vale o aviso padrão de 5). */
  estoqueMinimo: number | null;
  fotos: FotoProduto[];
  /** Só vem para quem gerencia o catálogo ou vê o faturamento; null = não informado ou sem permissão. */
  custoUnitario: number | null;
  /** Formas de vender em quantidade (ex.: fardo com 12) — D41. */
  embalagens: Embalagem[];
}

/** Embalagem do produto: preço próprio; o estoque baixa "unidades" por embalagem vendida. */
export interface Embalagem {
  id: string;
  nome: string;
  codigoBarras: string | null;
  unidades: number;
  preco: number;
}

export interface NovaEmbalagem {
  nome: string;
  codigoBarras: string | null;
  unidades: number;
  preco: number;
}

export function adicionarEmbalagem(produtoId: string, embalagem: NovaEmbalagem): Promise<Produto> {
  return httpClient.post<Produto>(`/produtos/${produtoId}/embalagens`, embalagem);
}

export function removerEmbalagem(produtoId: string, embalagemId: string): Promise<Produto> {
  return httpClient.delete<Produto>(`/produtos/${produtoId}/embalagens/${embalagemId}`);
}

export interface NovoProduto {
  nome: string;
  descricao: string | null;
  ncm: string;
  unidadeMedida: string;
  precoUnitario: number;
  codigoBarras: string | null;
  custoUnitario: number | null;
}

export interface AlteracaoProduto {
  nome: string;
  descricao: string | null;
  precoUnitario: number;
  codigoBarras: string | null;
  custoUnitario: number | null;
  /** Sempre enviado: o servidor grava o que vier (null = sem mínimo, vale o aviso padrão). */
  estoqueMinimo: number | null;
}

export type TipoMovimentacao = "ENTRADA" | "VENDA" | "DEVOLUCAO" | "PERDA" | "INVENTARIO_SOBRA" | "INVENTARIO_FALTA";
/** Por que o produto saiu do estoque sem ser vendido. */
export type MotivoPerda = "VENCIDO" | "AVARIADO" | "FURTO" | "TROCA" | "USO_INTERNO" | "OUTRO";

export interface MovimentacaoEstoque {
  id: string;
  tipo: TipoMovimentacao;
  quantidade: number;
  saldoApos: number;
  pedidoId: string | null;
  responsavel: string;
  criadaEm: string;
  motivo: MotivoPerda | null;
  observacao: string | null;
}

export function listarProdutos(): Promise<Produto[]> {
  return httpClient.get<Produto[]>("/produtos");
}

export function buscarProduto(id: string): Promise<Produto> {
  return httpClient.get<Produto>(`/produtos/${id}`);
}

export function cadastrarProduto(produto: NovoProduto): Promise<Produto> {
  return httpClient.post<Produto>("/produtos", produto);
}

export function atualizarProduto(id: string, alteracao: AlteracaoProduto): Promise<Produto> {
  return httpClient.put<Produto>(`/produtos/${id}`, alteracao);
}

export function desativarProduto(id: string): Promise<void> {
  return httpClient.delete<void>(`/produtos/${id}`);
}

export function ativarProduto(id: string): Promise<Produto> {
  return httpClient.post<Produto>(`/produtos/${id}/ativar`, {});
}

export function darEntradaNoEstoque(id: string, quantidade: number): Promise<Produto> {
  return httpClient.post<Produto>(`/produtos/${id}/estoque/entradas`, { quantidade });
}

/** Saída sem venda (vencido, avariado, furto, uso interno): nunca deixa o estoque negativo. */
export function registrarPerda(id: string, quantidade: number, motivo: MotivoPerda, observacao: string | null): Promise<Produto> {
  return httpClient.post<Produto>(`/produtos/${id}/estoque/perdas`, { quantidade, motivo, observacao });
}

export interface AjusteInventario {
  produtoId: string;
  nome: string;
  noSistema: number;
  contado: number;
  /** Positivo = sobrou; negativo = faltou. */
  diferenca: number;
}

/** Acerta o estoque dos produtos contados ao que tem na prateleira (tudo ou nada). */
export function aplicarInventario(contagens: ReadonlyArray<{ produtoId: string; quantidadeContada: number }>): Promise<AjusteInventario[]> {
  return httpClient.post<AjusteInventario[]>("/estoque/inventario", { contagens });
}

export function listarMovimentacoes(id: string): Promise<MovimentacaoEstoque[]> {
  return httpClient.get<MovimentacaoEstoque[]>(`/produtos/${id}/estoque/movimentacoes`);
}

export function enviarFoto(id: string, arquivo: File): Promise<FotoProduto> {
  const formulario = new FormData();
  formulario.append("arquivo", arquivo);
  return httpClient.post<FotoProduto>(`/produtos/${id}/fotos`, formulario);
}

export function removerFoto(produtoId: string, fotoId: string): Promise<void> {
  return httpClient.delete<void>(`/produtos/${produtoId}/fotos/${fotoId}`);
}

/** Leitor de código de barras (caixa e entrada de estoque). */
export function buscarProdutoPorCodigoBarras(codigo: string): Promise<Produto> {
  return httpClient.get<Produto>(`/produtos/codigo-barras/${encodeURIComponent(codigo)}`);
}

/** Produto que chegou no estoque mínimo e quanto comprar (30 dias de venda + mínimo − estoque). */
export interface SugestaoReposicao {
  produtoId: string;
  nome: string;
  codigoBarras: string | null;
  unidadeMedida: string;
  estoque: number;
  estoqueMinimo: number;
  minimoDefinido: boolean;
  vendidosEm30Dias: number;
  quantidadeSugerida: number;
}

export function listarReposicao(): Promise<SugestaoReposicao[]> {
  return httpClient.get<SugestaoReposicao[]>("/estoque/reposicao");
}

export function baixarListaDeCompra(): Promise<Blob> {
  return httpClient.arquivo("/estoque/reposicao/lista-de-compra.csv");
}

/** Preço em lote (D39): +5 = 5% mais caro; PRECO_UNICO = todos passam a custar o valor. */
export type ModoReajuste = "PERCENTUAL" | "PRECO_UNICO";

export interface PrecoAlterado {
  produtoId: string;
  produto: string;
  precoAnterior: number;
  precoNovo: number;
}

/** Devolve só os produtos cujo preço mudou de verdade. */
export function reajustarPrecos(produtoIds: readonly string[], modo: ModoReajuste, valor: number): Promise<PrecoAlterado[]> {
  return httpClient.post<PrecoAlterado[]>("/produtos/reajuste-precos", { produtoIds, modo, valor });
}

/** Ids dos produtos que mudaram de preço nos últimos dias (para reimprimir as etiquetas). */
export function listarPrecosAlterados(dias: number): Promise<string[]> {
  return httpClient.get<string[]>(`/produtos/precos-alterados?dias=${dias}`);
}
