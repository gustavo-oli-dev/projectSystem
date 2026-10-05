import type { RelatorioPerdas } from "../../api/relatoriosApi.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { criarGraficoRosca, type CorFatia, type Fatia } from "../graficos/graficoRosca.js";
import { rotuloDoMotivo } from "../produtos/motivosPerda.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";
import { criarCartao } from "./secoesRelatorio.js";

/**
 * Cor fixa por motivo. Só 4 cores passam no validador em qualquer combinação (1, 2, 3 e 7);
 * uso interno e "outro" se juntam no cinza "Outros" (regra: o que passa do limite vira "Outros").
 */
const COR_DO_MOTIVO: Record<string, CorFatia> = { VENCIDO: 1, AVARIADO: 2, INVENTARIO: 3, FURTO: 7 };
const ROTULO_OUTROS = "Outros (uso interno, outro)";

/** Perdas e quebras no período: por motivo (pizza) e por produto (tabela). */
export function montarSecaoPerdas(relatorio: RelatorioPerdas): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.className = "dinheiro-do-dia__titulo";
  titulo.textContent = "Perdas e quebras";
  const resumo = document.createElement("p");
  resumo.className = "secao-perdas__resumo";
  resumo.textContent = relatorio.unidades === 0
    ? "Nenhuma perda no período."
    : `${formatarInteiro(relatorio.unidades)} unidade(s) saíram sem ser vendidas · ${formatarMoeda(relatorio.valor)} em custo perdido`
      + (relatorio.unidadesSemCusto > 0 ? ` (${formatarInteiro(relatorio.unidadesSemCusto)} sem custo cadastrado, fora do valor)` : "");

  const secao = document.createElement("section");
  secao.className = "painel-relatorio";
  secao.append(titulo, resumo);
  if (relatorio.unidades === 0) {
    return secao;
  }
  const porProduto = criarCartao("Produtos que mais se perderam", criarTabela(
    ["Produto", "Unidades", "Custo perdido"],
    relatorio.porProduto.map((item) => criarLinha(
      celula(item.chave), celula(formatarInteiro(item.unidades)), celula(formatarMoeda(item.valor))
    )),
    "produto(s)"
  ));
  porProduto.classList.add("cartao-relatorio--largo");
  const linha = document.createElement("div");
  linha.className = "linha-relatorio";
  linha.append(criarCartao("Por motivo", criarPizzaMotivos(relatorio)), porProduto);
  secao.append(linha);
  return secao;
}

function criarPizzaMotivos(relatorio: RelatorioPerdas): HTMLElement {
  const fatias: Fatia[] = [];
  let outros = 0;
  for (const item of relatorio.porMotivo) {
    const cor = COR_DO_MOTIVO[item.chave];
    if (cor === undefined) {
      outros += item.unidades;
      continue;
    }
    fatias.push({ rotulo: rotuloDoMotivo(item.chave), cor, valor: item.unidades, detalhe: formatarMoeda(item.valor) });
  }
  if (outros > 0) {
    fatias.push({ rotulo: ROTULO_OUTROS, cor: "restante", valor: outros, detalhe: "" });
  }
  if (!fatias.some((fatia) => fatia.valor > 0)) {
    return cartaoEstado("Nenhuma perda no período.");
  }
  return criarGraficoRosca({
    fatias, formatarValor: (valor) => `${formatarInteiro(valor)} un.`, formatarTotal: formatarInteiro,
    rotuloTotal: "unidades", descricao: "Perdas por motivo",
  });
}
