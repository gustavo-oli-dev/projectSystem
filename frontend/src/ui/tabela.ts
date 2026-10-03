/**
 * Tabela padrão de listagem: barra com contagem de registros + cabeçalho em <thead> + linhas.
 * Compartilhada por todas as telas de lista para manter densidade e aparência iguais.
 */
export function criarTabela(
  colunas: readonly string[],
  linhas: HTMLTableRowElement[],
  rotuloContagem: string
): HTMLElement {
  const contagem = document.createElement("span");
  contagem.className = "barra-tabela__contagem";
  contagem.textContent = `${linhas.length} ${rotuloContagem}`;

  const barra = document.createElement("div");
  barra.className = "barra-tabela";
  barra.append(contagem);

  const linhaCabecalho = document.createElement("tr");
  for (const coluna of colunas) {
    const celulaCabecalho = document.createElement("th");
    celulaCabecalho.textContent = coluna;
    linhaCabecalho.append(celulaCabecalho);
  }

  const cabecalho = document.createElement("thead");
  cabecalho.append(linhaCabecalho);

  const corpo = document.createElement("tbody");
  corpo.append(...linhas);

  const tabela = document.createElement("table");
  tabela.className = "tabela";
  tabela.append(cabecalho, corpo);

  const bloco = document.createElement("div");
  bloco.className = "bloco-tabela";
  bloco.append(barra, tabela);
  return bloco;
}

export function criarLinha(...celulas: HTMLTableCellElement[]): HTMLTableRowElement {
  const linha = document.createElement("tr");
  linha.append(...celulas);
  return linha;
}

export function celula(texto: string): HTMLTableCellElement {
  const elemento = document.createElement("td");
  elemento.textContent = texto;
  return elemento;
}

export function celulaSelo(texto: string, modificador: string): HTMLTableCellElement {
  const selo = document.createElement("span");
  selo.className = `selo selo--${modificador}`;
  selo.textContent = texto;

  const elemento = document.createElement("td");
  elemento.append(selo);
  return elemento;
}

export function celulaComConteudo(conteudo: HTMLElement): HTMLTableCellElement {
  const elemento = document.createElement("td");
  elemento.append(conteudo);
  return elemento;
}

export function formatarDataCurta(iso: string): string {
  return new Date(iso).toLocaleDateString("pt-BR");
}
