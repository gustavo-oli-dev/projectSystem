import { buscarCliente, type Cliente } from "../../api/clientesApi.js";
import { criarCobranca, listarCobrancasDoPedido, type Cobranca, type MeioCobranca } from "../../api/cobrancasApi.js";
import {
  gerarDocumentosFiscais,
  listarDocumentosDoPedido,
  ROTULO_TIPO_DOCUMENTO,
  type DocumentoFiscal,
} from "../../api/fiscalApi.js";
import { buscarPedido, cancelarPedido, confirmarPedido, reembolsarPedido, type Pedido } from "../../api/pedidosApi.js";
import { buscarVendaDoBalcao, type VendaBalcao } from "../../api/pdvApi.js";
import type { Permissao } from "../../api/sessaoApi.js";
import { navegarPara } from "../../router.js";
import { lembrarPedidoEmDestaque } from "../../state/destaquePedidoState.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela, formatarDataCurta } from "../tabela.js";
import { descreverParte } from "../pdv/partesPagamentoView.js";
import { ROTULO_FORMA } from "../pdv/ultimasVendasView.js";

const ROTULO_SITUACAO_PAGAMENTO: Record<VendaBalcao["statusPagamento"], string> = {
  AGUARDANDO: "Aguardando pagamento",
  RECUSADO: "Cartão recusado",
  APROVADO: "Aprovado",
  ESTORNADO: "Estornado / cancelado",
};

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
  /** Só na venda de balcão: como foi paga no caixa. */
  vendaBalcao: VendaBalcao | null;
}

function seTiverPermissao<T>(permissao: Permissao, carregar: () => Promise<T>): Promise<T | null> {
  return possui(permissao) ? carregar() : Promise.resolve(null);
}

