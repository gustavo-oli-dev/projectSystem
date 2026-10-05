import type { ConferenciaCaixa, FormaVendaCaixa } from "../../api/caixaApi.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { ROTULO_CEDULA } from "./contagemCedulas.js";

const ROTULO_FORMA: Record<FormaVendaCaixa, string> = {
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Cartão de crédito",
  CARTAO_DEBITO: "Cartão de débito",
  PIX: "Pix na maquininha",
  PIX_QR: "Pix (QR na tela)",
};
const HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

/**
 * Conferência de um caixa. Primeiro a pergunta que importa: o dinheiro que entrou na gaveta bate
 * com o que foi vendido em dinheiro (produto por produto)? Certo, devendo ou sobrando. Depois a
 * conta detalhada da gaveta, as outras formas e a maquininha.
 * Usada no fim do fechamento e na tela de conferência.
 */
export function criarResultadoFechamento(conferencia: ConferenciaCaixa): HTMLElement {
  const elemento = document.createElement("div");
  elemento.className = "resultado-caixa";
  elemento.append(criarSituacao(conferencia));
  if (conferencia.dinheiroQueEntrou !== null && conferencia.diferenca !== null) {
    elemento.append(criarComparacao(conferencia.vendasEmDinheiro, conferencia.dinheiroQueEntrou, conferencia.diferenca));
  }
  elemento.append(
    criarProdutosEmDinheiro(conferencia),
    criarContaDaGaveta(conferencia),
    criarVendasPorForma(conferencia)
  );
  if (conferencia.conferenciasForma.length > 0) {
    elemento.append(criarConferenciaMaquininha(conferencia));
  }
  if (conferencia.movimentos.length > 0) {
    elemento.append(criarMovimentos(conferencia));
  }
  if (conferencia.observacao !== null) {
    const observacao = document.createElement("p");
    observacao.className = "resultado-caixa__observacao";
    observacao.textContent = `Observação: ${conferencia.observacao}`;
    elemento.append(observacao);
  }
  return elemento;
}

/** "Caixa certo", "Caixa devendo R$ X" ou "Caixa sobrando R$ X" — texto e cor (nunca só a cor). */
function criarSituacao(conferencia: ConferenciaCaixa): HTMLElement {
  const situacao = document.createElement("p");
  situacao.className = "resultado-caixa__situacao";
  const diferenca = conferencia.diferenca;
  if (diferenca === null) {
    situacao.textContent = "Caixa ainda aberto — os valores abaixo são de agora";
    return situacao;
  }
  if (diferenca === 0) {
    situacao.classList.add("resultado-caixa__situacao--bateu");
    situacao.textContent = "✓ Caixa certo — o dinheiro da gaveta bate com o que foi vendido em dinheiro";
  } else if (diferenca > 0) {
    situacao.classList.add("resultado-caixa__situacao--sobra");
    situacao.textContent = `▲ Caixa sobrando ${formatarMoeda(diferenca)} — entrou mais dinheiro do que foi vendido`;
  } else {
    situacao.classList.add("resultado-caixa__situacao--falta");
    situacao.textContent = `▼ Caixa devendo ${formatarMoeda(-diferenca)} — entrou menos dinheiro do que foi vendido`;
  }
  return situacao;
}

/**
 * Vendido em dinheiro × o que entrou na gaveta. "Entrou" já desconta o valor inicial e as
 * reposições e soma de volta as sangrias — sobra só o dinheiro das vendas.
 */
function criarComparacao(vendidoEmDinheiro: number, entrouNaGaveta: number, diferenca: number): HTMLElement {
  const resultado = diferenca === 0 ? "Certo"
    : diferenca > 0 ? `Sobrando ${formatarMoeda(diferenca)}` : `Devendo ${formatarMoeda(-diferenca)}`;
  return secao("Vendido em dinheiro × dinheiro na gaveta", criarListaConta([
    ["Vendido em dinheiro (produtos abaixo)", formatarMoeda(vendidoEmDinheiro)],
    ["Entrou na gaveta (contado − valor inicial − reposições + sangrias)", formatarMoeda(entrouNaGaveta)],
    ["Resultado", resultado, "resultado-caixa__linha--forte"],
  ]));
}

function criarProdutosEmDinheiro(conferencia: ConferenciaCaixa): HTMLElement {
  if (conferencia.produtosEmDinheiro.length === 0) {
    const vazio = document.createElement("p");
    vazio.className = "resultado-caixa__vazio";
    vazio.textContent = "Nenhum produto vendido em dinheiro neste caixa.";
    return secao("Produtos vendidos em dinheiro", vazio);
  }
  const tabela = criarTabela(["Produto", "Quantidade", "Valor"], conferencia.produtosEmDinheiro.map((produto) => criarLinha(
    celula(produto.descricao),
    celula(String(produto.quantidade)),
    celula(formatarMoeda(produto.valor))
  )), "produto(s)");
  return secao("Produtos vendidos em dinheiro", tabela);
}

