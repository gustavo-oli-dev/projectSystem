import { httpClient } from "./httpClient.js";

// ---------- Contatos ----------

export type TipoContato = "FORNECEDOR" | "TRANSPORTADORA" | "OUTRO";

export interface Contato {
  id: string;
  tipo: TipoContato;
  nome: string;
  documento: string | null;
  telefone: string | null;
  email: string | null;
  observacao: string | null;
  ativo: boolean;
}

export interface DadosContato {
  tipo: TipoContato;
  nome: string;
  documento: string | null;
  telefone: string | null;
  email: string | null;
  observacao: string | null;
}

export function listarContatos(): Promise<Contato[]> {
  return httpClient.get<Contato[]>("/contatos");
}

export function cadastrarContato(dados: DadosContato): Promise<Contato> {
  return httpClient.post<Contato>("/contatos", dados);
}

export function alterarContato(id: string, dados: DadosContato): Promise<Contato> {
  return httpClient.put<Contato>(`/contatos/${encodeURIComponent(id)}`, dados);
}

export function definirContatoAtivo(id: string, ativo: boolean): Promise<Contato> {
  return httpClient.post<Contato>(`/contatos/${encodeURIComponent(id)}/${ativo ? "ativar" : "desativar"}`, {});
}

// ---------- Contas a pagar ----------

export type StatusContaAPagar = "ABERTA" | "PAGA" | "CANCELADA";

export interface ContaAPagar {
  id: string;
  contatoId: string | null;
  contatoNome: string | null;
  descricao: string;
  valor: number;
  vencimento: string;
  status: StatusContaAPagar;
  /** Aberta e com o vencimento já passado (pelo dia da loja). */
  vencida: boolean;
  /** Parcela de uma nota de fornecedor (lançada pela entrada por XML). */
  daNota: boolean;
  pagaEm: string | null;
  pagaPor: string | null;
}

export function listarContasAPagar(): Promise<ContaAPagar[]> {
  return httpClient.get<ContaAPagar[]>("/contas-a-pagar");
}

export function lancarContaAPagar(conta: { contatoId: string | null; descricao: string; valor: number; vencimento: string }): Promise<ContaAPagar> {
  return httpClient.post<ContaAPagar>("/contas-a-pagar", conta);
}

export function pagarConta(id: string): Promise<ContaAPagar> {
  return httpClient.post<ContaAPagar>(`/contas-a-pagar/${encodeURIComponent(id)}/pagar`, {});
}

export function cancelarConta(id: string): Promise<ContaAPagar> {
  return httpClient.post<ContaAPagar>(`/contas-a-pagar/${encodeURIComponent(id)}/cancelar`, {});
}

// ---------- Entrada por nota (XML do fornecedor) ----------

export interface ItemDaNota {
  ordem: number;
  codigo: string;
  codigoBarras: string | null;
  descricao: string;
  unidade: string;
  quantidade: number;
  /** false = quantidade fracionada (ex.: 2,5 kg): não entra pela nota. */
  quantidadeInteira: boolean;
  custoUnitario: number;
  produtoSugeridoId: string | null;
  produtoSugeridoNome: string | null;
}

export interface PreVisualizacaoNota {
  chaveAcesso: string;
  numero: string;
  serie: string;
  emitidaEm: string;
  valorTotal: number;
  fornecedorDocumento: string;
  fornecedorNome: string;
  /** null = o fornecedor será cadastrado ao confirmar. */
  fornecedorCadastradoId: string | null;
  /** Preenchido = a nota já entrou no estoque. */
  jaLancadaEm: string | null;
  jaLancadaPor: string | null;
  itens: ItemDaNota[];
  parcelas: Array<{ numero: string; vencimento: string; valor: number }>;
}

export interface NotaEntrada {
  id: string;
  numero: string;
  chaveAcesso: string;
  fornecedorNome: string;
  valorTotal: number;
  itens: number;
  unidades: number;
  registradaPor: string;
  registradaEm: string;
}

/** Só lê o XML e mostra o que veio — nada é gravado. */
export function preVisualizarNota(arquivo: File): Promise<PreVisualizacaoNota> {
  const formulario = new FormData();
  formulario.append("arquivo", arquivo);
  return httpClient.post<PreVisualizacaoNota>("/compras/notas/pre-visualizar", formulario);
}

/** Dá entrada: o servidor lê o XML de novo; da tela só vale "item X é o produto Y" e se o custo muda. */
export function confirmarNota(
  arquivo: File, itens: ReadonlyArray<{ ordem: number; produtoId: string }>, atualizarCusto: boolean
): Promise<NotaEntrada> {
  const formulario = new FormData();
  formulario.append("arquivo", arquivo);
  formulario.append("dados", new Blob([JSON.stringify({ itens, atualizarCusto })], { type: "application/json" }));
  return httpClient.post<NotaEntrada>("/compras/notas", formulario);
}

export function listarNotasLancadas(): Promise<NotaEntrada[]> {
  return httpClient.get<NotaEntrada[]>("/compras/notas");
}
