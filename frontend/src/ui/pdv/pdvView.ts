import {
  iniciarNaMaquininha,
  iniciarVendaComPix,
  venderNoBalcao,
  type DadosVendaBalcao,
  type VendaBalcao,
} from "../../api/pdvApi.js";
import { buscarCaixaAberto, type CaixaAberto } from "../../api/caixaApi.js";
import { listarProdutos, type Produto } from "../../api/produtosApi.js";
import { listarPromocoesValendoHoje } from "../../api/promocoesApi.js";
import { navegarPara } from "../../router.js";
import {
  adicionarAoCarrinho,
  aoMudarCarrinho,
  itensDoCarrinho,
  limparCarrinho,
  definirDesconto,
  descontoDaVenda,
  totalAPagar,
  totalDoCarrinho,
} from "../../state/caixaState.js";
import { definirPromocoesDoDia } from "../../state/promocoesDoDia.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { pedirDescontoAutorizado } from "./autorizacaoView.js";
import { renderizarCarrinho } from "./carrinhoView.js";
import { criarCampoLeitura } from "./leituraView.js";
import { criarSeletorCliente } from "./clienteCaixaView.js";
import { criarPainelMaquininha } from "./maquininhaView.js";
import { criarPainelPagamento, type ModoFechamento } from "./pagamentoView.js";
import { criarPainelPix } from "./pixNaTelaView.js";
import { descreverParte } from "./partesPagamentoView.js";
import { carregarUltimasVendas, ROTULO_FORMA } from "./ultimasVendasView.js";

const HORA = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

/** Caixa (PDV): ler produtos → conferir → receber → finalizar. O estoque é o mesmo do site e do WhatsApp. */
export async function montarPdv(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Caixa";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const areaVenda = document.createElement("div");
  const areaUltimas = document.createElement("div");
  // Recolhido por padrão: no dia a dia o caixa só precisa da venda atual.
  const tituloUltimas = document.createElement("summary");
  tituloUltimas.textContent = "Últimas vendas do caixa";
  const secaoUltimas = document.createElement("details");
  secaoUltimas.className = "secao-recolhivel";
  secaoUltimas.append(tituloUltimas, areaUltimas);

  container.replaceChildren(cabecalho, areaVenda, secaoUltimas);
  void carregarUltimasVendas(areaUltimas);

  if (!possui("PDV_VENDER")) {
    areaVenda.replaceChildren(cartaoEstado("Seu perfil de acesso pode consultar e cancelar vendas, mas não vender."));
    return;
  }

  areaVenda.replaceChildren(elementoCarregando("Carregando o caixa..."));
  try {
    // Sem as promoções o caixa ainda vende (o servidor aplica ao vender); só a tela não as mostra antes.
    const [produtos, caixa, promocoes] = await Promise.all([
      listarProdutos(), buscarCaixaAberto(), listarPromocoesValendoHoje().catch(() => []),
    ]);
    definirPromocoesDoDia(promocoes);
    iniciarCaixa(areaVenda, produtos, caixa, () => void carregarUltimasVendas(areaUltimas));
  } catch {
    areaVenda.replaceChildren(cartaoEstado("Não foi possível carregar o caixa.", "erro"));
  }
}

/**
 * O operador só vende (D27): abrir, repor troco, sangria e fechar ficam na Gestão de caixa, com
 * quem tem CAIXA_GERENCIAR. Sem caixa aberto, a tela avisa e deixa conferir de novo.
 */
function iniciarCaixa(area: HTMLElement, produtos: Produto[], caixa: CaixaAberto | null, aoVender: () => void): void {
  if (caixa === null) {
    area.replaceChildren(criarAvisoCaixaFechado(() => {
      area.replaceChildren(elementoCarregando("Verificando o caixa..."));
      buscarCaixaAberto()
        .then((atual) => iniciarCaixa(area, produtos, atual, aoVender))
        .catch(() => area.replaceChildren(cartaoEstado("Não foi possível verificar o caixa.", "erro")));
    }));
    return;
  }
  const areaDaVenda = document.createElement("div");
  area.replaceChildren(criarFaixaCaixa(caixa), areaDaVenda);
  montarVenda(areaDaVenda, produtos, aoVender);
}

function criarAvisoCaixaFechado(aoVerificar: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Seu caixa ainda não foi aberto";
  const texto = document.createElement("p");
  texto.className = "caixa-painel__instrucao";
  texto.textContent = "Peça a um responsável pelo caixa para abrir com o fundo de troco. Depois, clique em \"Verificar de novo\".";
  const verificar = document.createElement("button");
  verificar.type = "button";
  verificar.className = "btn btn-primary";
  verificar.textContent = "Verificar de novo";
  verificar.addEventListener("click", aoVerificar);
  const acoes = document.createElement("div");
  acoes.className = "caixa-painel__acoes";
  if (possui("CAIXA_GERENCIAR")) {
    const gestao = document.createElement("button");
    gestao.type = "button";
    gestao.className = "btn btn-ghost";
    gestao.textContent = "Ir para a Gestão de caixa";
    gestao.addEventListener("click", () => navegarPara("gestao-caixa"));
    acoes.append(gestao);
  }
  acoes.append(verificar);
  const aviso = document.createElement("div");
  aviso.className = "caixa-painel";
  aviso.append(titulo, texto, acoes);
  return aviso;
}

