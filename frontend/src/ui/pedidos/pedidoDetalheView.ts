import { buscarCliente, type Cliente } from "../../api/clientesApi.js";
import { criarCobranca, listarCobrancasDoPedido, type Cobranca, type MeioCobranca } from "../../api/cobrancasApi.js";
import { gerarDocumentosFiscais, listarDocumentosDoPedido, type DocumentoFiscal } from "../../api/fiscalApi.js";
import { buscarPedido, cancelarPedido, confirmarPedido, reembolsarPedido, type Pedido } from "../../api/pedidosApi.js";
import type { Permissao } from "../../api/sessaoApi.js";
import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela, formatarDataCurta } from "../tabela.js";

const ROTULO_STATUS_PEDIDO: Record<string, string> = {
  ABERTO: "Aberto",
  AGUARDANDO_EMISSAO: "Aguardando emissão",
  CONCLUIDO: "Concluído",
  CANCELADO: "Cancelado",
};
const ROTULO_STATUS_COBRANCA: Record<string, string> = {
  PENDENTE: "Pendente",
  PAGA: "Paga",
  VENCIDA: "Vencida",
  CANCELADA: "Cancelada",
  REEMBOLSADA: "Reembolsada",
};
const ROTULO_STATUS_DOCUMENTO: Record<string, string> = {
  PENDENTE: "Pendente",
  AUTORIZADO: "Autorizado",
  REJEITADO: "Rejeitado",
  CANCELADO: "Cancelado",
};

/** null = o cargo não enxerga aquela informação; a seção correspondente não aparece. */
interface DadosPedido {
  pedido: Pedido;
  cliente: Cliente | null;
  cobrancas: Cobranca[] | null;
  documentos: DocumentoFiscal[] | null;
}

function seTiverPermissao<T>(permissao: Permissao, carregar: () => Promise<T>): Promise<T | null> {
  return possui(permissao) ? carregar() : Promise.resolve(null);
}

export async function montarDetalhePedido(container: HTMLElement, pedidoId: string | null): Promise<void> {
  if (pedidoId === null) {
    navegarPara("pedidos");
    return;
  }

  const voltar = criarLinkVoltar();
  container.replaceChildren(voltar, elementoCarregando("Carregando pedido..."));

  try {
    const pedido = await buscarPedido(pedidoId);
    const [cliente, cobrancas, documentos] = await Promise.all([
      seTiverPermissao("CLIENTES_VER", () => buscarCliente(pedido.clienteId)),
      seTiverPermissao("COBRANCAS_VER", () => listarCobrancasDoPedido(pedidoId)),
      seTiverPermissao("FISCAL_VER", () => listarDocumentosDoPedido(pedidoId)),
    ]);
    renderizar(container, { pedido, cliente, cobrancas, documentos });
  } catch {
    container.replaceChildren(voltar, cartaoEstado("Não foi possível carregar este pedido.", "erro"));
  }
}

function renderizar(container: HTMLElement, dados: DadosPedido): void {
  const { pedido } = dados;

  const titulo = document.createElement("h1");
  titulo.textContent = `Pedido #${pedido.id.slice(0, 8)}`;

  const selo = document.createElement("span");
  selo.className = `selo selo--${pedido.status.toLowerCase()}`;
  selo.textContent = ROTULO_STATUS_PEDIDO[pedido.status] ?? pedido.status;

  const tituloComStatus = document.createElement("div");
  tituloComStatus.className = "titulo-com-status";
  tituloComStatus.append(titulo, selo);

  const erroAcao = document.createElement("p");
  erroAcao.className = "aviso-erro";
  erroAcao.setAttribute("role", "alert");
  erroAcao.hidden = true;

  const recarregar = (): Promise<void> => montarDetalhePedido(container, pedido.id);

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(tituloComStatus, criarAcoes(dados, erroAcao, recarregar));

  const principal = document.createElement("div");
  principal.className = "detalhe-principal";
  principal.append(criarSecao("Itens", criarTabelaItens(pedido)));
  if (dados.cobrancas !== null) {
    principal.append(criarSecao("Cobranças", criarTabelaCobrancas(dados.cobrancas)));
  }
  if (dados.documentos !== null) {
    principal.append(criarSecao("Documentos fiscais", criarTabelaDocumentos(dados.documentos)));
  }

  const lateral = document.createElement("aside");
  lateral.className = "detalhe-lateral";
  if (dados.cliente !== null) {
    lateral.append(criarCartaoCliente(dados.cliente));
  }
  lateral.append(criarCartaoResumo(pedido));

  const grade = document.createElement("div");
  grade.className = "detalhe-grade";
  grade.append(principal, lateral);

  container.replaceChildren(criarLinkVoltar(), cabecalho, erroAcao, grade);
}

