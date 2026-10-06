import {
  listarPrecosAlterados,
  listarProdutos,
  reajustarPrecos,
  type ModoReajuste,
  type PrecoAlterado,
  type Produto,
} from "../../api/produtosApi.js";
import { listarPromocoesValendoHoje, type Promocao } from "../../api/promocoesApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, criarLinha, criarTabela } from "../tabela.js";
import { imprimirEtiquetas } from "./etiquetasGondola.js";

const DIAS_PRECO_ALTERADO = 7;

const MODOS: ReadonlyArray<{ valor: ModoReajuste; rotulo: string }> = [
  { valor: "PERCENTUAL", rotulo: "Percentual (ex.: 5 = 5% mais caro, -10 = 10% mais barato)" },
  { valor: "PRECO_UNICO", rotulo: "Mesmo preço para todos (ex.: 9,99)" },
];

/**
 * Preços e etiquetas (D39): marque os produtos, reajuste o preço de todos de uma vez e imprima as
 * etiquetas de gôndola (nome, preço, promoção do dia e código de barras).
 */
export async function montarPrecos(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Preços e etiquetas";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, area);

  const recarregar = async (): Promise<void> => {
    area.replaceChildren(elementoCarregando("Carregando produtos..."));
    try {
      const [produtos, promocoes] = await Promise.all([
        listarProdutos(), listarPromocoesValendoHoje().catch((): Promocao[] => []),
      ]);
      const ativos = produtos.filter((produto) => produto.ativo);
      area.replaceChildren(ativos.length === 0
        ? cartaoEstado("Nenhum produto à venda.")
        : montarTela(ativos, promocoes, () => void recarregar()));
    } catch {
      area.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
    }
  };
  await recarregar();
}

function montarTela(produtos: readonly Produto[], promocoes: readonly Promocao[], aoMudar: () => void): HTMLElement {
  const marcados = new Set<string>();
  const caixas = new Map<string, HTMLInputElement>();
  const resumo = document.createElement("p");
  resumo.className = "precos__resumo";
  const atualizarResumo = (): void => {
    resumo.textContent = `${marcados.size} produto(s) marcado(s)`;
    imprimir.disabled = marcados.size === 0;
    aplicar.disabled = marcados.size === 0;
  };
  const marcar = (ids: Iterable<string>, valor: boolean): void => {
    for (const id of ids) {
      const caixa = caixas.get(id);
      if (caixa === undefined) {
        continue;
      }
      caixa.checked = valor;
      if (valor) {
        marcados.add(id);
      } else {
        marcados.delete(id);
      }
    }
    atualizarResumo();
  };

  const tabela = criarTabela(
    ["", "Produto", "Código de barras", "Preço"],
    produtos.map((produto) => {
      const caixa = document.createElement("input");
      caixa.type = "checkbox";
      caixa.setAttribute("aria-label", `Marcar ${produto.nome}`);
      caixa.addEventListener("change", () => marcar([produto.id], caixa.checked));
      caixas.set(produto.id, caixa);
      return criarLinha(
        celulaComConteudo(caixa),
        celula(produto.nome),
        celula(produto.codigoBarras ?? "—"),
        celula(formatarMoeda(produto.precoUnitario))
      );
    }),
    "produto(s)"
  );

  const erro = criarMensagemErro();
  const todos = botao("Marcar todos", "btn btn-ghost btn-pequeno", () => marcar(produtos.map((produto) => produto.id), true));
  const nenhum = botao("Desmarcar", "btn btn-ghost btn-pequeno", () => marcar([...marcados], false));
  const alterados = botao(`Mudaram de preço (${DIAS_PRECO_ALTERADO} dias)`, "btn btn-ghost btn-pequeno", () => {
    erro.hidden = true;
    listarPrecosAlterados(DIAS_PRECO_ALTERADO)
      .then((ids) => {
        marcar([...marcados], false);
        marcar(ids, true);
        if (ids.length === 0) {
          mostrarErro(erro, null, `Nenhum produto mudou de preço nos últimos ${DIAS_PRECO_ALTERADO} dias.`);
        }
      })
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível ver os preços alterados."));
  });
  const imprimir = botao("Imprimir etiquetas", "btn btn-primary btn-pequeno", () =>
    imprimirEtiquetas(produtos.filter((produto) => marcados.has(produto.id)), promocoes));

  const barra = document.createElement("div");
  barra.className = "precos__barra";
  barra.append(resumo, todos, nenhum, alterados, imprimir);

  const modo = criarCampoSelecao("reajuste-modo", "Como reajustar", MODOS);
  const valor = criarCampoTexto("reajuste-valor", "Valor", "number", true);
  valor.entrada.step = "0.01";
  const aplicar = botao("Aplicar nos marcados", "btn btn-primary", () => undefined);
  aplicar.type = "submit";
  const resultado = document.createElement("div");
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao precos__reajuste";
  const tituloReajuste = document.createElement("h2");
  tituloReajuste.textContent = "Reajustar preço dos marcados";
  const campos = document.createElement("div");
  campos.className = "precos__campos";
  campos.append(modo.container, valor.container, aplicar);
  formulario.append(tituloReajuste, campos, resultado);
  formulario.hidden = !possui("CATALOGO_GERENCIAR");
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    const numero = Number(valor.entrada.value);
    // Valor vem de uma lista fixa de opções (MODOS), então sempre é um modo válido.
    const modoEscolhido = modo.selecao.value as ModoReajuste;
    const explicacao = modoEscolhido === "PERCENTUAL" ? `${numero > 0 ? "+" : ""}${numero}%` : `todos a ${formatarMoeda(numero)}`;
    if (!window.confirm(`Mudar o preço de ${marcados.size} produto(s): ${explicacao}?`)) {
      return;
    }
    aplicar.disabled = true;
    reajustarPrecos([...marcados], modoEscolhido, numero)
      .then((mudancas) => resultado.replaceChildren(criarResultado(mudancas, produtos, promocoes, aoMudar)))
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível reajustar os preços.");
        aplicar.disabled = false;
      });
  });

  atualizarResumo();
  const tela = document.createElement("div");
  tela.className = "precos";
  tela.append(formulario, barra, erro, tabela);
  return tela;
}

