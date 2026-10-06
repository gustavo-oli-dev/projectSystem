import { httpClient } from "./httpClient.js";

export interface ResumoVendas {
  faturamento: number;
  vendas: number;
  ticketMedio: number;
  unidades: number;
  lucroBruto: number;
  /** null = nenhum item vendido com custo informado. */
  margem: number | null;
  /** 0 a 1: quanto do faturamento tem custo informado. */
  coberturaCusto: number;
}

export interface PontoSerie {
  inicio: string;
  faturamento: number;
  vendas: number;
  lucroBruto: number;
}

export interface Recorte {
  chave: string;
  vendas: number;
  valor: number;
}

export interface ItemMaisVendido {
  id: string;
  descricao: string;
  tipo: "PRODUTO" | "SERVICO";
  unidades: number;
  faturamento: number;
  /** Só das vendas com custo informado; null se nenhuma tinha custo. */
  lucroBruto: number | null;
  /** false = parte das vendas sem custo: o lucro mostrado é parcial. */
  custoCompleto: boolean;
}

export type Granularidade = "DIA" | "SEMANA" | "MES";

export interface RelatorioVendas {
  inicio: string;
  fim: string;
  granularidade: Granularidade;
  resumo: ResumoVendas;
  periodoAnterior: ResumoVendas;
  desfeitas: Recorte;
  recebido: number;
  aReceber: number;
  serie: PontoSerie[];
  porCanal: Recorte[];
  porFormaPagamento: Recorte[];
  porHora: Recorte[];
  porDiaDaSemana: Recorte[];
  maisVendidos: ItemMaisVendido[];
}

export interface Periodo {
  inicio: string;
  fim: string;
}

function consulta(periodo: Periodo): string {
  return `inicio=${encodeURIComponent(periodo.inicio)}&fim=${encodeURIComponent(periodo.fim)}`;
}

export function gerarRelatorioVendas(periodo: Periodo): Promise<RelatorioVendas> {
  return httpClient.get<RelatorioVendas>(`/relatorios/vendas?${consulta(periodo)}`);
}

export type ExportacaoVendas = "periodos" | "mais-vendidos";

export function baixarCsvVendas(periodo: Periodo, tipo: ExportacaoVendas): Promise<Blob> {
  return httpClient.arquivo(`/relatorios/vendas/${tipo}.csv?${consulta(periodo)}`);
}

export type ResultadoFechamento = "ABERTO" | "BATEU" | "SOBROU" | "FALTOU";
export type FormaVendaRelatorio = "DINHEIRO" | "CARTAO_CREDITO" | "CARTAO_DEBITO" | "PIX" | "PIX_QR";

export interface RelatorioCaixa {
  totais: {
    caixas: number;
    fechados: number;
    /** Total que faltou, positivo. */
    faltas: number;
    sobras: number;
    /** Sobras − faltas. */
    saldo: number;
    sangrias: number;
    reposicoes: number;
    vendasEmDinheiro: number;
  };
  porResultado: Array<{ resultado: ResultadoFechamento; caixas: number }>;
  porOperador: Array<{
    operador: string;
    operadorNome: string;
    caixas: number;
    fechados: number;
    comFalta: number;
    faltas: number;
    sobras: number;
    saldo: number;
    sangrias: number;
    reposicoes: number;
  }>;
  /** Resumo do dia de cada caixa físico: vendido em cada forma e quem operou. */
  porCaixaEDia: Array<{
    dia: string;
    pontoNome: string;
    operadores: string[];
    vendasPorForma: Partial<Record<FormaVendaRelatorio, number>>;
    totalVendido: number;
  }>;
  caixas: Array<{
    id: string;
    pontoNome: string;
    operadorNome: string;
    abertaPorNome: string;
    fechadaPorNome: string | null;
    abertaEm: string;
    fechadaEm: string | null;
    fundoInicial: number;
    reposicoes: number;
    sangrias: number;
    vendasEmDinheiro: number | null;
    valorEsperado: number | null;
    valorContado: number | null;
    diferenca: number | null;
    /** Relatório da maquininha − sistema nas formas conferidas; null = não conferido. */
    diferencaMaquininha: number | null;
    resultado: ResultadoFechamento;
    vendasPorForma: Partial<Record<FormaVendaRelatorio, number>>;
    totalVendido: number;
  }>;
}

export function gerarRelatorioCaixa(periodo: Periodo): Promise<RelatorioCaixa> {
  return httpClient.get<RelatorioCaixa>(`/relatorios/caixa?${consulta(periodo)}`);
}

export function baixarCsvCaixa(periodo: Periodo): Promise<Blob> {
  return httpClient.arquivo(`/relatorios/caixa/fechamentos.csv?${consulta(periodo)}`);
}

/** Conferência do dinheiro de um dia: o contado nas gavetas × o vendido em dinheiro no sistema. */
export interface DinheiroDoDia {
  dia: string;
  vendidoEmDinheiro: number;
  entrouNasGavetas: number;
  /** Entrou − vendido: zero certo, negativo devendo, positivo sobrando. */
  diferenca: number;
  /** null = nenhum caixa fechado no dia ainda. */
  resultado: ResultadoFechamento | null;
  caixasAbertos: number;
  caixas: Array<{
    pontoNome: string;
    operadorNome: string;
    abertaEm: string;
    fechadaEm: string | null;
    valorInicial: number;
    reposicoes: number;
    sangrias: number;
    contado: number | null;
    entrouNaGaveta: number | null;
    vendidoEmDinheiro: number | null;
    diferenca: number | null;
    resultado: ResultadoFechamento;
  }>;
  produtosEmDinheiro: Array<{ descricao: string; quantidade: number; valor: number }>;
}

export function conferirDinheiroDoDia(dia: string): Promise<DinheiroDoDia> {
  return httpClient.get<DinheiroDoDia>(`/relatorios/caixa/dia?data=${encodeURIComponent(dia)}`);
}

/** Perdas e quebras no período: perdas registradas + faltas de inventário ("INVENTARIO"). */
export interface RelatorioPerdas {
  unidades: number;
  valor: number;
  /** Unidades perdidas de produtos sem custo cadastrado (contam, mas sem valor). */
  unidadesSemCusto: number;
  porMotivo: Array<{ chave: string; unidades: number; valor: number }>;
  porProduto: Array<{ chave: string; unidades: number; valor: number }>;
}

export function gerarRelatorioPerdas(periodo: Periodo): Promise<RelatorioPerdas> {
  return httpClient.get<RelatorioPerdas>(`/relatorios/perdas?${consulta(periodo)}`);
}

/** Itens tirados da venda no caixa, com quem autorizou (trilha para conferência). */
export interface ItemCancelado {
  id: string;
  descricao: string;
  quantidade: number;
  valor: number;
  operadorNome: string;
  autorizadoPorNome: string;
  canceladoEm: string;
}

export function listarItensCancelados(periodo: Periodo): Promise<ItemCancelado[]> {
  return httpClient.get<ItemCancelado[]>(`/relatorios/itens-cancelados?${consulta(periodo)}`);
}