function criarAcoes(dados: DadosPedido, erroAcao: HTMLElement, recarregar: () => Promise<void>): HTMLElement {
  const { pedido } = dados;
  const acoes = document.createElement("div");
  acoes.className = "barra-acoes";

  const executar = (rotulo: string, classe: string, acao: () => Promise<unknown>): HTMLButtonElement => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = `btn btn-pequeno ${classe}`;
    botao.textContent = rotulo;
    botao.addEventListener("click", () => void rodarAcao(botao, erroAcao, acao, recarregar));
    return botao;
  };

  const gerenciaPedidos = possui("PEDIDOS_GERENCIAR");

  if (pedido.status === "ABERTO" && gerenciaPedidos) {
    acoes.append(
      executar("Cancelar pedido", "btn-ghost", () => confirmarECancelar(pedido.id)),
      executar("Confirmar pedido", "btn-primary", () => confirmarPedido(pedido.id))
    );
  }

  if (pedido.status === "AGUARDANDO_EMISSAO") {
    const pago = dados.cobrancas !== null && dados.cobrancas.some((cobranca) => cobranca.status === "PAGA");
    if (pago && gerenciaPedidos && possui("COBRANCAS_GERENCIAR")) {
      acoes.append(executar(`Reembolsar ${formatarMoeda(pedido.valorTotal)}`, "btn-ghost",
        () => confirmarEReembolsar(pedido)));
    }
    if (!pago && gerenciaPedidos) {
      acoes.append(executar("Cancelar pedido", "btn-ghost", () => confirmarECancelar(pedido.id)));
    }
    if (dados.cobrancas !== null && dados.cobrancas.length === 0 && possui("COBRANCAS_GERENCIAR")) {
      acoes.append(
        executar("Cobrar via boleto", "btn-outline", () => cobrar(pedido.id, "BOLETO")),
        executar("Cobrar via Pix", "btn-outline", () => cobrar(pedido.id, "PIX"))
      );
    }
    if (dados.documentos !== null && dados.documentos.length === 0 && possui("FISCAL_GERENCIAR")) {
      acoes.append(executar("Gerar documentos fiscais", "btn-primary", () => gerarDocumentosFiscais(pedido.id)));
    }
  }

  return acoes;
}

async function rodarAcao(
  botao: HTMLButtonElement,
  erroAcao: HTMLElement,
  acao: () => Promise<unknown>,
  recarregar: () => Promise<void>
): Promise<void> {
  botao.disabled = true;
  erroAcao.hidden = true;
  try {
    await acao();
    await recarregar();
  } catch (erro) {
    erroAcao.textContent = erro instanceof Error ? erro.message : "Não foi possível concluir a ação.";
    erroAcao.hidden = false;
    botao.disabled = false;
  }
}

function confirmarECancelar(pedidoId: string): Promise<unknown> {
  if (!window.confirm("Cancelar este pedido? Os produtos voltam ao estoque. Esta ação não pode ser desfeita.")) {
    return Promise.resolve();
  }
  return cancelarPedido(pedidoId);
}

function confirmarEReembolsar(pedido: Pedido): Promise<unknown> {
  const mensagem = `Devolver ${formatarMoeda(pedido.valorTotal)} ao cliente pelo Mercado Pago? `
    + "O pedido é cancelado e os produtos voltam ao estoque. Não pode ser desfeito.";
  if (!window.confirm(mensagem)) {
    return Promise.resolve();
  }
  return reembolsarPedido(pedido.id);
}

