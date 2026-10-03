export function montarProgresso(
  container: HTMLElement,
  etapaAtual: number,
  totalEtapas: number,
  tituloEtapa: string
): void {
  const envoltorio = document.createElement("div");
  envoltorio.className = "progresso";

  const dots = document.createElement("div");
  dots.className = "progresso-dots";
  dots.setAttribute("role", "progressbar");
  dots.setAttribute("aria-valuenow", String(etapaAtual));
  dots.setAttribute("aria-valuemin", "1");
  dots.setAttribute("aria-valuemax", String(totalEtapas));
  dots.setAttribute("aria-label", tituloEtapa);

  for (let etapa = 1; etapa <= totalEtapas; etapa++) {
    const ponto = document.createElement("span");
    ponto.className = "progresso-dot";
    if (etapa === etapaAtual) {
      ponto.classList.add("progresso-dot--ativo");
    } else if (etapa < etapaAtual) {
      ponto.classList.add("progresso-dot--concluida");
    }
    dots.append(ponto);
  }

  const legenda = document.createElement("p");
  legenda.className = "progresso-legenda";
  legenda.textContent = `Etapa ${etapaAtual} de ${totalEtapas} · ${tituloEtapa}`;

  envoltorio.append(dots, legenda);
  container.replaceChildren(envoltorio);
}
