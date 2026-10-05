import {
  acompanharMaquininha,
  cancelarVendaDoBalcao,
  tentarDeNovoNaMaquininha,
  type VendaBalcao,
} from "../../api/pdvApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { ROTULO_FORMA } from "./ultimasVendasView.js";

const INTERVALO_CONSULTA_MS = 2_000;

/**
 * Enquanto o cliente passa o cartão: consulta a maquininha a cada 2 s. Aprovado → recibo (bandeira e
 * autorização vêm da maquininha). Recusado → tentar de novo (outro cartão) ou cancelar a venda.
 */
export function criarPainelMaquininha(
  vendaInicial: VendaBalcao,
  aoAprovar: (venda: VendaBalcao) => void,
  aoCancelar: () => void
): HTMLElement {
  const titulo = document.createElement("h2");
  // No pagamento dividido a maquininha cobra só a última parte (o que faltava).
  const naMaquininha = vendaInicial.pagamentos.find((parte) => parte.maquininhaIntegrada);
  titulo.textContent = naMaquininha === undefined
    ? `${ROTULO_FORMA[vendaInicial.formaPagamento]} · ${formatarMoeda(vendaInicial.total)}`
    : `${ROTULO_FORMA[naMaquininha.forma]} · ${formatarMoeda(naMaquininha.valor)}`;

  const situacao = document.createElement("p");
  situacao.className = "maquininha__situacao";
  situacao.setAttribute("role", "status");

  const erro = criarMensagemErro();

  const tentarDeNovo = document.createElement("button");
  tentarDeNovo.type = "button";
  tentarDeNovo.className = "btn btn-primary";
  tentarDeNovo.textContent = "Enviar de novo para a maquininha";

  const acoes = document.createElement("div");
  acoes.className = "barra-acoes";
  acoes.append(tentarDeNovo);

  const painel = document.createElement("div");
  painel.className = "formulario-cartao maquininha";
  painel.append(titulo, situacao, erro, acoes);

  if (possui("PDV_CANCELAR")) {
    acoes.append(criarBotaoCancelar(vendaInicial.pedidoId, erro, aoCancelar));
  }

  let intervalo = 0;
  const aguardar = (): void => {
    situacao.className = "maquininha__situacao";
    situacao.textContent = "Valor enviado. Peça para o cliente aproximar, inserir ou passar o cartão na maquininha...";
    tentarDeNovo.hidden = true;
    window.clearInterval(intervalo);
    intervalo = window.setInterval(consultar, INTERVALO_CONSULTA_MS);
  };

  const consultar = (): void => {
    if (!painel.isConnected) {
      window.clearInterval(intervalo);
      return;
    }
    acompanharMaquininha(vendaInicial.pedidoId)
      .then((venda) => {
        if (venda.statusPagamento === "APROVADO") {
          window.clearInterval(intervalo);
          aoAprovar(venda);
        } else if (venda.statusPagamento === "RECUSADO") {
          window.clearInterval(intervalo);
          situacao.className = "maquininha__situacao maquininha__situacao--recusado";
          situacao.textContent = "Pagamento recusado na maquininha. Tente outro cartão ou cancele a venda.";
          tentarDeNovo.hidden = false;
        }
      })
      .catch(() => {
        situacao.textContent = "Aguardando a maquininha... (tentando reconectar)";
      });
  };

  tentarDeNovo.addEventListener("click", () => {
    tentarDeNovo.disabled = true;
    erro.hidden = true;
    tentarDeNovoNaMaquininha(vendaInicial.pedidoId)
      .then(aguardar)
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível enviar para a maquininha."))
      .finally(() => {
        tentarDeNovo.disabled = false;
      });
  });

  aguardar();
  return painel;
}

function criarBotaoCancelar(pedidoId: string, erro: HTMLElement, aoCancelar: () => void): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-perigo";
  botao.textContent = "Cancelar venda";
  botao.addEventListener("click", () => {
    if (!window.confirm("Cancelar esta venda? O valor sai da maquininha e os produtos voltam ao estoque.")) {
      return;
    }
    botao.disabled = true;
    cancelarVendaDoBalcao(pedidoId)
      .then(aoCancelar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível cancelar a venda.");
        botao.disabled = false;
      });
  });
  return botao;
}