/** Só informação: de quem é o caixa e quem abriu. Nenhuma ação de dinheiro aqui. */
function criarFaixaCaixa(caixa: CaixaAberto): HTMLElement {
  const marcador = document.createElement("span");
  marcador.className = "barra-caixa__marcador";
  marcador.setAttribute("aria-hidden", "true");
  const texto = document.createElement("span");
  texto.textContent = `${caixa.pontoNome} aberto às ${HORA.format(new Date(caixa.abertaEm))} por ${caixa.abertaPorNome} · operador: ${caixa.operadorNome}`;
  const situacao = document.createElement("p");
  situacao.className = "barra-caixa__situacao";
  situacao.append(marcador, texto);
  const faixa = document.createElement("div");
  faixa.className = "barra-caixa";
  faixa.append(situacao);
  return faixa;
}

function montarVenda(area: HTMLElement, produtos: Produto[], aoVender: () => void): void {
  limparCarrinho();
  const leitura = criarCampoLeitura(produtos, adicionarAoCarrinho);
  const carrinho = document.createElement("div");

  const total = document.createElement("p");
  total.className = "pdv__total";
  const pagamento = criarPainelPagamento();
  const cpf = criarCampoTexto("pdv-cpf", "CPF na nota (opcional)", "text", false);
  cpf.entrada.inputMode = "numeric";
  cpf.entrada.maxLength = 14;

  const cliente = criarSeletorCliente((cpfDoCliente) => {
    cpf.entrada.value = cpfDoCliente;
  });

  const erro = criarMensagemErro();
  const finalizar = document.createElement("button");
  finalizar.type = "button";
  finalizar.className = "btn btn-primary pdv__finalizar";
  finalizar.textContent = pagamento.rotuloFinalizar();
  pagamento.aoMudarModo(() => {
    finalizar.textContent = pagamento.rotuloFinalizar();
  });

  const colunaItens = document.createElement("section");
  colunaItens.className = "pdv__itens";
  colunaItens.append(leitura.elemento, carrinho);

  const colunaPagamento = document.createElement("aside");
  colunaPagamento.className = "pdv__pagamento";
  const rotuloTotal = document.createElement("p");
  rotuloTotal.className = "pdv__rotulo-total";
  rotuloTotal.textContent = "Total da venda";
  // Cliente e CPF na nota são opcionais: ficam recolhidos para não poluir o fechamento da venda.
  const resumoOpcionais = document.createElement("summary");
  resumoOpcionais.textContent = "Cliente e CPF na nota (opcional)";
  const opcionais = document.createElement("details");
  opcionais.className = "pdv__opcionais";
  opcionais.append(resumoOpcionais, cliente.elemento, cpf.container);

  const blocoTotal = document.createElement("div");
  blocoTotal.className = "pdv__bloco-total";
  const linhaDesconto = document.createElement("div");
  linhaDesconto.className = "pdv__desconto";
  blocoTotal.append(rotuloTotal, total, linhaDesconto);
  colunaPagamento.append(blocoTotal, pagamento.elemento, opcionais, erro, finalizar);

  const grade = document.createElement("div");
  grade.className = "pdv";
  grade.append(colunaItens, colunaPagamento);
  area.replaceChildren(grade);

  const atualizar = (): void => {
    renderizarCarrinho(carrinho);
    const valor = totalAPagar();
    total.textContent = formatarMoeda(valor);
    pagamento.atualizarTotal(valor);
    linhaDesconto.replaceChildren(criarLinhaDesconto(totalDoCarrinho()));
    finalizar.disabled = itensDoCarrinho().length === 0;
  };
  aoMudarCarrinho(atualizar);
  atualizar();
  leitura.focar();

  const novaVenda = (): void => montarVenda(area, produtos, aoVender);
  const mostrarRecibo = (venda: VendaBalcao): void => {
    area.replaceChildren(criarRecibo(venda, novaVenda));
    aoVender();
  };

  finalizar.addEventListener("click", () => {
    erro.hidden = true;
    finalizar.disabled = true;
    const dados: DadosVendaBalcao = {
      itens: itensDoCarrinho().map((item) => ({
        produtoId: item.produto.id, embalagemId: item.embalagem?.id ?? null, quantidade: item.quantidade,
      })),
      cpfNaNota: textoOuNulo(cpf.entrada.value),
      clienteId: cliente.clienteId(),
      desconto: descontoParaEnviar(),
    };
    const aoDesistir = (): void => {
      novaVenda();
      aoVender();
    };
    const mostrarEspera = (painel: HTMLElement): void => {
      area.replaceChildren(painel);
      aoVender();
    };

    const fechamentos: Record<ModoFechamento, () => Promise<unknown>> = {
      PIX_QR: () => iniciarVendaComPix(dados)
        .then((pix) => mostrarEspera(criarPainelPix(pix, mostrarRecibo, aoDesistir))),
      MAQUININHA: () => iniciarNaMaquininha(dados, pagamento.lerPartes(), pagamento.formaCartao())
        .then((venda) => mostrarEspera(criarPainelMaquininha(venda, mostrarRecibo, aoDesistir))),
      DINHEIRO: () => venderNoBalcao({ ...dados, partes: pagamento.lerPartes(), pagamento: pagamento.lerPagamento() }).then(mostrarRecibo),
      CONTINGENCIA: () => venderNoBalcao({ ...dados, partes: pagamento.lerPartes(), pagamento: pagamento.lerPagamento() }).then(mostrarRecibo),
    };

    fechamentos[pagamento.modo()]().catch((falha: unknown) => {
      mostrarErro(erro, falha, "Não foi possível finalizar a venda.");
      finalizar.disabled = false;
    });
  });
}