export async function montarDetalhePedido(container: HTMLElement, pedidoId: string | null): Promise<void> {
  if (pedidoId === null) {
    navegarPara("pedidos");
    return;
  }

  lembrarPedidoEmDestaque(pedidoId);
  const voltar = criarLinkVoltar();
  container.replaceChildren(voltar, elementoCarregando("Carregando pedido..."));

  try {
    const pedido = await buscarPedido(pedidoId);
    const [cliente, cobrancas, documentos, vendaBalcao] = await Promise.all([
      pedido.clienteId === null
        ? Promise.resolve(null)
        : seTiverPermissao("CLIENTES_VER", () => buscarCliente(pedido.clienteId ?? "")),
      seTiverPermissao("COBRANCAS_VER", () => listarCobrancasDoPedido(pedidoId)),
      seTiverPermissao("FISCAL_VER", () => listarDocumentosDoPedido(pedidoId)),
      pedido.canal === "BALCAO" ? buscarVendaDoBalcao(pedidoId) : Promise.resolve(null),
    ]);
    renderizar(container, { pedido, cliente, cobrancas, documentos, vendaBalcao });
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

  const identificacao = document.createElement("div");
  identificacao.className = "pedido-identificacao";
  identificacao.append(tituloComStatus, criarLinhaPartes(dados));

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(identificacao, criarAcoes(dados, erroAcao, recarregar));

  const principal = document.createElement("div");
  principal.className = "detalhe-principal";
  principal.append(criarSecao("Itens", criarTabelaItens(pedido)));
  // Venda de balcão é paga no caixa: "Recebimentos" (online) só aparece se houver alguma (Pix com QR).
  const mostrarCobrancas = dados.cobrancas !== null
    && (pedido.canal !== "BALCAO" || dados.cobrancas.length > 0);
  if (mostrarCobrancas && dados.cobrancas !== null) {
    principal.append(criarSecao("Recebimentos", criarTabelaCobrancas(dados.cobrancas)));
  }
  if (dados.documentos !== null) {
    principal.append(criarSecao("Documentos fiscais", criarTabelaDocumentos(dados.documentos)));
  }

  const lateral = document.createElement("aside");
  lateral.className = "detalhe-lateral";
  if (dados.cliente !== null) {
    lateral.append(criarCartaoCliente(dados.cliente));
  }
  if (pedido.canal === "BALCAO") {
    lateral.append(criarCartaoLateral("Venda no balcão", [["CPF na nota", pedido.cpfNaNota ?? "—"]]));
  }
  if (dados.vendaBalcao !== null) {
    lateral.append(criarCartaoPagamentoCaixa(dados.vendaBalcao));
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
      executar("Cancelar pedido", "btn-perigo", () => confirmarECancelar(pedido.id)),
      executar("Confirmar pedido", "btn-primary", () => confirmarPedido(pedido.id))
    );
  }

  // Venda de balcão: cancelar pelo Caixa (estorna o pagamento); não tem cobrança online.
  if (pedido.canal === "BALCAO") {
    if (dados.documentos !== null && dados.documentos.length === 0 && possui("FISCAL_GERENCIAR")) {
      acoes.append(executar("Gerar documentos fiscais", "btn-primary", () => gerarDocumentosFiscais(pedido.id)));
    }
    return acoes;
  }

  if (pedido.status === "AGUARDANDO_EMISSAO") {
    const pago = dados.cobrancas !== null && dados.cobrancas.some((cobranca) => cobranca.status === "PAGA");
    if (pago && gerenciaPedidos && possui("COBRANCAS_GERENCIAR")) {
      acoes.append(executar(`Reembolsar ${formatarMoeda(pedido.valorTotal)}`, "btn-perigo",
        () => confirmarEReembolsar(pedido)));
    }
    if (!pago && gerenciaPedidos) {
      acoes.append(executar("Cancelar pedido", "btn-perigo", () => confirmarECancelar(pedido.id)));
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
    return cartaoEstado("Nenhum recebimento para este pedido.");
  }
  const linhas = cobrancas.map((cobranca) =>
    criarLinha(
      celula(cobranca.meio === "PIX" ? "Pix" : "Boleto"),
      celula(formatarMoeda(cobranca.valor)),
      celulaSelo(ROTULO_STATUS_COBRANCA[cobranca.status] ?? cobranca.status, cobranca.status.toLowerCase()),
      celula(formatarDataCurta(cobranca.criadoEm))
    )
  );
  return criarTabela(["Meio", "Valor", "Status", "Criada em"], linhas, "recebimento(s)");
}

function criarTabelaDocumentos(documentos: DocumentoFiscal[]): HTMLElement {
  if (documentos.length === 0) {
    return cartaoEstado("Nenhum documento fiscal gerado para este pedido.");
  }
  const linhas = documentos.map((documento) =>
    criarLinha(
      celula(ROTULO_TIPO_DOCUMENTO[documento.tipo]),
      celulaSelo(ROTULO_STATUS_DOCUMENTO[documento.status] ?? documento.status, documento.status.toLowerCase()),
      celula(documento.protocolo ?? "—"),
      celula(formatarDataCurta(documento.atualizadoEm))
    )
  );
  return criarTabela(["Tipo", "Status", "Protocolo", "Atualizado em"], linhas, "documento(s)");
}

function criarCartaoPagamentoCaixa(venda: VendaBalcao): HTMLElement {
  const linhas: Array<[string, string]> = [
    ["Forma", ROTULO_FORMA[venda.formaPagamento]],
    ["Situação", ROTULO_SITUACAO_PAGAMENTO[venda.statusPagamento]],
  ];
  // Promoção e desconto do gerente (com quem autorizou) ficam no histórico da venda.
  if (venda.descontoPromocao > 0) {
    linhas.push(["Promoções", `− ${formatarMoeda(venda.descontoPromocao)}`]);
  }
  if (venda.desconto > 0) {
    linhas.push(["Desconto", `− ${formatarMoeda(venda.desconto)}`]);
    linhas.push(["Autorizado por", venda.descontoAutorizadoPor ?? "—"]);
  }
  if (venda.formaPagamento === "DIVIDIDO") {
    venda.pagamentos.forEach((parte) => linhas.push([descreverParte(parte), formatarMoeda(parte.valor)]));
    if (venda.troco !== null) {
      linhas.push(["Troco", formatarMoeda(venda.troco)]);
    }
    return criarCartaoLateral("Pagamento no caixa", comVendedor(linhas, venda));
  }
  if (venda.bandeira !== null) {
    linhas.push(["Bandeira", venda.bandeira.replace("_", " ")]);
  }
  if (venda.codigoAutorizacao !== null) {
    linhas.push(["Autorização", venda.codigoAutorizacao]);
  }
  if (venda.formaPagamento !== "DINHEIRO" && venda.formaPagamento !== "PIX_QR") {
    linhas.push(["Maquininha", venda.maquininhaIntegrada ? "Integrada" : "Avulsa (contingência)"]);
  }
  if (venda.valorRecebido !== null) {
    linhas.push(["Recebido", formatarMoeda(venda.valorRecebido)]);
  }
  if (venda.troco !== null) {
    linhas.push(["Troco", formatarMoeda(venda.troco)]);
  }
  return criarCartaoLateral("Pagamento no caixa", comVendedor(linhas, venda));
}

function comVendedor(linhas: Array<[string, string]>, venda: VendaBalcao): Array<[string, string]> {
  const vendedor = nomeDeQuemVendeu(venda);
  return vendedor === null ? linhas : [...linhas, ["Vendido por", vendedor]];
}

/** "Cliente: Ana Lima · Vendido por: Dono" — logo abaixo do número, para bater o olho. */
function criarLinhaPartes(dados: DadosPedido): HTMLElement {
  const linha = document.createElement("p");
  linha.className = "pedido-partes";
  linha.append(criarParte("Cliente", descreverCliente(dados)));
  const vendedor = dados.vendaBalcao === null ? null : nomeDeQuemVendeu(dados.vendaBalcao);
  if (vendedor !== null) {
    linha.append(criarParte("Vendido por", vendedor));
  }
  return linha;
}

function criarParte(rotulo: string, valor: string): HTMLElement {
  const termo = document.createElement("span");
  termo.className = "pedido-partes__rotulo";
  termo.textContent = `${rotulo}:`;
  const conteudo = document.createElement("strong");
  conteudo.textContent = valor;
  const parte = document.createElement("span");
  parte.className = "pedido-partes__item";
  parte.append(termo, conteudo);
  return parte;
}

function descreverCliente(dados: DadosPedido): string {
  if (dados.cliente !== null) {
    return dados.cliente.nome;
  }
  // Tem cliente, mas o perfil de acesso não pode ver clientes: não dá para mostrar o nome.
  return dados.pedido.clienteId === null ? "Consumidor não identificado" : "Cliente cadastrado";
}

function nomeDeQuemVendeu(venda: VendaBalcao): string | null {
  return venda.operadorNome ?? venda.operador;
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
  botao.className = "btn btn-ghost btn-pequeno botao-voltar";
  botao.textContent = "← Voltar para Pedidos";
  botao.addEventListener("click", () => navegarPara("pedidos"));
  return botao;
}
