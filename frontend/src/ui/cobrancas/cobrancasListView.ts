import { listarCobrancas, type Cobranca } from "../../api/cobrancasApi.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela, formatarDataCurta } from "../tabela.js";

const ROTULO_STATUS: Record<string, string> = {
  PENDENTE: "Pendente",
  PAGA: "Paga",
  VENCIDA: "Vencida",
  CANCELADA: "Cancelada",
  REEMBOLSADA: "Reembolsada",
};
const ROTULO_MEIO: Record<string, string> = { PIX: "Pix", BOLETO: "Boleto" };

export async function montarListaCobrancas(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Cobranças";

  const areaLista = document.createElement("div");
  container.replaceChildren(titulo, areaLista);
  areaLista.append(elementoCarregando("Carregando cobranças..."));

  try {
    const cobrancas = await listarCobrancas();
    areaLista.replaceChildren(renderizar(cobrancas));
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar as cobranças.", "erro"));
  }
}

function renderizar(cobrancas: Cobranca[]): HTMLElement {
  if (cobrancas.length === 0) {
    return cartaoEstado("Nenhuma cobrança gerada ainda.");
  }

  const linhas = cobrancas.map((cobranca) =>
    criarLinha(
      celula(cobranca.pedidoId.slice(0, 8)),
      celula(ROTULO_MEIO[cobranca.meio] ?? cobranca.meio),
      celula(formatarMoeda(cobranca.valor)),
      celulaSelo(ROTULO_STATUS[cobranca.status] ?? cobranca.status, cobranca.status.toLowerCase()),
      celula(formatarDataCurta(cobranca.criadoEm))
    )
  );

  return criarTabela(["Pedido", "Meio", "Valor", "Status", "Criada em"], linhas, "cobrança(s)");
}