/** Mostra de quanto para quanto e oferece imprimir as etiquetas só dos que mudaram. */
function criarResultado(
  mudancas: readonly PrecoAlterado[], produtos: readonly Produto[], promocoes: readonly Promocao[], aoMudar: () => void
): HTMLElement {
  const bloco = document.createElement("div");
  bloco.className = "precos__resultado";
  if (mudancas.length === 0) {
    bloco.append(cartaoEstado("Nenhum preço mudou (os novos preços eram iguais aos atuais)."));
    return bloco;
  }
  const lista = criarTabela(["Produto", "Antes", "Agora"], mudancas.map((mudanca) => criarLinha(
    celula(mudanca.produto), celula(formatarMoeda(mudanca.precoAnterior)), celula(formatarMoeda(mudanca.precoNovo))
  )), "preço(s) alterado(s)");
  const novosPrecos = new Map(mudancas.map((mudanca) => [mudanca.produtoId, mudanca.precoNovo]));
  const atualizados = produtos
    .filter((produto) => novosPrecos.has(produto.id))
    .map((produto) => ({ ...produto, precoUnitario: novosPrecos.get(produto.id) ?? produto.precoUnitario }));
  const imprimir = botao("Imprimir etiquetas destes", "btn btn-primary", () => imprimirEtiquetas(atualizados, promocoes));
  const concluir = botao("Concluir", "btn btn-ghost", aoMudar);
  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(concluir, imprimir);
  bloco.append(lista, acoes);
  return bloco;
}

function botao(texto: string, classe: string, aoClicar: () => void): HTMLButtonElement {
  const elemento = document.createElement("button");
  elemento.type = "button";
  elemento.className = classe;
  elemento.textContent = texto;
  elemento.addEventListener("click", aoClicar);
  return elemento;
}