function criarListaConta(linhas: ReadonlyArray<readonly [string, string, string?]>): HTMLElement {
  const lista = document.createElement("dl");
  lista.className = "resultado-caixa__conta";
  for (const [rotulo, valor, classe] of linhas) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    const linha = document.createElement("div");
    linha.className = classe === undefined ? "resultado-caixa__linha" : `resultado-caixa__linha ${classe}`;
    linha.append(termo, definicao);
    lista.append(linha);
  }
  return lista;
}

function criarContaDaGaveta(conferencia: ConferenciaCaixa): HTMLElement {
  const linhas: Array<[string, string, string?]> = [
    ["Valor inicial (fundo de troco)", formatarMoeda(conferencia.fundoInicial)],
    ["+ Vendas em dinheiro", formatarMoeda(conferencia.vendasEmDinheiro)],
    ["+ Reposições de troco", formatarMoeda(conferencia.totalSuprimentos)],
    ["− Sangrias", formatarMoeda(conferencia.totalSangrias)],
    ["= Deveria ter na gaveta", formatarMoeda(conferencia.valorEsperado), "resultado-caixa__linha--forte"],
  ];
  if (conferencia.valorContado !== null) {
    linhas.push(["Contado na gaveta", formatarMoeda(conferencia.valorContado), "resultado-caixa__linha--forte"]);
  }
  if (conferencia.dinheiroQueEntrou !== null) {
    linhas.push(["Entrou em dinheiro no dia (contado − valor inicial)", formatarMoeda(conferencia.dinheiroQueEntrou)]);
  }

  const lista = document.createElement("dl");
  lista.className = "resultado-caixa__conta";
  for (const [rotulo, valor, classe] of linhas) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    const linha = document.createElement("div");
    linha.className = classe === undefined ? "resultado-caixa__linha" : `resultado-caixa__linha ${classe}`;
    linha.append(termo, definicao);
    lista.append(linha);
  }
  return secao("Dinheiro da gaveta", lista);
}

function criarVendasPorForma(conferencia: ConferenciaCaixa): HTMLElement {
  if (conferencia.vendasPorForma.length === 0) {
    const vazio = document.createElement("p");
    vazio.className = "resultado-caixa__vazio";
    vazio.textContent = "Nenhuma venda neste caixa.";
    return secao("Vendas por forma de pagamento", vazio);
  }
  const tabela = criarTabela(["Forma", "Vendas", "Valor"], conferencia.vendasPorForma.map((forma) => criarLinha(
    celula(ROTULO_FORMA[forma.forma]),
    celula(String(forma.vendas)),
    celula(formatarMoeda(forma.valor))
  )), "forma(s)");
  return secao("Vendas por forma de pagamento", tabela);
}

/** Crédito, débito e Pix: sistema × relatório da maquininha, com a diferença de cada um. */
function criarConferenciaMaquininha(conferencia: ConferenciaCaixa): HTMLElement {
  const tabela = criarTabela(["Forma", "No sistema", "Na maquininha", "Diferença"], conferencia.conferenciasForma.map((forma) => criarLinha(
    celula(ROTULO_FORMA[forma.forma]),
    celula(formatarMoeda(forma.valorSistema)),
    celula(formatarMoeda(forma.valorInformado)),
    celulaDiferenca(forma.diferenca)
  )), "forma(s)");
  return secao("Conferência da maquininha", tabela);
}

function celulaDiferenca(diferenca: number): HTMLTableCellElement {
  if (diferenca === 0) {
    return celulaSelo("Certo", "concluido");
  }
  return diferenca > 0
    ? celulaSelo(`Sobrando ${formatarMoeda(diferenca)}`, "pendente")
    : celulaSelo(`Devendo ${formatarMoeda(-diferenca)}`, "rejeitado");
}

function criarMovimentos(conferencia: ConferenciaCaixa): HTMLElement {
  const tabela = criarTabela(["Quando", "Tipo", "Valor", "Cédulas", "Motivo"], conferencia.movimentos.map((movimento) => criarLinha(
    celula(HORA.format(new Date(movimento.registradoEm))),
    celula(movimento.tipo === "SUPRIMENTO" ? "Reposição de troco" : "Sangria"),
    celula(formatarMoeda(movimento.valor)),
    celula(movimento.cedulas.map((cedula) => `${cedula.quantidade} × ${ROTULO_CEDULA[cedula.cedula]}`).join(", ") || "—"),
    celula(movimento.motivo)
  )), "movimento(s)");
  return secao("Reposições e sangrias", tabela);
}

function secao(titulo: string, conteudo: HTMLElement): HTMLElement {
  const cabecalho = document.createElement("h3");
  cabecalho.textContent = titulo;
  const elemento = document.createElement("section");
  elemento.className = "resultado-caixa__secao";
  elemento.append(cabecalho, conteudo);
  return elemento;
}