function cobrar(pedidoId: string, meio: MeioCobranca): Promise<unknown> {
  return criarCobranca(pedidoId, meio);
}

function criarTabelaItens(pedido: Pedido): HTMLElement {
  const linhas = pedido.itens.map((item) =>
    criarLinha(
      celula(item.descricao),
      celula(item.tipo === "PRODUTO" ? "Produto" : "Serviço"),
      celula(String(item.quantidade)),
      celula(formatarMoeda(item.precoUnitario)),
      celula(formatarMoeda(item.subtotal))
    )
  );
  return criarTabela(["Descrição", "Tipo", "Qtd.", "Preço unit.", "Subtotal"], linhas, "item(ns)");
}

function criarTabelaCobrancas(cobrancas: Cobranca[]): HTMLElement {
  if (cobrancas.length === 0) {
    return cartaoEstado("Nenhuma cobrança gerada para este pedido.");
  }
  const linhas = cobrancas.map((cobranca) =>
    criarLinha(
      celula(cobranca.meio === "PIX" ? "Pix" : "Boleto"),
      celula(formatarMoeda(cobranca.valor)),
      celulaSelo(ROTULO_STATUS_COBRANCA[cobranca.status] ?? cobranca.status, cobranca.status.toLowerCase()),
      celula(formatarDataCurta(cobranca.criadoEm))
    )
  );
  return criarTabela(["Meio", "Valor", "Status", "Criada em"], linhas, "cobrança(s)");
}

function criarTabelaDocumentos(documentos: DocumentoFiscal[]): HTMLElement {
  if (documentos.length === 0) {
    return cartaoEstado("Nenhum documento fiscal gerado para este pedido.");
  }
  const linhas = documentos.map((documento) =>
    criarLinha(
      celula(documento.tipo === "NFE" ? "NF-e" : "NFS-e"),
      celulaSelo(ROTULO_STATUS_DOCUMENTO[documento.status] ?? documento.status, documento.status.toLowerCase()),
      celula(documento.protocolo ?? "—"),
      celula(formatarDataCurta(documento.atualizadoEm))
    )
  );
  return criarTabela(["Tipo", "Status", "Protocolo", "Atualizado em"], linhas, "documento(s)");
}

function criarCartaoCliente(cliente: Cliente): HTMLElement {
  return criarCartaoLateral("Cliente", [
    ["Nome", cliente.nome],
    ["CPF/CNPJ", cliente.documento],
    ["WhatsApp", cliente.telefoneWhatsapp],
  ]);
}

function criarCartaoResumo(pedido: Pedido): HTMLElement {
  return criarCartaoLateral("Resumo", [
    ["Total", formatarMoeda(pedido.valorTotal)],
    ["Itens", String(pedido.itens.length)],
    ["Criado em", formatarDataCurta(pedido.criadoEm)],
  ]);
}

function criarCartaoLateral(tituloTexto: string, pares: Array<[string, string]>): HTMLElement {
  const titulo = document.createElement("p");
  titulo.className = "cartao-lateral__titulo";
  titulo.textContent = tituloTexto;

  const lista = document.createElement("dl");
  lista.className = "lista-dados";
  for (const [rotulo, valor] of pares) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    lista.append(termo, definicao);
  }

  const cartao = document.createElement("div");
  cartao.className = "cartao-lateral";
  cartao.append(titulo, lista);
  return cartao;
}

function criarSecao(tituloTexto: string, corpo: HTMLElement): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.className = "secao-painel__titulo";
  titulo.textContent = tituloTexto;

  const secao = document.createElement("section");
  secao.className = "secao-detalhe";
  secao.append(titulo, corpo);
  return secao;
}

function criarLinkVoltar(): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "link-voltar";
  botao.textContent = "← Pedidos";
  botao.addEventListener("click", () => navegarPara("pedidos"));
  return botao;
}
