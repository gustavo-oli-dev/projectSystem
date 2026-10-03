import { consultarCobrancasPendentes, consultarPedidosPorStatus } from "../../api/painelApi.js";
import { listarProdutos } from "../../api/produtosApi.js";
import {
  baixarCsvVendas,
  gerarRelatorioVendas,
  type ExportacaoVendas,
  type Periodo,
  type RelatorioVendas,
} from "../../api/relatoriosApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { criarGraficoRosca, type CorFatia, type Fatia } from "../graficos/graficoRosca.js";
import { situacaoEstoque } from "../produtos/situacaoEstoque.js";
import { criarIndicadores } from "./indicadoresView.js";
import { ATALHOS, periodoDoAtalho, type AtalhoPeriodo } from "./periodoPainel.js";
import {
  criarSecaoCanais,
  criarSecaoCustoLucro,
  criarSecaoDiasDaSemana,
  criarSecaoFaturamento,
  criarSecaoFormasPagamento,
  criarSecaoHorarios,
  criarSecaoMaisVendidos,
} from "./secoesRelatorio.js";

const ATALHO_INICIAL: AtalhoPeriodo = "TRINTA_DIAS";
const ESTOQUE_BAIXO_MAXIMO = 8;
const STATUS_PEDIDO: Record<string, { rotulo: string; cor: CorFatia }> = {
  ABERTO: { rotulo: "Abertos", cor: 1 },
  AGUARDANDO_EMISSAO: { rotulo: "Aguardando nota", cor: 2 },
  CONCLUIDO: { rotulo: "Concluídos", cor: 3 },
  CANCELADO: { rotulo: "Cancelados", cor: 4 },
};
const COR_STATUS_DESCONHECIDO: CorFatia = 5;

/**
 * Painel = central de relatórios: indicadores com comparação, faturamento no tempo, canais, formas
 * de pagamento, horários, mais vendidos e exportação. Valores em dinheiro exigem "ver faturamento";
 * quem não tem vê só a parte operacional (pedidos, estoque, cobranças) que o perfil permite.
 */
export async function montarPainel(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Painel";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const areaRelatorio = document.createElement("div");
  areaRelatorio.className = "painel-relatorio";
  const areaOperacional = document.createElement("div");
  areaOperacional.className = "painel-operacional";

  if (!possui("FATURAMENTO_VER")) {
    container.replaceChildren(cabecalho, areaOperacional);
    montarOperacional(areaOperacional);
    return;
  }

  let periodo = periodoDoAtalho(ATALHO_INICIAL);
  const carregar = (novo: Periodo): void => {
    periodo = novo;
    void carregarRelatorio(areaRelatorio, periodo);
  };
  container.replaceChildren(cabecalho, criarFiltroPeriodo(carregar), areaRelatorio, areaOperacional);
  areaRelatorio.append(elementoCarregando("Calculando o relatório..."));
  carregar(periodo);
  montarOperacional(areaOperacional);
}

async function carregarRelatorio(area: HTMLElement, periodo: Periodo): Promise<void> {
  // Recarregar mantém o relatório anterior esmaecido (sem piscar a tela) até chegar o novo.
  area.classList.add("painel-relatorio--atualizando");
  try {
    const relatorio = await gerarRelatorioVendas(periodo);
    area.replaceChildren(...montarRelatorio(relatorio, periodo));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível calcular o relatório deste período.", "erro"));
  } finally {
    area.classList.remove("painel-relatorio--atualizando");
  }
}

function montarRelatorio(relatorio: RelatorioVendas, periodo: Periodo): HTMLElement[] {
  const faturamento = criarSecaoFaturamento(relatorio);
  faturamento.querySelector(".cartao-relatorio__cabecalho")
    ?.append(criarBotaoExportar("Exportar CSV", periodo, "periodos"));

  // Grade de 3 colunas iguais em todas as linhas: o que é largo (tempo, tabela) ocupa 2.
  return [
    criarIndicadores(relatorio),
    linha(largo(faturamento), criarSecaoFormasPagamento(relatorio)),
    linha(criarSecaoCanais(relatorio), criarSecaoCustoLucro(relatorio), criarSecaoDiasDaSemana(relatorio)),
    linha(
      largo(criarSecaoMaisVendidos(relatorio, criarBotaoExportar("Exportar CSV", periodo, "mais-vendidos"))),
      criarSecaoHorarios(relatorio)
    ),
  ];
}

