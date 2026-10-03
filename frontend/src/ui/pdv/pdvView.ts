import { iniciarVendaComPix, venderNoBalcao, type VendaBalcao } from "../../api/pdvApi.js";
import { listarProdutos, type Produto } from "../../api/produtosApi.js";
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
import { criarPainelPagamento } from "./pagamentoView.js";
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
  const tituloUltimas = document.createElement("h2");
  tituloUltimas.className = "secao-painel__titulo";
  tituloUltimas.textContent = "Últimas vendas do caixa";
  const secaoUltimas = document.createElement("section");
  secaoUltimas.className = "secao-painel";
  secaoUltimas.append(tituloUltimas, areaUltimas);

  container.replaceChildren(cabecalho, areaVenda, secaoUltimas);
  void carregarUltimasVendas(areaUltimas);

  if (!possui("PDV_VENDER")) {
    areaVenda.replaceChildren(cartaoEstado("Seu perfil de acesso pode consultar e cancelar vendas, mas não vender."));
    return;
  }

  areaVenda.replaceChildren(elementoCarregando("Carregando produtos..."));
  try {
    const produtos = await listarProdutos();
    montarVenda(areaVenda, produtos, () => void carregarUltimasVendas(areaUltimas));
  } catch {
    areaVenda.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
  }
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

  const erro = criarMensagemErro();
  const finalizar = document.createElement("button");
  finalizar.type = "button";
  finalizar.className = "btn btn-primary pdv__finalizar";
  finalizar.textContent = "Finalizar venda";

  const colunaItens = document.createElement("section");
  colunaItens.className = "pdv__itens";
  colunaItens.append(leitura.elemento, carrinho);

  const colunaPagamento = document.createElement("aside");
  colunaPagamento.className = "pdv__pagamento";
  colunaPagamento.append(total, pagamento.elemento, cpf.container, erro, finalizar);

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
    const itens = itensDoCarrinho().map((item) => ({ produtoId: item.produto.id, quantidade: item.quantidade }));
    const cpfNaNota = textoOuNulo(cpf.entrada.value);

    const venda: Promise<unknown> = pagamento.pixNaTela()
      ? iniciarVendaComPix(itens, cpfNaNota).then((pix) => {
        area.replaceChildren(criarPainelPix(pix, mostrarRecibo, () => {
          novaVenda();
          aoVender();
        }));
        aoVender();
      })
      : venderNoBalcao({ itens, cpfNaNota, pagamento: pagamento.lerPagamento() }).then(mostrarRecibo);

    venda.catch((falha: unknown) => {
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
