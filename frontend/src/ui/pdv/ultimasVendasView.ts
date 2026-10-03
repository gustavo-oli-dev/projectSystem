import { cancelarVendaDoBalcao, listarUltimasVendasDoBalcao, type VendaBalcao } from "../../api/pdvApi.js";
import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";

export const ROTULO_FORMA: Record<VendaBalcao["formaPagamento"], string> = {
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Crédito",
  CARTAO_DEBITO: "Débito",
  PIX: "Pix",
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
    venda.statusPagamento === "ESTORNADO" ? celulaSelo("Cancelada", "cancelado") : celulaSelo("Concluída", "concluido"),
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

  if (venda.statusPagamento === "APROVADO" && possui("PDV_CANCELAR")) {
    const cancelar = document.createElement("button");
    cancelar.type = "button";
    cancelar.className = "btn btn-ghost btn-pequeno";
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
  const devolucao = venda.formaPagamento === "DINHEIRO"
    ? `Devolva ${formatarMoeda(venda.total)} em dinheiro ao cliente.`
    : `Faça também o estorno de ${formatarMoeda(venda.total)} na maquininha (ela ainda não é integrada).`;
  return `Cancelar esta venda? Os produtos voltam ao estoque. ${devolucao}`;
}
