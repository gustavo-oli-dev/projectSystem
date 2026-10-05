import type { OpcaoSelecao } from "../camposFormulario.js";

/** Bandeiras que o operador escolhe ao digitar o comprovante da maquininha avulsa. */
export const BANDEIRAS: readonly OpcaoSelecao[] = [
  { valor: "VISA", rotulo: "Visa" },
  { valor: "MASTERCARD", rotulo: "Mastercard" },
  { valor: "ELO", rotulo: "Elo" },
  { valor: "AMERICAN_EXPRESS", rotulo: "American Express" },
  { valor: "HIPERCARD", rotulo: "Hipercard" },
  { valor: "OUTRA", rotulo: "Outra" },
];
