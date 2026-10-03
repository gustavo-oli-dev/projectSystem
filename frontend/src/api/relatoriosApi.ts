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
