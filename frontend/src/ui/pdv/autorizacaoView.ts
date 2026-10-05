import { autorizarNoCaixa, type AcaoAutorizada, type Autorizacao } from "../../api/pdvApi.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, type CampoTexto } from "../camposFormulario.js";
import { formatarMoeda } from "../formatarMoeda.js";

const CENTAVOS_POR_REAL = 100;
const CEM_POR_CENTO = 100;

/**
 * Janela "senha do gerente" (D35): quem tem permissão de autorizar digita e-mail e senha ali no
 * caixa. Resolve com a autorização (token curto) ou null se cancelarem. A senha só vai ao servidor.
 */
export async function pedirAutorizacao(acao: AcaoAutorizada, titulo: string, explicacao: string): Promise<Autorizacao | null> {
  const resultado = await abrirJanela(acao, titulo, explicacao, null);
  return resultado?.autorizacao ?? null;
}

/**
 * Desconto: valor (em R$ ou "10%") e a senha do gerente na mesma janela. Resolve com o valor em
 * reais e a autorização, ou null se cancelarem.
 */
export async function pedirDescontoAutorizado(totalDosItens: number): Promise<{ valor: number; autorizacao: Autorizacao } | null> {
  const campo = criarCampoTexto("desconto-valor", "Desconto (em R$, ou termine com % — ex.: 10%)", "text", true);
  campo.entrada.inputMode = "decimal";
  campo.entrada.autocomplete = "off";
  const resultado = await abrirJanela("DESCONTO", "Dar desconto",
    `A venda está em ${formatarMoeda(totalDosItens)}. O desconto precisa da autorização de um gerente.`,
    { campo, interpretar: (texto) => interpretarDesconto(texto, totalDosItens),
      mensagemInvalido: "Desconto inválido: maior que zero e menor que o total da venda." });
  return resultado === null || resultado.valor === null ? null : { valor: resultado.valor, autorizacao: resultado.autorizacao };
}

interface CampoValor {
  campo: CampoTexto;
  interpretar: (texto: string) => number | null;
  mensagemInvalido: string;
}

function abrirJanela(
  acao: AcaoAutorizada, titulo: string, explicacao: string, campoValor: CampoValor | null
): Promise<{ autorizacao: Autorizacao; valor: number | null } | null> {
  return new Promise((resolver) => {
    const cabecalho = document.createElement("h2");
    cabecalho.textContent = titulo;
    const texto = document.createElement("p");
    texto.className = "caixa-painel__instrucao";
    texto.textContent = explicacao;
    const email = criarCampoTexto("autorizacao-email", "E-mail de quem autoriza", "email", true);
    email.entrada.autocomplete = "off";
    const senha = criarCampoTexto("autorizacao-senha", "Senha", "password", true);
    senha.entrada.autocomplete = "off";
    const erro = criarMensagemErro();

    const cancelar = document.createElement("button");
    cancelar.type = "button";
    cancelar.className = "btn btn-ghost";
    cancelar.textContent = "Cancelar";
    const autorizar = document.createElement("button");
    autorizar.type = "submit";
    autorizar.className = "btn btn-primary";
    autorizar.textContent = "Autorizar";
    const acoes = document.createElement("div");
    acoes.className = "caixa-painel__acoes";
    acoes.append(cancelar, autorizar);

    const formulario = document.createElement("form");
    formulario.className = "autorizacao";
    formulario.append(cabecalho, texto);
    if (campoValor !== null) {
      formulario.append(campoValor.campo.container);
    }
    formulario.append(email.container, senha.container, erro, acoes);

    const janela = document.createElement("dialog");
    janela.className = "autorizacao__janela";
    janela.append(formulario);
    document.body.append(janela);

    let resultado: { autorizacao: Autorizacao; valor: number | null } | null = null;
    janela.addEventListener("close", () => {
      janela.remove();
      resolver(resultado);
    });
    cancelar.addEventListener("click", () => janela.close());
    formulario.addEventListener("submit", (evento) => {
      evento.preventDefault();
      erro.hidden = true;
      const valor = campoValor === null ? null : campoValor.interpretar(campoValor.campo.entrada.value);
      if (campoValor !== null && valor === null) {
        mostrarErro(erro, null, campoValor.mensagemInvalido);
        campoValor.campo.entrada.focus();
        return;
      }
      autorizar.disabled = true;
      autorizarNoCaixa(email.entrada.value.trim(), senha.entrada.value, acao)
        .then((autorizacao) => {
          resultado = { autorizacao, valor };
          janela.close();
        })
        .catch((falha: unknown) => {
          senha.entrada.value = "";
          mostrarErro(erro, falha, "Não foi possível autorizar.");
          autorizar.disabled = false;
          senha.entrada.focus();
        });
    });
    janela.showModal();
    (campoValor?.campo.entrada ?? email.entrada).focus();
  });
}

/** "5" ou "5,50" = reais; "10%" = porcentagem do total. Arredonda para centavos. */
function interpretarDesconto(texto: string, totalDosItens: number): number | null {
  const limpo = texto.trim().replace(",", ".");
  const porcentagem = limpo.endsWith("%");
  const numero = Number(porcentagem ? limpo.slice(0, -1) : limpo);
  if (!Number.isFinite(numero) || numero <= 0) {
    return null;
  }
  const bruto = porcentagem ? (totalDosItens * numero) / CEM_POR_CENTO : numero;
  const valor = Math.round(bruto * CENTAVOS_POR_REAL) / CENTAVOS_POR_REAL;
  return valor > 0 && valor < totalDosItens ? valor : null;
}
