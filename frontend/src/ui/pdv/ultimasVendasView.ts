import { cancelarVendaDoBalcao, listarUltimasVendasDoBalcao, type VendaBalcao } from "../../api/pdvApi.js";
import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { descreverParte } from "./partesPagamentoView.js";

export const ROTULO_FORMA: Record<VendaBalcao["formaPagamento"], string> = {
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Crédito",
  CARTAO_DEBITO: "Débito",
  PIX: "Pix (maquininha)",
  PIX_QR: "Pix (QR na tela)",
  DIVIDIDO: "Dividido",
};

const SITUACAO_VENDA: Record<VendaBalcao["statusPagamento"], { rotulo: string; modificador: string }> = {
  AGUARDANDO: { rotulo: "Aguardando pagamento", modificador: "pendente" },
  RECUSADO: { rotulo: "Cartão recusado", modificador: "vencida" },
  APROVADO: { rotulo: "Concluída", modificador: "concluido" },
  ESTORNADO: { rotulo: "Cancelada", modificador: "cancelado" },
};

export async function carregarUltimasVendas(area: HTMLElement): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando vendas do caixa..."));
  try {
    renderizar(area, await listarUltimasVendasDoBalcao());
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar as vendas do caixa.", "erro"));
  }
}

function renderizar(area: HTMLElement, vendas: VendaBalcao[]): void {
  if (vendas.length === 0) {
    area.replaceChildren(cartaoEstado("Nenhuma venda no caixa ainda."));
    return;
  }
  const erro = criarMensagemErro();
  const linhas = vendas.map((venda) => criarLinha(
    celula(new Date(venda.criadaEm).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })),
    celula(venda.itens.map((item) => `${item.quantidade}× ${item.descricao}`).join(", ")),
    celula(formatarMoeda(venda.total)),
    celula(ROTULO_FORMA[venda.formaPagamento]),
    celulaSelo(SITUACAO_VENDA[venda.statusPagamento].rotulo, SITUACAO_VENDA[venda.statusPagamento].modificador),
    celulaComConteudo(criarAcoes(venda, area, erro))
  ));
  area.replaceChildren(erro, criarTabela(["Quando", "Itens", "Total", "Pagamento", "Situação", ""], linhas, "venda(s) recentes"));
}

function criarAcoes(venda: VendaBalcao, area: HTMLElement, erro: HTMLElement): HTMLElement {
  const acoes = document.createElement("div");
  acoes.className = "barra-acoes";

  const detalhe = document.createElement("button");
  detalhe.type = "button";
  detalhe.className = "link-tabela";
  detalhe.textContent = `#${venda.pedidoId.slice(0, 8)}`;
  detalhe.addEventListener("click", () => navegarPara("pedido-detalhe", venda.pedidoId));
  acoes.append(detalhe);

  if (venda.statusPagamento !== "ESTORNADO" && possui("PDV_CANCELAR")) {
    const cancelar = document.createElement("button");
    cancelar.type = "button";
    cancelar.className = "btn btn-perigo btn-pequeno";
    cancelar.textContent = "Cancelar";
    cancelar.addEventListener("click", () => {
      if (!window.confirm(mensagemCancelamento(venda))) {
        return;
      }
      cancelar.disabled = true;
      cancelarVendaDoBalcao(venda.pedidoId)
        .then(() => carregarUltimasVendas(area))
        .catch((falha: unknown) => {
          mostrarErro(erro, falha, "Não foi possível cancelar a venda.");
          cancelar.disabled = false;
        });
    });
    acoes.append(cancelar);
  }
  return acoes;
}

function mensagemCancelamento(venda: VendaBalcao): string {
  const devolucao = mensagemDevolucao(venda);
  return `Cancelar esta venda? Os produtos voltam ao estoque. ${devolucao}`;
}

function mensagemDevolucao(venda: VendaBalcao): string {
  const valor = formatarMoeda(venda.total);
  if (venda.formaPagamento === "PIX_QR") {
    return venda.statusPagamento === "APROVADO"
      ? `O Pix de ${valor} é devolvido automaticamente pelo Mercado Pago.`
      : "O Pix ainda não pago é cancelado no Mercado Pago.";
  }
  if (venda.formaPagamento === "DINHEIRO") {
    return `Devolva ${valor} em dinheiro ao cliente.`;
  }
  if (venda.formaPagamento === "DIVIDIDO") {
    return `Devolva cada parte: ${venda.pagamentos
      .map((parte) => `${descreverParte(parte)} ${formatarMoeda(parte.valor)}`).join("; ")}.`
      + " Dinheiro volta da gaveta; cartão e Pix são estornados na maquininha.";
  }
  return `Faça também o estorno de ${valor} na maquininha (ela ainda não é integrada).`;
}
