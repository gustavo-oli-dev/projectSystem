const INTEIRO = new Intl.NumberFormat("pt-BR");
const PERCENTUAL = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });
const MOEDA_COMPACTA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  notation: "compact",
  minimumFractionDigits: 0,
  maximumFractionDigits: 1,
});
const MOEDA_SEM_CENTAVOS = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});
const MIL = 1000;

export function formatarInteiro(valor: number): string {
  return INTEIRO.format(valor);
}

/** 0,4 → "40%". */
export function formatarPercentual(fracao: number): string {
  return PERCENTUAL.format(fracao);
}

/** Para eixos de gráfico: R$ 1,2 mil / R$ 3,4 mi. */
export function formatarMoedaCompacta(valor: number): string {
  return valor >= MIL ? MOEDA_COMPACTA.format(valor) : MOEDA_SEM_CENTAVOS.format(valor);
}
