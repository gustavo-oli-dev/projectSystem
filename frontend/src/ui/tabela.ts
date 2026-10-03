import { criarIcone } from "./icones.js";

/** Abaixo disso a busca não aparece: com poucas linhas ela só ocupa espaço. */
const MINIMO_LINHAS_PARA_BUSCA = 2;

/**
 * Tabela padrão de listagem: barra com contagem + busca, cabeçalho em <thead> e linhas.
 * Compartilhada por todas as telas de lista para manter densidade e aparência iguais. A busca
 * filtra as linhas desta tabela pelo texto (fica junto da lista que ela filtra).
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

  const nadaEncontrado = document.createElement("p");
  nadaEncontrado.className = "barra-tabela__nada";
  nadaEncontrado.textContent = "Nada encontrado para essa busca.";
  nadaEncontrado.hidden = true;

  if (linhas.length >= MINIMO_LINHAS_PARA_BUSCA) {
    barra.append(criarBusca(linhas, (visiveis) => {
      contagem.textContent = visiveis === linhas.length
        ? `${linhas.length} ${rotuloContagem}`
        : `${visiveis} de ${linhas.length} ${rotuloContagem}`;
      nadaEncontrado.hidden = visiveis > 0;
    }));
  }

  const rolagem = document.createElement("div");
  rolagem.className = "bloco-tabela__rolagem";
  rolagem.append(tabela);

  const bloco = document.createElement("div");
  bloco.className = "bloco-tabela";
  bloco.append(barra, rolagem, nadaEncontrado);
  return bloco;
}

function criarBusca(linhas: HTMLTableRowElement[], aoFiltrar: (visiveis: number) => void): HTMLElement {
  const campo = document.createElement("input");
  campo.type = "search";
  campo.placeholder = "Buscar nesta lista";
  campo.setAttribute("aria-label", "Buscar nesta lista");

  campo.addEventListener("input", () => {
    const termo = campo.value.trim().toLowerCase();
    let visiveis = 0;
    for (const linha of linhas) {
      const corresponde = termo === "" || (linha.textContent ?? "").toLowerCase().includes(termo);
      linha.hidden = !corresponde;
      visiveis += corresponde ? 1 : 0;
    }
    aoFiltrar(visiveis);
  });

  const caixa = document.createElement("label");
  caixa.className = "barra-tabela__busca";
  caixa.append(criarIcone("busca"), campo);
  return caixa;
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