/** Filtro único acima de tudo: atalhos de período + datas livres. Todos os números seguem ele. */
function criarFiltroPeriodo(aoMudar: (periodo: Periodo) => void): HTMLElement {
  const botoes = ATALHOS.map(({ atalho, rotulo }) => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "filtro-periodo__atalho";
    botao.textContent = rotulo;
    botao.setAttribute("aria-pressed", String(atalho === ATALHO_INICIAL));
    botao.addEventListener("click", () => {
      marcar(botao);
      const periodo = periodoDoAtalho(atalho);
      inicio.value = periodo.inicio;
      fim.value = periodo.fim;
      aoMudar(periodo);
    });
    return botao;
  });

  const inicial = periodoDoAtalho(ATALHO_INICIAL);
  const inicio = criarData("Início do período", inicial.inicio);
  const fim = criarData("Fim do período", inicial.fim);
  const aplicar = document.createElement("button");
  aplicar.type = "button";
  aplicar.className = "btn btn-ghost btn-pequeno";
  aplicar.textContent = "Aplicar datas";
  aplicar.addEventListener("click", () => {
    if (inicio.value === "" || fim.value === "" || fim.value < inicio.value) {
      return;
    }
    marcar(null);
    aoMudar({ inicio: inicio.value, fim: fim.value });
  });

  const marcar = (ativo: HTMLButtonElement | null): void => {
    botoes.forEach((botao) => botao.setAttribute("aria-pressed", String(botao === ativo)));
  };

  const grupoAtalhos = document.createElement("div");
  grupoAtalhos.className = "filtro-periodo__atalhos";
  grupoAtalhos.setAttribute("role", "group");
  grupoAtalhos.setAttribute("aria-label", "Período");
  grupoAtalhos.append(...botoes);

  const separador = document.createElement("span");
  separador.className = "filtro-periodo__ate";
  separador.textContent = "até";
  const datas = document.createElement("div");
  datas.className = "filtro-periodo__datas";
  datas.append(inicio, separador, fim, aplicar);

  const filtro = document.createElement("div");
  filtro.className = "filtro-periodo";
  filtro.append(grupoAtalhos, datas);
  return filtro;
}

function criarData(rotulo: string, valor: string): HTMLInputElement {
  const campo = document.createElement("input");
  campo.type = "date";
  campo.value = valor;
  campo.setAttribute("aria-label", rotulo);
  return campo;
}

function criarBotaoExportar(rotulo: string, periodo: Periodo, tipo: ExportacaoVendas): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-ghost btn-pequeno";
  botao.textContent = rotulo;
  botao.addEventListener("click", () => {
    botao.disabled = true;
    baixarCsvVendas(periodo, tipo)
      .then((arquivo) => salvarArquivo(arquivo, `${tipo}_${periodo.inicio}_a_${periodo.fim}.csv`))
      .catch(() => {
        botao.textContent = "Falhou — tentar de novo";
      })
      .finally(() => {
        botao.disabled = false;
      });
  });
  return botao;
}

function salvarArquivo(arquivo: Blob, nome: string): void {
  const endereco = URL.createObjectURL(arquivo);
  const link = document.createElement("a");
  link.href = endereco;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(endereco);
}

function linha(...cartoes: HTMLElement[]): HTMLElement {
  const grade = document.createElement("div");
  grade.className = "linha-relatorio";
  grade.append(...cartoes);
  return grade;
}

function largo(cartao: HTMLElement): HTMLElement {
  cartao.classList.add("cartao-relatorio--largo");
  return cartao;
}

