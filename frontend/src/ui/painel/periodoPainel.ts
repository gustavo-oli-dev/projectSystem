import type { Periodo } from "../../api/relatoriosApi.js";

export type AtalhoPeriodo = "HOJE" | "SETE_DIAS" | "TRINTA_DIAS" | "MES" | "NOVENTA_DIAS" | "ANO";

export const ATALHOS: ReadonlyArray<{ atalho: AtalhoPeriodo; rotulo: string }> = [
  { atalho: "HOJE", rotulo: "Hoje" },
  { atalho: "SETE_DIAS", rotulo: "7 dias" },
  { atalho: "TRINTA_DIAS", rotulo: "30 dias" },
  { atalho: "MES", rotulo: "Este mês" },
  { atalho: "NOVENTA_DIAS", rotulo: "3 meses" },
  { atalho: "ANO", rotulo: "Este ano" },
];

const DIAS_ATRAS: Partial<Record<AtalhoPeriodo, number>> = {
  HOJE: 0,
  SETE_DIAS: 6,
  TRINTA_DIAS: 29,
  NOVENTA_DIAS: 89,
};

/** Período de um atalho, sempre terminando hoje (no relógio de quem está usando — a loja). */
export function periodoDoAtalho(atalho: AtalhoPeriodo, hoje: Date = new Date()): Periodo {
  const fim = paraIso(hoje);
  if (atalho === "MES") {
    return { inicio: paraIso(new Date(hoje.getFullYear(), hoje.getMonth(), 1)), fim };
  }
  if (atalho === "ANO") {
    return { inicio: paraIso(new Date(hoje.getFullYear(), 0, 1)), fim };
  }
  const inicio = new Date(hoje);
  inicio.setDate(hoje.getDate() - (DIAS_ATRAS[atalho] ?? 0));
  return { inicio: paraIso(inicio), fim };
}

export function paraIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${data.getFullYear()}-${mes}-${dia}`;
}

/** "2026-09-12" → data local (sem o deslocamento de fuso que new Date("2026-09-12") causaria). */
export function deIso(iso: string): Date {
  const [ano = 0, mes = 1, dia = 1] = iso.split("-").map(Number);
  return new Date(ano, mes - 1, dia);
}
