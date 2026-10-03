import {
  consultarConfiguracaoFiscal,
  gerarDocumentosFiscais,
  listarDocumentosFiscais,
  ROTULO_TIPO_DOCUMENTO,
  type ConfiguracaoFiscal,
  type DocumentoFiscal,
  type StatusDocumentoFiscal,
} from "../../api/fiscalApi.js";
import { listarPedidos, type Pedido } from "../../api/pedidosApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import {
  celula,
  celulaComConteudo,
  celulaSelo,
  criarLinha,
  criarTabela,
  formatarDataCurta,
} from "../tabela.js";

const ROTULO_STATUS: Record<StatusDocumentoFiscal, string> = {
  PENDENTE: "Pendente",
  AUTORIZADO: "Autorizado",
  REJEITADO: "Rejeitado",
  CANCELADO: "Cancelado",
};
const STATUS_EXIBIDOS: readonly StatusDocumentoFiscal[] = ["PENDENTE", "AUTORIZADO", "REJEITADO", "CANCELADO"];

export async function montarFiscal(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Fiscal · SEFAZ";

  const subtitulo = document.createElement("p");
  subtitulo.className = "subtitulo-pagina";
  subtitulo.textContent = "NF-e (produtos, SEFAZ-CE) e NFS-e (serviços, Prefeitura de Fortaleza) gerados a partir dos pedidos confirmados.";

  const conteudo = document.createElement("div");
  container.replaceChildren(titulo, subtitulo, conteudo);

  await carregar(conteudo);
}

async function carregar(conteudo: HTMLElement): Promise<void> {
  conteudo.replaceChildren(elementoCarregando("Carregando dados fiscais..."));

  try {
    const [configuracao, documentos, pedidos] = await Promise.all([
      consultarConfiguracaoFiscal(),
      listarDocumentosFiscais(),
      possui("PEDIDOS_VER") ? listarPedidos() : Promise.resolve<Pedido[]>([]),
    ]);
    renderizar(conteudo, configuracao, documentos, pedidos);
  } catch {
    conteudo.replaceChildren(cartaoEstado("Não foi possível carregar os dados fiscais.", "erro"));
  }
}

function renderizar(
  conteudo: HTMLElement,
  configuracao: ConfiguracaoFiscal,
  documentos: DocumentoFiscal[],
  pedidos: Pedido[]
): void {
  const pedidosComDocumento = new Set(documentos.map((documento) => documento.pedidoId));
  const pedidosAguardando = pedidos.filter(
    (pedido) => pedido.status === "AGUARDANDO_EMISSAO" && !pedidosComDocumento.has(pedido.id)
  );

  conteudo.replaceChildren(
    criarPainelConfiguracao(configuracao),
    criarGradeStatus(documentos),
    criarSecao("Pedidos aguardando geração de documentos", criarTabelaAguardando(pedidosAguardando, conteudo)),
    criarSecao("Documentos fiscais", criarTabelaDocumentos(documentos))
  );
}

function criarPainelConfiguracao(configuracao: ConfiguracaoFiscal): HTMLElement {
  const painel = document.createElement("div");
  painel.className = configuracao.emissaoHabilitada ? "aviso-fiscal aviso-fiscal--ok" : "aviso-fiscal";

  const cabecalho = document.createElement("div");
  cabecalho.className = "aviso-fiscal__cabecalho";

  const tituloAviso = document.createElement("strong");
  tituloAviso.textContent = configuracao.emissaoHabilitada
    ? "Transmissão habilitada"
    : "Transmissão à SEFAZ/Prefeitura desabilitada";

  const ambiente = document.createElement("span");
  ambiente.className = "aviso-fiscal__ambiente";
  ambiente.textContent = `Ambiente: ${configuracao.ambiente}`;

  cabecalho.append(tituloAviso, ambiente);

  const explicacao = document.createElement("p");
  explicacao.className = "aviso-fiscal__texto";
  explicacao.textContent = configuracao.emissaoHabilitada
    ? "Certificado, regime tributário e provedor de NFS-e configurados."
    : "Os documentos são gerados e ficam como Pendente até estes itens serem configurados:";

  const requisitos = document.createElement("ul");
  requisitos.className = "aviso-fiscal__requisitos";
  requisitos.append(
    criarRequisito("Certificado digital A1", configuracao.certificadoA1Configurado),
    criarRequisito("Regime tributário da empresa", configuracao.regimeTributarioDefinido),
    criarRequisito("Provedor de NFS-e (Focus NFe)", configuracao.provedorNfseConfigurado)
  );

  painel.append(cabecalho, explicacao, requisitos);
  return painel;
}

