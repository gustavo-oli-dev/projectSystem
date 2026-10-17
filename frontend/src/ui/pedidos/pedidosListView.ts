import { listarPedidos, type Pedido } from "../../api/pedidosApi.js";
import { navegarPara } from "../../router.js";
import { consumirPedidoEmDestaque } from "../../state/destaquePedidoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela, formatarDataCurta } from "../tabela.js";

/** Quantos pedidos a tela traz por vez (o mesmo passo do "Ver pedidos mais antigos"). */
const PEDIDOS_POR_VEZ = 50;
/** Teto do backend: acima disso, o histórico fica nos relatórios do Painel. */
const LIMITE_MAXIMO = 1000;

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
  await carregar(areaLista, PEDIDOS_POR_VEZ, consumirPedidoEmDestaque());
}

async function carregar(areaLista: HTMLElement, limite: number, pedidoEmDestaque: string | null): Promise<void> {
  areaLista.replaceChildren(elementoCarregando("Carregando pedidos..."));
  try {
    const pedidos = await listarPedidos(limite);
    areaLista.replaceChildren(renderizar(pedidos, pedidoEmDestaque), criarRodapeLista(areaLista, pedidos.length, limite));
    rolarAteDestaque(areaLista);
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os pedidos.", "erro"));
  }
}

/** Avisa que só os mais recentes estão na tela e oferece trazer mais. */
function criarRodapeLista(areaLista: HTMLElement, quantidade: number, limite: number): HTMLElement {
  const rodape = document.createElement("div");
  rodape.className = "rodape-lista";
  if (quantidade < limite) {
    return rodape;
  }

  const aviso = document.createElement("p");
  aviso.className = "rodape-lista__aviso";
  rodape.append(aviso);

  if (limite >= LIMITE_MAXIMO) {
    aviso.textContent = `Mostrando os ${quantidade} pedidos mais recentes. Para vendas mais antigas, use os relatórios do Painel.`;
    return rodape;
  }

  aviso.textContent = `Mostrando os ${quantidade} pedidos mais recentes.`;
  const verMais = document.createElement("button");
  verMais.type = "button";
  verMais.className = "btn btn-ghost";
  verMais.textContent = "Ver pedidos mais antigos";
  verMais.addEventListener("click", () => void carregar(areaLista, Math.min(limite + PEDIDOS_POR_VEZ, LIMITE_MAXIMO), null));
  rodape.append(verMais);
  return rodape;
}

function renderizar(pedidos: Pedido[], pedidoEmDestaque: string | null): HTMLElement {
  if (pedidos.length === 0) {
    return cartaoEstado("Nenhum pedido criado ainda.");
  }

  const linhas = pedidos.map((pedido) => {
    const linha = criarLinha(
      celula(pedido.id.slice(0, 8)),
      celula(descreverCliente(pedido)),
      celula(ROTULO_CANAL[pedido.canal]),
      celulaProdutos(pedido),
      celulaSelo(ROTULOS_STATUS[pedido.status] ?? pedido.status, pedido.status.toLowerCase()),
      celula(formatarMoeda(pedido.valorTotal)),
      celula(formatarDataCurta(pedido.criadoEm))
    );
    linha.className = "linha-clicavel";
    linha.tabIndex = 0;
    if (pedido.id === pedidoEmDestaque) {
      linha.classList.add("linha-destaque");
      linha.setAttribute("aria-current", "true");
    }
    linha.addEventListener("click", () => navegarPara("pedido-detalhe", pedido.id));
    linha.addEventListener("keydown", (evento) => {
      if (evento.key === "Enter") {
        navegarPara("pedido-detalhe", pedido.id);
      }
    });
    return linha;
  });

  return criarTabela(["Pedido", "Cliente", "Canal", "Produtos", "Status", "Total", "Criado em"], linhas, "pedido(s)");
}

/** Leva a linha destacada para o meio da tela e põe o foco nela (Enter abre de novo). */
function rolarAteDestaque(area: HTMLElement): void {
  const destacada = area.querySelector<HTMLElement>(".linha-destaque");
  if (destacada === null) {
    return;
  }
  destacada.scrollIntoView({ block: "center" });
  destacada.focus({ preventScroll: true });
}

const ROTULO_CANAL: Record<Pedido["canal"], string> = {
  PAINEL: "Painel",
  BALCAO: "Caixa",
};

/** Venda de balcão sem cliente: mostra o CPF na nota, se houver. */
function descreverCliente(pedido: Pedido): string {
  if (pedido.clienteNome !== null) {
    return pedido.clienteNome;
  }
  return pedido.cpfNaNota === null ? "Consumidor" : `CPF ${pedido.cpfNaNota}`;
}

/**
 * Todos os produtos do pedido numa linha só (cortada com reticências na tela). O texto inteiro fica
 * na célula, então a busca da tabela acha a venda pelo nome de qualquer produto.
 */
function celulaProdutos(pedido: Pedido): HTMLTableCellElement {
  const texto = pedido.itens.map((item) => `${item.quantidade}× ${item.descricao}`).join(", ");
  const elemento = celula(texto);
  elemento.className = "celula-produtos";
  elemento.title = texto;
  return elemento;
}
