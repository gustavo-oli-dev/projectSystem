export interface Barra {
  rotulo: string;
  valor: number;
  /** Texto à direita do valor (ex.: "12 vendas"). */
  detalhe: string;
}

/**
 * Comparação de poucas categorias (canal, forma de pagamento): barras horizontais de uma cor,
 * valor sempre escrito ao lado — a barra nunca é a única forma de ler o número. A ordem é a de
 * quem chama (ex.: "A receber" fica por último mesmo sendo grande).
 */
export function criarBarrasHorizontais(barras: readonly Barra[], formatarValor: (valor: number) => string): HTMLElement {
  const maior = Math.max(0, ...barras.map((barra) => barra.valor));
  const lista = document.createElement("ul");
  lista.className = "barras";
  lista.append(...barras.map((barra) => {
    const rotulo = document.createElement("span");
    rotulo.className = "barras__rotulo";
    rotulo.textContent = barra.rotulo;

    const preenchimento = document.createElement("span");
    preenchimento.className = "barras__preenchimento";
    preenchimento.style.width = `${maior === 0 ? 0 : (barra.valor / maior) * 100}%`;
    const trilho = document.createElement("span");
    trilho.className = "barras__trilho";
    trilho.append(preenchimento);

    const valor = document.createElement("strong");
    valor.className = "barras__valor";
    valor.textContent = formatarValor(barra.valor);
    const detalhe = document.createElement("span");
    detalhe.className = "barras__detalhe";
    detalhe.textContent = barra.detalhe;

    const item = document.createElement("li");
    item.className = "barras__item";
    item.append(rotulo, trilho, valor, detalhe);
    return item;
  }));
  return lista;
}
