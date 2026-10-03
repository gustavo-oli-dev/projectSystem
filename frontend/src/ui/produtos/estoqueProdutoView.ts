import {
  darEntradaNoEstoque,
  listarMovimentacoes,
  type MovimentacaoEstoque,
  type Produto,
  type TipoMovimentacao,
} from "../../api/produtosApi.js";
import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { criarEntradaPorLeitura } from "./entradaPorLeituraView.js";
import { criarSecao } from "./secaoEdicao.js";
import { situacaoEstoque } from "./situacaoEstoque.js";

const ROTULO_MOVIMENTACAO: Record<TipoMovimentacao, string> = {
  ENTRADA: "Entrada",
  VENDA: "Venda",
  DEVOLUCAO: "Devolução",
};
const MODIFICADOR_MOVIMENTACAO: Record<TipoMovimentacao, string> = {
  ENTRADA: "em_estoque",
  VENDA: "aberto",
  DEVOLUCAO: "estoque_baixo",
};

export function criarSecaoEstoque(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const situacao = situacaoEstoque(produto.quantidadeEmEstoque);
  const saldo = document.createElement("p");
  saldo.className = "saldo-estoque";
  const numero = document.createElement("strong");
  numero.textContent = `${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`;
  const selo = document.createElement("span");
  selo.className = `selo selo--${situacao.modificador}`;
  selo.textContent = situacao.rotulo;
  saldo.append(numero, selo);

  const historico = document.createElement("div");
  historico.append(elementoCarregando("Carregando histórico..."));
  void carregarHistorico(produto, historico);

  // Controles à esquerda (saldo e entradas), histórico à direita — usa a largura toda da tela.
  const controles = document.createElement("div");
  controles.className = "estoque__controles";
  controles.append(saldo);
  if (possui("ESTOQUE_GERENCIAR")) {
    controles.append(criarEntradaPorLeitura(produto, recarregar), criarFormularioEntrada(produto, recarregar));
  }

  const tituloHistorico = document.createElement("p");
  tituloHistorico.className = "subtitulo-bloco";
  tituloHistorico.textContent = "Histórico de movimentações";
  const colunaHistorico = document.createElement("div");
  colunaHistorico.append(tituloHistorico, historico);

  const colunas = document.createElement("div");
  colunas.className = "estoque__colunas";
  colunas.append(controles, colunaHistorico);
  return criarSecao("Estoque", colunas);
}

function criarFormularioEntrada(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const quantidade = criarCampoTexto("estoque-quantidade", "Ou digite a quantidade que chegou", "number", true);
  quantidade.entrada.min = "1";
  quantidade.entrada.step = "1";

  const erro = criarMensagemErro();
  const botao = document.createElement("button");
  botao.type = "submit";
  botao.className = "btn btn-primary btn-pequeno";
  botao.textContent = "Dar entrada";

  const formulario = document.createElement("form");
  formulario.className = "formulario-linha";
  formulario.append(quantidade.container, botao, erro);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    botao.disabled = true;
    darEntradaNoEstoque(produto.id, Number(quantidade.entrada.value))
      .then(recarregar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível dar entrada.");
        botao.disabled = false;
      });
  });
  return formulario;
}

async function carregarHistorico(produto: Produto, area: HTMLElement): Promise<void> {
  try {
    const movimentacoes = await listarMovimentacoes(produto.id);
    if (movimentacoes.length === 0) {
      area.replaceChildren(cartaoEstado("Nenhuma movimentação ainda."));
      return;
    }
    area.replaceChildren(criarTabela(
      ["Quando", "Tipo", "Quantidade", "Saldo depois", "Pedido", "Responsável"],
      movimentacoes.map(criarLinhaMovimentacao),
      "movimentação(ões) recentes"));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar o histórico.", "erro"));
  }
}

function criarLinhaMovimentacao(movimentacao: MovimentacaoEstoque): HTMLTableRowElement {
  const sinal = movimentacao.tipo === "VENDA" ? "−" : "+";
  return criarLinha(
    celula(new Date(movimentacao.criadaEm).toLocaleString("pt-BR", {
      day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit",
    })),
    celulaSelo(ROTULO_MOVIMENTACAO[movimentacao.tipo], MODIFICADOR_MOVIMENTACAO[movimentacao.tipo]),
    celula(`${sinal}${movimentacao.quantidade}`),
    celula(String(movimentacao.saldoApos)),
    movimentacao.pedidoId === null ? celula("—") : celulaComConteudo(criarLinkPedido(movimentacao.pedidoId)),
    celulaResponsavel(movimentacao.responsavel)
  );
}

function criarLinkPedido(pedidoId: string): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "link-tabela";
  botao.textContent = `#${pedidoId.slice(0, 8)}`;
  botao.addEventListener("click", () => navegarPara("pedido-detalhe", pedidoId));
  return botao;
}

/** Mostra só a parte antes do @ (cabe na coluna); o e-mail completo fica no "title". */
function celulaResponsavel(responsavel: string): HTMLTableCellElement {
  const elemento = celula(responsavel.split("@")[0] ?? responsavel);
  elemento.title = responsavel;
  return elemento;
}