function criarRequisito(rotulo: string, atendido: boolean): HTMLLIElement {
  const item = document.createElement("li");
  item.className = atendido ? "requisito requisito--ok" : "requisito";

  const marcador = document.createElement("span");
  marcador.className = "requisito__marcador";
  marcador.textContent = atendido ? "✓" : "✕";

  const texto = document.createElement("span");
  texto.textContent = rotulo;

  item.append(marcador, texto);
  return item;
}

function criarGradeStatus(documentos: DocumentoFiscal[]): HTMLElement {
  const grade = document.createElement("div");
  grade.className = "grade-widgets grade-widgets--compacta";

  for (const status of STATUS_EXIBIDOS) {
    const quantidade = documentos.filter((documento) => documento.status === status).length;

    const card = document.createElement("div");
    card.className = "widget widget--compacto";

    const rotulo = document.createElement("p");
    rotulo.className = "widget__rotulo";
    rotulo.textContent = ROTULO_STATUS[status];

    const valor = document.createElement("p");
    valor.className = "widget__valor";
    valor.textContent = String(quantidade);

    card.append(rotulo, valor);
    grade.append(card);
  }
  return grade;
}

function criarTabelaAguardando(pedidos: Pedido[], conteudo: HTMLElement): HTMLElement {
  if (pedidos.length === 0) {
    return cartaoEstado("Nenhum pedido confirmado aguardando documentos fiscais.");
  }

  const linhas = pedidos.map((pedido) => {
    const tiposNecessarios = [...new Set(pedido.itens.map((item) => (item.tipo === "PRODUTO" ? "NF-e" : "NFS-e")))];

    return criarLinha(
      celula(pedido.id.slice(0, 8)),
      celula(tiposNecessarios.join(" + ")),
      celula(formatarMoeda(pedido.valorTotal)),
      celula(formatarDataCurta(pedido.criadoEm)),
      possui("FISCAL_GERENCIAR") ? celulaComConteudo(criarBotaoGerar(pedido.id, conteudo)) : celula("")
    );
  });

  return criarTabela(["Pedido", "Documentos exigidos", "Total", "Criado em", ""], linhas, "pedido(s)");
}

function criarBotaoGerar(pedidoId: string, conteudo: HTMLElement): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-primary btn-pequeno";
  botao.textContent = "Gerar documentos";
  botao.addEventListener("click", () => void gerar(pedidoId, botao, conteudo));
  return botao;
}

async function gerar(pedidoId: string, botao: HTMLButtonElement, conteudo: HTMLElement): Promise<void> {
  botao.disabled = true;
  try {
    await gerarDocumentosFiscais(pedidoId);
    await carregar(conteudo);
  } catch {
    botao.disabled = false;
    botao.textContent = "Falhou — tentar de novo";
  }
}

function criarTabelaDocumentos(documentos: DocumentoFiscal[]): HTMLElement {
  if (documentos.length === 0) {
    return cartaoEstado("Nenhum documento fiscal gerado ainda.");
  }

  const linhas = documentos.map((documento) =>
    criarLinha(
      celula(ROTULO_TIPO_DOCUMENTO[documento.tipo]),
      celula(documento.pedidoId.slice(0, 8)),
      celulaSelo(ROTULO_STATUS[documento.status], documento.status.toLowerCase()),
      celula(documento.protocolo ?? "—"),
      celula(documento.motivoRejeicao ?? "—"),
      celula(formatarDataCurta(documento.atualizadoEm))
    )
  );

  return criarTabela(
    ["Tipo", "Pedido", "Status", "Protocolo", "Motivo da rejeição", "Atualizado em"],
    linhas,
    "documento(s)"
  );
}

function criarSecao(tituloTexto: string, corpo: HTMLElement): HTMLElement {
  const secao = document.createElement("section");
  secao.className = "secao-painel";

  const tituloSecao = document.createElement("h2");
  tituloSecao.className = "secao-painel__titulo";
  tituloSecao.textContent = tituloTexto;

  secao.append(tituloSecao, corpo);
  return secao;
}
