import type { ConferenciaCaixa, FormaVendaCaixa } from "../../api/caixaApi.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";
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
 * Conferência de um caixa: a conta do dinheiro da gaveta, linha por linha, até a diferença.
 * Usada no fim do fechamento (operador) e na tela de conferência (gerente).
 */
export function criarResultadoFechamento(conferencia: ConferenciaCaixa): HTMLElement {
  const elemento = document.createElement("div");
  elemento.className = "resultado-caixa";
  elemento.append(
    criarSituacao(conferencia),
    criarContaDaGaveta(conferencia),
    criarVendasPorForma(conferencia)
  );
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

/** "Bateu", "Sobrou R$ X" ou "Faltou R$ X" — texto e cor (nunca só a cor). */
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
    situacao.textContent = "✓ O caixa bateu";
  } else if (diferenca > 0) {
    situacao.classList.add("resultado-caixa__situacao--sobra");
    situacao.textContent = `▲ Sobrou ${formatarMoeda(diferenca)}`;
  } else {
    situacao.classList.add("resultado-caixa__situacao--falta");
    situacao.textContent = `▼ Faltou ${formatarMoeda(-diferenca)}`;
  }
  return situacao;
}

function criarContaDaGaveta(conferencia: ConferenciaCaixa): HTMLElement {
  const linhas: Array<[string, string, string?]> = [
    ["Fundo de troco (início)", formatarMoeda(conferencia.fundoInicial)],
    ["+ Vendas em dinheiro", formatarMoeda(conferencia.vendasEmDinheiro)],
    ["+ Reposições de troco", formatarMoeda(conferencia.totalSuprimentos)],
    ["− Sangrias", formatarMoeda(conferencia.totalSangrias)],
    ["= Deveria ter na gaveta", formatarMoeda(conferencia.valorEsperado), "resultado-caixa__linha--forte"],
  ];
  if (conferencia.valorContado !== null) {
    linhas.push(["Contado na gaveta", formatarMoeda(conferencia.valorContado), "resultado-caixa__linha--forte"]);
  }
  if (conferencia.dinheiroQueEntrou !== null) {
    linhas.push(["Entrou em dinheiro no dia (contado − fundo inicial)", formatarMoeda(conferencia.dinheiroQueEntrou)]);
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