/** Parte operacional (sem valores de faturamento): cada bloco só aparece com a permissão dele. */
function montarOperacional(area: HTMLElement): void {
  const blocos: HTMLElement[] = [];
  if (possui("CATALOGO_VER")) {
    blocos.push(blocoAssincrono("Estoque baixo", carregarEstoqueBaixo));
  }
  if (possui("PEDIDOS_VER")) {
    blocos.push(blocoAssincrono("Pedidos por situação", carregarPedidosPorStatus));
  }
  if (possui("COBRANCAS_VER")) {
    blocos.push(blocoAssincrono("Cobranças pendentes", carregarCobrancasPendentes));
  }
  if (blocos.length === 0) {
    area.replaceChildren(cartaoEstado("Seu perfil de acesso não inclui nenhum indicador do painel."));
    return;
  }
  const titulo = document.createElement("h2");
  titulo.className = "painel-operacional__titulo";
  titulo.textContent = "Operação agora";
  const grade = document.createElement("div");
  grade.className = "linha-relatorio";
  grade.append(...blocos);
  area.replaceChildren(titulo, grade);
}

function blocoAssincrono(titulo: string, carregar: () => Promise<HTMLElement>): HTMLElement {
  const elementoTitulo = document.createElement("h2");
  elementoTitulo.textContent = titulo;
  const cabecalho = document.createElement("div");
  cabecalho.className = "cartao-relatorio__cabecalho";
  cabecalho.append(elementoTitulo);
  const corpo = document.createElement("div");
  corpo.append(elementoCarregando("Carregando..."));
  const cartao = document.createElement("section");
  cartao.className = "cartao-relatorio";
  cartao.append(cabecalho, corpo);
  carregar()
    .then((conteudo) => corpo.replaceChildren(conteudo))
    .catch(() => corpo.replaceChildren(cartaoEstado("Não foi possível carregar.", "erro")));
  return cartao;
}

async function carregarEstoqueBaixo(): Promise<HTMLElement> {
  const baixos = (await listarProdutos())
    .filter((produto) => produto.ativo && situacaoEstoque(produto.quantidadeEmEstoque).modificador !== "em_estoque")
    .sort((a, b) => a.quantidadeEmEstoque - b.quantidadeEmEstoque)
    .slice(0, ESTOQUE_BAIXO_MAXIMO);
  if (baixos.length === 0) {
    return cartaoEstado("Nenhum produto com estoque baixo.");
  }
  const lista = document.createElement("ul");
  lista.className = "lista-simples";
  lista.append(...baixos.map((produto) => {
    const situacao = situacaoEstoque(produto.quantidadeEmEstoque);
    const nome = document.createElement("span");
    nome.textContent = produto.nome;
    const selo = document.createElement("span");
    selo.className = `selo selo--${situacao.modificador}`;
    selo.textContent = `${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`;
    const item = document.createElement("li");
    item.append(nome, selo);
    return item;
  }));
  return lista;
}

async function carregarPedidosPorStatus(): Promise<HTMLElement> {
  const porStatus = await consultarPedidosPorStatus();
  const fatias: Fatia[] = Object.entries(porStatus).map(([status, quantidade]) => ({
    rotulo: STATUS_PEDIDO[status]?.rotulo ?? status,
    cor: STATUS_PEDIDO[status]?.cor ?? COR_STATUS_DESCONHECIDO,
    valor: quantidade,
    detalhe: "",
  }));
  if (!fatias.some((fatia) => fatia.valor > 0)) {
    return cartaoEstado("Nenhum pedido registrado.");
  }
  return criarGraficoRosca({
    fatias, formatarValor: formatarInteiro, formatarTotal: formatarInteiro, rotuloTotal: "pedidos",
    descricao: "Pedidos por situação",
  });
}

async function carregarCobrancasPendentes(): Promise<HTMLElement> {
  const pendentes = await consultarCobrancasPendentes();
  if (pendentes.length === 0) {
    return cartaoEstado("Nenhuma cobrança pendente — tudo em dia.");
  }
  const total = pendentes.reduce((soma, cobranca) => soma + cobranca.valor, 0);
  const resumo = document.createElement("p");
  resumo.className = "painel-operacional__resumo";
  resumo.textContent = `${formatarInteiro(pendentes.length)} cobrança(s) · ${formatarMoeda(total)}`;
  return resumo;
}
