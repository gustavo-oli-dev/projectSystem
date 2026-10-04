import {
  iniciarNaMaquininha,
  iniciarVendaComPix,
  venderNoBalcao,
  type DadosVendaBalcao,
  type VendaBalcao,
} from "../../api/pdvApi.js";
import { buscarCaixaAberto, type CaixaAberto } from "../../api/caixaApi.js";
import { listarProdutos, type Produto } from "../../api/produtosApi.js";
import { criarAberturaCaixa } from "../caixa/aberturaCaixaView.js";
import {
  criarBarraCaixa,
  criarPainelFechamento,
  criarPainelReposicao,
  criarPainelSangria,
  criarTelaCaixaFechado,
} from "../caixa/operacaoCaixaView.js";
import {
  adicionarAoCarrinho,
  aoMudarCarrinho,
  itensDoCarrinho,
  limparCarrinho,
  totalDoCarrinho,
} from "../../state/caixaState.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { renderizarCarrinho } from "./carrinhoView.js";
import { criarCampoLeitura } from "./leituraView.js";
import { criarSeletorCliente } from "./clienteCaixaView.js";
import { criarPainelMaquininha } from "./maquininhaView.js";
import { criarPainelPagamento, type ModoFechamento } from "./pagamentoView.js";
import { criarPainelPix } from "./pixNaTelaView.js";
import { carregarUltimasVendas, ROTULO_FORMA } from "./ultimasVendasView.js";

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
    const [produtos, caixa] = await Promise.all([listarProdutos(), buscarCaixaAberto()]);
    iniciarCaixa(areaVenda, produtos, caixa, () => void carregarUltimasVendas(areaUltimas));
  } catch {
    areaVenda.replaceChildren(cartaoEstado("Não foi possível carregar o caixa.", "erro"));
  }
}

/**
 * Sem caixa aberto: tela de abertura (contagem do fundo de troco). Com caixa aberto: faixa do caixa
 * no topo e a venda embaixo. Reposição, sangria e fechamento abrem no lugar da venda sem perder o
 * carrinho — "Voltar para a venda" devolve tudo como estava.
 */
function iniciarCaixa(area: HTMLElement, produtos: Produto[], caixa: CaixaAberto | null, aoVender: () => void): void {
  if (caixa === null) {
    area.replaceChildren(criarAberturaCaixa((aberto) => iniciarCaixa(area, produtos, aberto, aoVender)));
    return;
  }
  const faixa = document.createElement("div");
  const areaPainel = document.createElement("div");
  areaPainel.hidden = true;
  const areaDaVenda = document.createElement("div");

  const voltarParaVenda = (): void => {
    areaPainel.hidden = true;
    areaPainel.replaceChildren();
    areaDaVenda.hidden = false;
  };
  const abrirPainel = (painel: HTMLElement): void => {
    areaDaVenda.hidden = true;
    areaPainel.replaceChildren(painel);
    areaPainel.hidden = false;
  };
  const aoAtualizarCaixa = (atualizado: CaixaAberto): void => {
    desenharFaixa(atualizado);
    voltarParaVenda();
  };
  const desenharFaixa = (atual: CaixaAberto): void => {
    faixa.replaceChildren(criarBarraCaixa(atual, {
      aoRepor: () => abrirPainel(criarPainelReposicao(aoAtualizarCaixa, voltarParaVenda)),
      aoSangria: () => abrirPainel(criarPainelSangria(aoAtualizarCaixa, voltarParaVenda)),
      aoFechar: () => abrirPainel(criarPainelFechamento((conferencia) => {
        limparCarrinho();
        area.replaceChildren(criarTelaCaixaFechado(conferencia, () => iniciarCaixa(area, produtos, null, aoVender)));
      }, voltarParaVenda)),
    }));
  };

  desenharFaixa(caixa);
  area.replaceChildren(faixa, areaPainel, areaDaVenda);
  montarVenda(areaDaVenda, produtos, aoVender);
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
  blocoTotal.append(rotuloTotal, total);
  colunaPagamento.append(blocoTotal, pagamento.elemento, opcionais, erro, finalizar);

  const grade = document.createElement("div");
  grade.className = "pdv";
  grade.append(colunaItens, colunaPagamento);
  area.replaceChildren(grade);

  const atualizar = (): void => {
    renderizarCarrinho(carrinho);
    const valor = totalDoCarrinho();
    total.textContent = formatarMoeda(valor);
    pagamento.atualizarTotal(valor);
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
      itens: itensDoCarrinho().map((item) => ({ produtoId: item.produto.id, quantidade: item.quantidade })),
      cpfNaNota: textoOuNulo(cpf.entrada.value),
      clienteId: cliente.clienteId(),
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
      MAQUININHA: () => iniciarNaMaquininha(dados, pagamento.formaCartao())
        .then((venda) => mostrarEspera(criarPainelMaquininha(venda, mostrarRecibo, aoDesistir))),
      DINHEIRO: () => venderNoBalcao({ ...dados, pagamento: pagamento.lerPagamento() }).then(mostrarRecibo),
      CONTINGENCIA: () => venderNoBalcao({ ...dados, pagamento: pagamento.lerPagamento() }).then(mostrarRecibo),
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
  if (venda.troco !== null) {
    linhas.push(["Troco", formatarMoeda(venda.troco)]);
  }
  if (venda.bandeira !== null) {
    linhas.push(["Bandeira", venda.bandeira.replace("_", " ")]);
  }
  if (venda.codigoAutorizacao !== null) {
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
