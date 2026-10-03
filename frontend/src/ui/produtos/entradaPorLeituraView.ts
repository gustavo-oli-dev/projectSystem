import { atualizarProduto, darEntradaNoEstoque, type Produto } from "../../api/produtosApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";

const SO_DIGITOS = /^\d{8,14}$/;

/**
 * Entrada de estoque com o leitor: com o produto já escolhido, cada leitura conta +1 unidade. Nada
 * vai ao servidor até "Confirmar" (uma movimentação só, com o total contado).
 * - Produto sem código: a primeira leitura oferece vincular o código lido a ele.
 * - Código de outro produto: avisa e não conta (evita somar no produto errado).
 */
export function criarEntradaPorLeitura(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  let codigoDoProduto = produto.codigoBarras;
  let contadas = 0;

  const entrada = document.createElement("input");
  entrada.type = "text";
  entrada.className = "leitura__campo";
  entrada.placeholder = "Clique aqui e leia cada unidade com o leitor";
  entrada.setAttribute("aria-label", "Leitura das unidades que chegaram");
  entrada.autocomplete = "off";

  const contador = document.createElement("p");
  contador.className = "contador-leitura";
  const aviso = document.createElement("p");
  aviso.className = "leitura__aviso";
  aviso.setAttribute("role", "status");
  const erro = criarMensagemErro();

  const confirmar = document.createElement("button");
  confirmar.type = "button";
  confirmar.className = "btn btn-primary btn-pequeno";
  const zerar = document.createElement("button");
  zerar.type = "button";
  zerar.className = "btn btn-ghost btn-pequeno";
  zerar.textContent = "Zerar contagem";

  const atualizarContador = (): void => {
    contador.textContent = `${contadas} unidade(s) lida(s)`;
    confirmar.textContent = `Confirmar entrada de ${contadas}`;
    confirmar.disabled = contadas === 0;
    zerar.disabled = contadas === 0;
  };

  const contar = (): void => {
    contadas += 1;
    aviso.textContent = "";
    atualizarContador();
  };

  entrada.addEventListener("keydown", (evento) => {
    if (evento.key !== "Enter") {
      return;
    }
    evento.preventDefault();
    const codigo = entrada.value.trim();
    entrada.value = "";
    if (!SO_DIGITOS.test(codigo)) {
      aviso.textContent = "Leitura inválida — leia o código de barras de novo.";
      return;
    }
    if (codigoDoProduto === codigo) {
      contar();
      return;
    }
    if (codigoDoProduto !== null) {
      aviso.textContent = `O código ${codigo} não é deste produto. Unidade não contada.`;
      return;
    }
    void vincularCodigo(codigo);
  });

  async function vincularCodigo(codigo: string): Promise<void> {
    if (!possui("CATALOGO_GERENCIAR")) {
      aviso.textContent = "Este produto ainda não tem código de barras. Peça a quem gerencia o catálogo para cadastrá-lo.";
      return;
    }
    if (!window.confirm(`Este produto não tem código de barras. Vincular o código ${codigo} a "${produto.nome}"?`)) {
      return;
    }
    try {
      await atualizarProduto(produto.id, {
        nome: produto.nome,
        descricao: produto.descricao,
        precoUnitario: produto.precoUnitario,
        codigoBarras: codigo,
        custoUnitario: produto.custoUnitario,
      });
      codigoDoProduto = codigo;
      contar();
    } catch (falha) {
      mostrarErro(erro, falha, "Não foi possível vincular o código.");
    }
  }

  confirmar.addEventListener("click", () => {
    confirmar.disabled = true;
    darEntradaNoEstoque(produto.id, contadas)
      .then(recarregar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível dar entrada.");
        confirmar.disabled = false;
      });
  });
  zerar.addEventListener("click", () => {
    contadas = 0;
    atualizarContador();
    entrada.focus();
  });

  const acoes = document.createElement("div");
  acoes.className = "barra-acoes";
  acoes.append(zerar, confirmar);

  const titulo = document.createElement("p");
  titulo.className = "subtitulo-bloco";
  titulo.textContent = "Entrada por leitura";

  const bloco = document.createElement("div");
  bloco.className = "entrada-leitura";
  bloco.append(titulo, entrada, contador, aviso, erro, acoes);
  atualizarContador();
  return bloco;
}
