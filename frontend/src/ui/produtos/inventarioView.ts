import { aplicarInventario, listarProdutos, type AjusteInventario, type Produto } from "../../api/produtosApi.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";

interface LinhaContagem {
  produto: Produto;
  contado: HTMLInputElement;
}

/**
 * Inventário (D32): conte a prateleira, digite a quantidade real de cada produto e aplique. O
 * estoque passa a ser o contado e a diferença fica registrada (sobra ou falta). Produto sem
 * quantidade digitada fica como está.
 */
export async function montarInventario(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Inventário";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, area);
  await carregar(area);
}

async function carregar(area: HTMLElement): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando produtos..."));
  try {
    const produtos = (await listarProdutos()).filter((produto) => produto.ativo);
    area.replaceChildren(produtos.length === 0
      ? cartaoEstado("Nenhum produto ativo para contar.")
      : montarContagem(area, produtos));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
  }
}

function montarContagem(area: HTMLElement, produtos: readonly Produto[]): HTMLElement {
  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = "Digite quantas unidades de cada produto você contou na loja e no depósito. Produto em branco não muda. Ao aplicar, o estoque vira o contado e a diferença fica registrada no histórico.";

  const resumo = document.createElement("p");
  resumo.className = "abertura-caixas__resumo";
  const linhas: LinhaContagem[] = [];
  const atualizarResumo = (): void => {
    const contados = linhas.filter((linha) => linha.contado.value !== "");
    const comDiferenca = contados.filter((linha) => Number(linha.contado.value) !== linha.produto.quantidadeEmEstoque).length;
    resumo.textContent = `${contados.length} produto(s) contado(s) · ${comDiferenca} com diferença`;
  };

  const tabela = criarTabela(
    ["Produto", "Código de barras", "No sistema", "Contado", "Diferença"],
    produtos.map((produto) => {
      const contado = document.createElement("input");
      contado.type = "number";
      contado.min = "0";
      contado.step = "1";
      contado.inputMode = "numeric";
      contado.className = "inventario__contado";
      contado.setAttribute("aria-label", `Quantidade contada de ${produto.nome}`);
      const diferenca = document.createElement("span");
      diferenca.textContent = "—";
      contado.addEventListener("input", () => {
        diferenca.replaceChildren(criarDiferenca(contado.value, produto.quantidadeEmEstoque));
        atualizarResumo();
      });
      linhas.push({ produto, contado });
      return criarLinha(
        celula(produto.nome),
        celula(produto.codigoBarras ?? "—"),
        celula(`${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`),
        celulaComConteudo(contado),
        celulaComConteudo(diferenca)
      );
    }),
    "produto(s)"
  );

  const erro = criarMensagemErro();
  const aplicar = document.createElement("button");
  aplicar.type = "button";
  aplicar.className = "btn btn-primary";
  aplicar.textContent = "Aplicar inventário";
  aplicar.addEventListener("click", () => {
    erro.hidden = true;
    const contagens = linhas
      .filter((linha) => linha.contado.value !== "")
      .map((linha) => ({ produtoId: linha.produto.id, quantidadeContada: Number(linha.contado.value) }));
    if (contagens.length === 0) {
      mostrarErro(erro, null, "Digite a quantidade contada de ao menos um produto.");
      return;
    }
    aplicar.disabled = true;
    aplicarInventario(contagens)
      .then((ajustes) => area.replaceChildren(criarResultado(ajustes, () => void carregar(area))))
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível aplicar o inventário.");
        aplicar.disabled = false;
      });
  });

  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(resumo, aplicar);
  const painel = document.createElement("div");
  painel.className = "caixa-painel";
  painel.append(instrucao, tabela, erro, acoes);
  atualizarResumo();
  return painel;
}

/** "+2" (sobrou), "−3" (faltou), "certo" — com selo e texto, nunca só a cor. */
function criarDiferenca(valorContado: string, noSistema: number): HTMLElement {
  const texto = document.createElement("span");
  if (valorContado === "") {
    texto.textContent = "—";
    return texto;
  }
  const diferenca = Number(valorContado) - noSistema;
  const selo = document.createElement("span");
  selo.className = diferenca === 0 ? "selo selo--concluido" : diferenca > 0 ? "selo selo--pendente" : "selo selo--rejeitado";
  selo.textContent = diferenca === 0 ? "Certo" : diferenca > 0 ? `Sobra +${diferenca}` : `Falta ${diferenca}`;
  return selo;
}

function criarResultado(ajustes: readonly AjusteInventario[], aoNovo: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Inventário aplicado";
  const faltas = ajustes.filter((ajuste) => ajuste.diferenca < 0).length;
  const sobras = ajustes.filter((ajuste) => ajuste.diferenca > 0).length;
  const resumo = document.createElement("p");
  resumo.className = "caixa-painel__instrucao";
  resumo.textContent = `${ajustes.length} produto(s) contado(s): ${faltas} com falta, ${sobras} com sobra, ${ajustes.length - faltas - sobras} certo(s). As faltas aparecem no relatório de perdas.`;
  const tabela = criarTabela(["Produto", "Tinha no sistema", "Contado", "Resultado"], ajustes.map((ajuste) => criarLinha(
    celula(ajuste.nome),
    celula(String(ajuste.noSistema)),
    celula(String(ajuste.contado)),
    ajuste.diferenca === 0 ? celulaSelo("Certo", "concluido")
      : ajuste.diferenca > 0 ? celulaSelo(`Sobra +${ajuste.diferenca}`, "pendente") : celulaSelo(`Falta ${ajuste.diferenca}`, "rejeitado")
  )), "produto(s)");
  const novo = document.createElement("button");
  novo.type = "button";
  novo.className = "btn btn-primary";
  novo.textContent = "Fazer outro inventário";
  novo.addEventListener("click", aoNovo);
  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  acoes.append(novo);
  const painel = document.createElement("div");
  painel.className = "caixa-painel";
  painel.append(titulo, resumo, tabela, acoes);
  return painel;
}
