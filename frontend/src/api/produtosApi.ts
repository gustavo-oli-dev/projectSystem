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
  fotos: FotoProduto[];
}

export interface NovoProduto {
  nome: string;
  descricao: string | null;
  ncm: string;
  unidadeMedida: string;
  precoUnitario: number;
  codigoBarras: string | null;
}

export interface AlteracaoProduto {
  nome: string;
  descricao: string | null;
  precoUnitario: number;
  codigoBarras: string | null;
}

export type TipoMovimentacao = "ENTRADA" | "VENDA" | "DEVOLUCAO";

export interface MovimentacaoEstoque {
  id: string;
  tipo: TipoMovimentacao;
  quantidade: number;
  saldoApos: number;
  pedidoId: string | null;
  responsavel: string;
  criadaEm: string;
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
