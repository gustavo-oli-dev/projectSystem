import {
  acompanharPix,
  cancelarVendaDoBalcao,
  type VendaBalcao,
  type VendaPixIniciada,
} from "../../api/pdvApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";

const INTERVALO_CONSULTA_MS = 3_000;
const TEMPO_COPIADO_MS = 2_000;

/**
 * Mostra o QR do Pix para o cliente e consulta a cada 3 s se ele pagou. A consulta para sozinha
 * quando o painel sai da tela (venda concluída, cancelada ou troca de página).
 */
export function criarPainelPix(
  pix: VendaPixIniciada,
  aoPagar: (venda: VendaBalcao) => void,
  aoCancelar: () => void
): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = `Pix de ${formatarMoeda(pix.total)}`;

  const instrucao = document.createElement("p");
  instrucao.className = "pix__instrucao";
  instrucao.textContent = "Peça para o cliente abrir o app do banco, escolher Pix e ler o QR code.";

  const situacao = document.createElement("p");
  situacao.className = "pix__situacao";
  situacao.setAttribute("role", "status");
  situacao.textContent = "Aguardando pagamento...";

  const erro = criarMensagemErro();

  const painel = document.createElement("div");
  painel.className = "formulario-cartao pix";
  painel.append(titulo, instrucao, criarImagemQr(pix), criarCopiaECola(pix), situacao, erro);

  if (possui("PDV_CANCELAR")) {
    painel.append(criarBotaoCancelar(pix, erro, aoCancelar));
  }

  const intervalo = window.setInterval(() => {
    if (!painel.isConnected) {
      window.clearInterval(intervalo);
      return;
    }
    acompanharPix(pix.pedidoId)
      .then((venda) => {
        if (venda.statusPagamento === "APROVADO") {
          window.clearInterval(intervalo);
          aoPagar(venda);
        } else if (venda.statusPagamento === "ESTORNADO") {
          window.clearInterval(intervalo);
          situacao.textContent = "Pix cancelado ou expirado. Os produtos voltaram ao estoque.";
        }
      })
      .catch(() => {
        // Falha momentânea de rede: tenta de novo na próxima volta, sem assustar o operador.
        situacao.textContent = "Aguardando pagamento... (tentando reconectar)";
      });
  }, INTERVALO_CONSULTA_MS);

  return painel;
}

function criarImagemQr(pix: VendaPixIniciada): HTMLElement {
  if (pix.qrCodeImagemBase64 === null) {
    const aviso = document.createElement("p");
    aviso.className = "estado-info";
    aviso.textContent = "O Mercado Pago não enviou a imagem do QR. Use o código copia e cola abaixo.";
    return aviso;
  }
  const imagem = document.createElement("img");
  imagem.className = "pix__qr";
  imagem.alt = "QR code do Pix";
  imagem.src = `data:image/png;base64,${pix.qrCodeImagemBase64}`;
  return imagem;
}

function criarCopiaECola(pix: VendaPixIniciada): HTMLElement {
  const bloco = document.createElement("div");
  bloco.className = "pix__copia-e-cola";
  if (pix.qrCodeCopiaECola === null) {
    return bloco;
  }
  const codigo = document.createElement("input");
  codigo.type = "text";
  codigo.readOnly = true;
  codigo.value = pix.qrCodeCopiaECola;
  codigo.setAttribute("aria-label", "Código Pix copia e cola");

  const copiar = document.createElement("button");
  copiar.type = "button";
  copiar.className = "btn btn-outline btn-pequeno";
  copiar.textContent = "Copiar código";
  copiar.addEventListener("click", () => {
    navigator.clipboard.writeText(codigo.value)
      .then(() => {
        copiar.textContent = "Copiado!";
        window.setTimeout(() => {
          copiar.textContent = "Copiar código";
        }, TEMPO_COPIADO_MS);
      })
      .catch(() => codigo.select());
  });

  bloco.append(codigo, copiar);
  return bloco;
}

function criarBotaoCancelar(pix: VendaPixIniciada, erro: HTMLElement, aoCancelar: () => void): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-ghost btn-pequeno";
  botao.textContent = "Cliente desistiu — cancelar venda";
  botao.addEventListener("click", () => {
    if (!window.confirm("Cancelar esta venda? Os produtos voltam ao estoque e o Pix é cancelado.")) {
      return;
    }
    botao.disabled = true;
    cancelarVendaDoBalcao(pix.pedidoId)
      .then(aoCancelar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível cancelar a venda.");
        botao.disabled = false;
      });
  });
  return botao;
}