function criarRecibo(venda: VendaBalcao, novaVenda: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "Venda concluída";

  const linhas: Array<[string, string]> = [
    ["Total", formatarMoeda(venda.total)],
    ["Pagamento", ROTULO_FORMA[venda.formaPagamento]],
  ];
  if (venda.desconto > 0) {
    linhas.splice(1, 0, ["Desconto", `− ${formatarMoeda(venda.desconto)}`]);
  }
  if (venda.descontoPromocao > 0) {
    linhas.splice(1, 0, ["Promoções", `− ${formatarMoeda(venda.descontoPromocao)}`]);
  }
  const dividido = venda.pagamentos.length > 1;
  if (dividido) {
    // Pagamento dividido: uma linha por forma, na ordem (o troco é só do dinheiro).
    venda.pagamentos.forEach((parte) => linhas.push([descreverParte(parte), formatarMoeda(parte.valor)]));
  }
  if (venda.troco !== null) {
    linhas.push(["Troco", formatarMoeda(venda.troco)]);
  }
  if (!dividido && venda.bandeira !== null) {
    linhas.push(["Bandeira", venda.bandeira.replace("_", " ")]);
  }
  if (!dividido && venda.codigoAutorizacao !== null) {
    linhas.push(["Autorização", venda.codigoAutorizacao]);
  }
  linhas.push(["Nota fiscal", "NFC-e gerada (pendente de transmissão)"]);

  const lista = document.createElement("dl");
  lista.className = "lista-dados";
  for (const [rotulo, valor] of linhas) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    lista.append(termo, definicao);
  }

  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-primary";
  botao.textContent = "Nova venda";
  botao.addEventListener("click", novaVenda);

  const recibo = document.createElement("div");
  recibo.className = "formulario-cartao recibo";
  recibo.append(titulo, lista, botao);
  queueMicrotask(() => botao.focus());
  return recibo;
}

/**
 * Desconto na venda (D35): "Dar desconto" pede o valor e a senha do gerente; aplicado, mostra
 * "Desconto − R$ X · autorizado por Fulano" com a opção de tirar.
 */
function criarLinhaDesconto(totalDosItens: number): HTMLElement {
  const desconto = descontoDaVenda();
  const conteudo = document.createElement("div");
  conteudo.className = "pdv__desconto-linha";
  if (desconto !== null) {
    const texto = document.createElement("span");
    texto.textContent = `Desconto − ${formatarMoeda(desconto.valor)} · autorizado por ${desconto.autorizadoPorNome}`;
    const tirar = document.createElement("button");
    tirar.type = "button";
    tirar.className = "link-tabela";
    tirar.textContent = "tirar";
    tirar.addEventListener("click", () => definirDesconto(null));
    conteudo.append(texto, tirar);
    return conteudo;
  }
  if (totalDosItens <= 0) {
    return conteudo;
  }
  const dar = document.createElement("button");
  dar.type = "button";
  dar.className = "btn btn-ghost btn-pequeno";
  dar.textContent = "Dar desconto";
  dar.addEventListener("click", () => void pedirDesconto(totalDosItens));
  conteudo.append(dar);
  return conteudo;
}

async function pedirDesconto(totalDosItens: number): Promise<void> {
  const escolhido = await pedirDescontoAutorizado(totalDosItens);
  if (escolhido !== null) {
    definirDesconto({
      valor: escolhido.valor,
      tokenAutorizacao: escolhido.autorizacao.token,
      autorizadoPorNome: escolhido.autorizacao.autorizadoPorNome,
    });
  }
}

function descontoParaEnviar(): { valor: number; tokenAutorizacao: string } | null {
  const desconto = descontoDaVenda();
  return desconto === null ? null : { valor: desconto.valor, tokenAutorizacao: desconto.tokenAutorizacao };
}
