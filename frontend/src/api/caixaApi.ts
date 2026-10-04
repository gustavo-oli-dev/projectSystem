import { httpClient } from "./httpClient.js";

/** Espelha o enum Cedula do backend, da menor para a maior. */
export type Cedula =
  | "MOEDA_5_CENTAVOS"
  | "MOEDA_10_CENTAVOS"
  | "MOEDA_25_CENTAVOS"
  | "MOEDA_50_CENTAVOS"
  | "MOEDA_1_REAL"
  | "NOTA_2"
  | "NOTA_5"
  | "NOTA_10"
  | "NOTA_20"
  | "NOTA_50"
  | "NOTA_100"
  | "NOTA_200";

/** Quantidade por cédula; cédula ausente = zero. */
export type Contagem = Partial<Record<Cedula, number>>;

export interface CedulaContada {
  cedula: Cedula;
  valorUnitario: number;
  quantidade: number;
  subtotal: number;
}

export interface MovimentoCaixa {
  id: string;
  tipo: "SUPRIMENTO" | "SANGRIA";
  valor: number;
  motivo: string;
  cedulas: CedulaContada[];
  registradoPor: string;
  registradoEm: string;
}

/** Um caixa aberto — sem o valor esperado (fechamento cego). */
export interface CaixaAberto {
  id: string;
  operador: string;
  operadorNome: string;
  abertaPorNome: string;
  abertaEm: string;
  fundoInicial: number;
  cedulasAbertura: CedulaContada[];
  totalSuprimentos: number;
  totalSangrias: number;
  movimentos: MovimentoCaixa[];
}

export type FormaVendaCaixa = "DINHEIRO" | "CARTAO_CREDITO" | "CARTAO_DEBITO" | "PIX" | "PIX_QR";

export interface ConferenciaCaixa {
  id: string;
  operador: string;
  operadorNome: string;
  abertaPorNome: string;
  fechadaPorNome: string | null;
  status: "ABERTA" | "FECHADA";
  abertaEm: string;
  fechadaEm: string | null;
  fundoInicial: number;
  cedulasAbertura: CedulaContada[];
  movimentos: MovimentoCaixa[];
  totalSuprimentos: number;
  totalSangrias: number;
  vendasPorForma: Array<{ forma: FormaVendaCaixa; vendas: number; valor: number }>;
  vendasEmDinheiro: number;
  valorEsperado: number;
  valorContado: number | null;
  cedulasFechamento: CedulaContada[];
  /** Sobra (positivo) ou falta (negativo). */
  diferenca: number | null;
  dinheiroQueEntrou: number | null;
  observacao: string | null;
}

export interface ResumoCaixa {
  id: string;
  operador: string;
  operadorNome: string;
  status: "ABERTA" | "FECHADA";
  abertaEm: string;
  fechadaEm: string | null;
  fundoInicial: number;
  vendasEmDinheiro: number;
  valorEsperado: number;
  valorContado: number | null;
  diferenca: number | null;
}

/** null = o caixa deste operador ainda não foi aberto (a API responde 204). */
export async function buscarCaixaAberto(): Promise<CaixaAberto | null> {
  return (await httpClient.get<CaixaAberto | undefined>("/caixa/atual")) ?? null;
}

export interface OperadorCaixa {
  id: string;
  nome: string;
  email: string;
  caixaAberto: boolean;
}

export function listarCaixasAbertos(): Promise<CaixaAberto[]> {
  return httpClient.get<CaixaAberto[]>("/caixa/abertos");
}

export function listarOperadoresDeCaixa(): Promise<OperadorCaixa[]> {
  return httpClient.get<OperadorCaixa[]>("/caixa/operadores");
}

export function abrirCaixa(operadorId: string, cedulas: Contagem): Promise<CaixaAberto> {
  return httpClient.post<CaixaAberto>("/caixa/abrir", { operadorId, cedulas });
}

export function registrarSuprimento(caixaId: string, cedulas: Contagem, motivo: string): Promise<CaixaAberto> {
  return httpClient.post<CaixaAberto>(`/caixa/${encodeURIComponent(caixaId)}/suprimento`, { cedulas, motivo });
}

export function registrarSangria(caixaId: string, valor: number, motivo: string): Promise<CaixaAberto> {
  return httpClient.post<CaixaAberto>(`/caixa/${encodeURIComponent(caixaId)}/sangria`, { valor, motivo });
}

export function fecharCaixa(caixaId: string, cedulas: Contagem, observacao: string | null): Promise<ConferenciaCaixa> {
  return httpClient.post<ConferenciaCaixa>(`/caixa/${encodeURIComponent(caixaId)}/fechar`, { cedulas, observacao });
}

export function buscarFundoPadrao(): Promise<CedulaContada[]> {
  return httpClient.get<CedulaContada[]>("/caixa/fundo-padrao");
}

export function definirFundoPadrao(cedulas: Contagem): Promise<CedulaContada[]> {
  return httpClient.put<CedulaContada[]>("/caixa/fundo-padrao", { cedulas });
}

export function listarCaixasParaConferencia(): Promise<ResumoCaixa[]> {
  return httpClient.get<ResumoCaixa[]>("/caixa/conferencia");
}

export function detalharCaixa(id: string): Promise<ConferenciaCaixa> {
  return httpClient.get<ConferenciaCaixa>(`/caixa/conferencia/${encodeURIComponent(id)}`);
}
