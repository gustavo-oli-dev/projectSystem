import type { Permissao, Sessao } from "../api/sessaoApi.js";

/**
 * Quem está logado e o que pode fazer. Serve só para esconder o que a pessoa não pode usar —
 * a proteção de verdade é o backend, que recusa (403) qualquer chamada sem permissão.
 */
let sessaoCarregada: Sessao | null = null;

export function definirSessao(sessao: Sessao | null): void {
  sessaoCarregada = sessao;
}

export function sessaoAtual(): Sessao | null {
  return sessaoCarregada;
}

export function possui(permissao: Permissao): boolean {
  return sessaoCarregada !== null && sessaoCarregada.permissoes.includes(permissao);
}

export function possuiAlguma(permissoes: readonly Permissao[]): boolean {
  return permissoes.some(possui);
}
