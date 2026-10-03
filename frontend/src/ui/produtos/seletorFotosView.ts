/** Mesmos limites do backend: conferidos antes para avisar na hora, sem enviar à toa. */
export const FOTOS_POR_PRODUTO = 8;
const TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;
const TIPOS_ACEITOS = ["image/jpeg", "image/png", "image/webp"];

export interface SeletorFotos {
  elemento: HTMLElement;
  arquivos: () => File[];
}

/**
 * Área para escolher fotos antes de salvar: clicar ou arrastar, miniaturas de prévia, remover. A
 * primeira foto vira a capa (no site, na loja do WhatsApp e na vitrine do painel).
 */
export function criarSeletorFotos(): SeletorFotos {
  let selecionadas: File[] = [];

  const entrada = document.createElement("input");
  entrada.type = "file";
  entrada.accept = TIPOS_ACEITOS.join(",");
  entrada.multiple = true;
  entrada.id = "novo-produto-fotos";
  entrada.className = "seletor-arquivo__entrada";

  const area = document.createElement("label");
  area.htmlFor = entrada.id;
  area.className = "zona-fotos";
  const chamada = document.createElement("strong");
  chamada.textContent = "Clique ou arraste as fotos aqui";
  const detalhe = document.createElement("span");
  detalhe.textContent = `JPG, PNG ou WEBP · até 5 MB cada · no máximo ${FOTOS_POR_PRODUTO} fotos`;
  area.append(chamada, detalhe);

  const aviso = document.createElement("p");
  aviso.className = "leitura__aviso";
  aviso.setAttribute("role", "status");

  const previas = document.createElement("div");
  previas.className = "grade-fotos";

  const adicionar = (novos: File[]): void => {
    const recusados: string[] = [];
    for (const arquivo of novos) {
      if (!TIPOS_ACEITOS.includes(arquivo.type)) {
        recusados.push(`${arquivo.name}: formato não aceito`);
      } else if (arquivo.size > TAMANHO_MAXIMO_BYTES) {
        recusados.push(`${arquivo.name}: maior que 5 MB`);
      } else if (selecionadas.length >= FOTOS_POR_PRODUTO) {
        recusados.push(`${arquivo.name}: limite de ${FOTOS_POR_PRODUTO} fotos`);
      } else {
        selecionadas = [...selecionadas, arquivo];
      }
    }
    aviso.textContent = recusados.join(" · ");
    renderizarPrevias();
  };

  const renderizarPrevias = (): void => {
    previas.querySelectorAll("img").forEach((imagem) => URL.revokeObjectURL(imagem.src));
    previas.replaceChildren(...selecionadas.map((arquivo, indice) => criarPrevia(arquivo, indice === 0, () => {
      selecionadas = selecionadas.filter((_, posicao) => posicao !== indice);
      renderizarPrevias();
    })));
  };

  entrada.addEventListener("change", () => {
    adicionar(Array.from(entrada.files ?? []));
    entrada.value = "";
  });
  area.addEventListener("dragover", (evento) => {
    evento.preventDefault();
    area.classList.add("zona-fotos--arrastando");
  });
  area.addEventListener("dragleave", () => area.classList.remove("zona-fotos--arrastando"));
  area.addEventListener("drop", (evento) => {
    evento.preventDefault();
    area.classList.remove("zona-fotos--arrastando");
    adicionar(Array.from(evento.dataTransfer?.files ?? []));
  });

  const elemento = document.createElement("div");
  elemento.className = "seletor-fotos";
  elemento.append(entrada, area, aviso, previas);
  return { elemento, arquivos: () => [...selecionadas] };
}

function criarPrevia(arquivo: File, capa: boolean, aoRemover: () => void): HTMLElement {
  const imagem = document.createElement("img");
  imagem.src = URL.createObjectURL(arquivo);
  imagem.alt = arquivo.name;

  const remover = document.createElement("button");
  remover.type = "button";
  remover.className = "grade-fotos__remover";
  remover.textContent = "Remover";
  remover.addEventListener("click", aoRemover);

  const item = document.createElement("figure");
  item.className = "grade-fotos__item";
  item.append(imagem, remover);
  if (capa) {
    const marca = document.createElement("span");
    marca.className = "grade-fotos__capa";
    marca.textContent = "Capa";
    item.append(marca);
  }
  return item;
}
