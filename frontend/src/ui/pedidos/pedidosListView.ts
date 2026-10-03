import { listarPedidos, type Pedido } from "../../api/pedidosApi.js";
import { navegarPara } from "../../router.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela, formatarDataCurta } from "../tabela.js";

const ROTULOS_STATUS: Record<string, string> = {
  ABERTO: "Aberto",
  AGUARDANDO_EMISSAO: "Aguardando emissão",
  CONCLUIDO: "Concluído",
  CANCELADO: "Cancelado",
};

export async function montarListaPedidos(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Pedidos";

  const areaLista = document.createElement("div");
  container.replaceChildren(titulo, areaLista);
  areaLista.append(elementoCarregando("Carregando pedidos..."));

  try {
    const pedidos = await listarPedidos();
    areaLista.replaceChildren(renderizar(pedidos));
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os pedidos.", "erro"));
  }
}

function renderizar(pedidos: Pedido[]): HTMLElement {
  if (pedidos.length === 0) {
    return cartaoEstado("Nenhum pedido criado ainda.");
  }

  const linhas = pedidos.map((pedido) => {
    const linha = criarLinha(
      celula(pedido.id.slice(0, 8)),
      celulaSelo(ROTULOS_STATUS[pedido.status] ?? pedido.status, pedido.status.toLowerCase()),
      celula(String(pedido.itens.length)),
      celula(formatarMoeda(pedido.valorTotal)),
      celula(formatarDataCurta(pedido.criadoEm))
    );
    linha.className = "linha-clicavel";
    linha.tabIndex = 0;
    linha.addEventListener("click", () => navegarPara("pedido-detalhe", pedido.id));
    linha.addEventListener("keydown", (evento) => {
      if (evento.key === "Enter") {
        navegarPara("pedido-detalhe", pedido.id);
      }
    });
    return linha;
  });

  return criarTabela(["Pedido", "Status", "Itens", "Total", "Criado em"], linhas, "pedido(s)");
}
