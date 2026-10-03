const CHAVE_PREFERENCIA = "barraLateralRecolhida";

/** Lembra se a pessoa prefere a barra lateral recolhida (só neste navegador). */
export function barraLateralRecolhida(): boolean {
  try {
    return localStorage.getItem(CHAVE_PREFERENCIA) === "sim";
  } catch {
    return false;
  }
}

export function lembrarBarraLateralRecolhida(recolhida: boolean): void {
  try {
    localStorage.setItem(CHAVE_PREFERENCIA, recolhida ? "sim" : "nao");
  } catch {
    // armazenamento indisponível: a preferência só não sobrevive ao recarregar
  }
}
