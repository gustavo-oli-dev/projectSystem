import { enviarFoto, removerFoto, type FotoProduto, type Produto } from "../../api/produtosApi.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { criarSecao } from "./secaoEdicao.js";

const FOTOS_POR_PRODUTO = 8;
const TIPOS_ACEITOS = "image/jpeg,image/png,image/webp";
/** Mesmo limite do backend; conferido antes para não enviar à toa nem receber erro do nginx. */
const TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;

export function criarSecaoFotos(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const erro = criarMensagemErro();

  const grade = document.createElement("div");
  grade.className = "grade-fotos";
  grade.append(...produto.fotos.map((foto, indice) => criarMiniatura(produto, foto, indice === 0, erro, recarregar)));

  const ajuda = document.createElement("p");
  ajuda.className = "nota-campo";
  ajuda.textContent = `JPG, PNG ou WEBP, até 5 MB cada, no máximo ${FOTOS_POR_PRODUTO} fotos. A primeira é a capa.`;

  const conteudo: HTMLElement[] = [];
  if (produto.fotos.length === 0) {
    const vazio = document.createElement("p");
    vazio.className = "estado-info";
    vazio.textContent = "Nenhuma foto ainda.";
    conteudo.push(vazio);
  } else {
    conteudo.push(grade);
  }
  if (produto.fotos.length < FOTOS_POR_PRODUTO) {
    conteudo.push(criarSeletorDeArquivos(produto, erro, recarregar));
  }

  return criarSecao("Fotos", ...conteudo, ajuda, erro);
}

function criarMiniatura(
  produto: Produto, foto: FotoProduto, capa: boolean, erro: HTMLElement, recarregar: () => Promise<void>
): HTMLElement {
  const imagem = document.createElement("img");
  imagem.src = foto.url;
  imagem.alt = `${produto.nome} — foto`;
  imagem.loading = "lazy";

  const botaoRemover = document.createElement("button");
  botaoRemover.type = "button";
  botaoRemover.className = "grade-fotos__remover";
  botaoRemover.textContent = "Remover";
  botaoRemover.addEventListener("click", () => {
    if (!window.confirm("Remover esta foto?")) {
      return;
    }
    botaoRemover.disabled = true;
    removerFoto(produto.id, foto.id)
      .then(recarregar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível remover a foto.");
        botaoRemover.disabled = false;
      });
  });

  const item = document.createElement("figure");
  item.className = "grade-fotos__item";
  item.append(imagem, botaoRemover);
  if (capa) {
    const marca = document.createElement("span");
    marca.className = "grade-fotos__capa";
    marca.textContent = "Capa";
    item.append(marca);
  }
  return item;
}

/** Envia os arquivos um por vez, para um erro (ex.: arquivo grande) não perder os outros. */
function criarSeletorDeArquivos(produto: Produto, erro: HTMLElement, recarregar: () => Promise<void>): HTMLElement {
  const entrada = document.createElement("input");
  entrada.type = "file";
  entrada.accept = TIPOS_ACEITOS;
  entrada.multiple = true;
  entrada.id = "fotos-produto";
  entrada.className = "seletor-arquivo__entrada";

  const rotulo = document.createElement("label");
  rotulo.htmlFor = entrada.id;
  rotulo.className = "btn btn-outline btn-pequeno";
  rotulo.textContent = "+ Adicionar fotos";

  entrada.addEventListener("change", () => {
    const arquivos = Array.from(entrada.files ?? []);
    if (arquivos.length === 0) {
      return;
    }
    rotulo.textContent = "Enviando...";
    void enviarEmSequencia(produto.id, arquivos).then((falhas) => {
      if (falhas.length === 0) {
        return recarregar();
      }
      // Mantém o erro visível; as fotos que deram certo aparecem ao atualizar.
      mostrarErro(erro, new Error(falhas.join(" · ")), "");
      rotulo.textContent = "Atualizar fotos";
      rotulo.htmlFor = "";
      rotulo.addEventListener("click", () => void recarregar(), { once: true });
      return undefined;
    });
  });

  const bloco = document.createElement("div");
  bloco.className = "seletor-arquivo";
  bloco.append(entrada, rotulo);
  return bloco;
}

/** @returns uma mensagem por arquivo que falhou (vazio = todos enviados) */
async function enviarEmSequencia(produtoId: string, arquivos: File[]): Promise<string[]> {
  const falhas: string[] = [];
  for (const arquivo of arquivos) {
    if (arquivo.size > TAMANHO_MAXIMO_BYTES) {
      falhas.push(`${arquivo.name}: maior que 5 MB`);
      continue;
    }
    try {
      await enviarFoto(produtoId, arquivo);
    } catch (falha) {
      falhas.push(`${arquivo.name}: ${falha instanceof Error ? falha.message : "falhou"}`);
    }
  }
  return falhas;
}
