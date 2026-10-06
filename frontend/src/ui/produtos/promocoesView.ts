import { listarProdutos, type Produto } from "../../api/produtosApi.js";
import {
  criarPromocao,
  encerrarPromocao,
  listarPromocoes,
  type Promocao,
  type SituacaoPromocao,
  type TipoPromocao,
} from "../../api/promocoesApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { descreverPromocao } from "./descricaoPromocao.js";

const DATA = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

const SITUACAO: Record<SituacaoPromocao, { rotulo: string; modificador: string }> = {
  VALENDO: { rotulo: "Valendo", modificador: "concluido" },
  AGENDADA: { rotulo: "Agendada", modificador: "pendente" },
  TERMINADA: { rotulo: "Terminou", modificador: "cancelado" },
  ENCERRADA: { rotulo: "Encerrada", modificador: "cancelado" },
};

const TIPOS: ReadonlyArray<{ valor: TipoPromocao; rotulo: string }> = [
  { valor: "PRECO_OFERTA", rotulo: "Preço de oferta (de R$ X por R$ Y)" },
  { valor: "LEVE_PAGUE", rotulo: "Leve X, pague Y" },
];

/**
 * Promoções (D38): preço de oferta ou "leve X pague Y", por período. O caixa aplica sozinho no
 * dia; para mudar uma promoção, encerre e crie outra (o que valeu em cada venda fica registrado).
 */
export async function montarPromocoes(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Promoções";
  const nova = document.createElement("button");
  nova.type = "button";
  nova.className = "btn btn-primary";
  nova.textContent = "Nova promoção";
  nova.hidden = !possui("PROMOCOES_GERENCIAR");
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, nova);
  const formulario = document.createElement("div");
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, formulario, area);

  const recarregar = async (): Promise<void> => {
    area.replaceChildren(elementoCarregando("Carregando promoções..."));
    try {
      const promocoes = await listarPromocoes();
      area.replaceChildren(promocoes.length === 0
        ? cartaoEstado("Nenhuma promoção cadastrada.")
        : criarLista(promocoes, () => void recarregar()));
    } catch {
      area.replaceChildren(cartaoEstado("Não foi possível carregar as promoções.", "erro"));
    }
  };
  nova.addEventListener("click", () => {
    nova.hidden = true;
    void abrirFormulario(formulario, () => {
      formulario.replaceChildren();
      nova.hidden = false;
      void recarregar();
    });
  });
  await recarregar();
}

function criarLista(promocoes: readonly Promocao[], aoMudar: () => void): HTMLElement {
  const podeEncerrar = possui("PROMOCOES_GERENCIAR");
  return criarTabela(
    ["Produto", "Promoção", "Preço normal", "Período", "Situação", ""],
    promocoes.map((promocao) => criarLinha(
      celula(promocao.produtoNome),
      celula(descreverPromocao(promocao)),
      celula(formatarMoeda(promocao.precoNormal)),
      celula(`${DATA.format(dataDe(promocao.inicio))} a ${DATA.format(dataDe(promocao.fim))}`),
      celulaSelo(SITUACAO[promocao.situacao].rotulo, SITUACAO[promocao.situacao].modificador),
      celulaComConteudo(podeEncerrar && (promocao.situacao === "VALENDO" || promocao.situacao === "AGENDADA")
        ? criarBotaoEncerrar(promocao, aoMudar)
        : document.createElement("span"))
    )),
    "promoção(ões)"
  );
}

function criarBotaoEncerrar(promocao: Promocao, aoMudar: () => void): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "link-tabela";
  botao.textContent = "Encerrar";
  botao.addEventListener("click", () => {
    if (!window.confirm(`Encerrar "${descreverPromocao(promocao)}" de ${promocao.produtoNome}? O preço volta ao normal agora.`)) {
      return;
    }
    botao.disabled = true;
    encerrarPromocao(promocao.id)
      .then(aoMudar)
      .catch((falha: unknown) => {
        window.alert(falha instanceof Error && falha.message !== "" ? falha.message : "Não foi possível encerrar a promoção.");
        botao.disabled = false;
      });
  });
  return botao;
}

async function abrirFormulario(area: HTMLElement, aoTerminar: () => void): Promise<void> {
  area.replaceChildren(elementoCarregando("Carregando produtos..."));
  let produtos: Produto[];
  try {
    produtos = (await listarProdutos()).filter((produto) => produto.ativo);
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
    return;
  }
  const produto = criarCampoSelecao("promocao-produto", "Produto",
    produtos.map((opcao) => ({ valor: opcao.id, rotulo: `${opcao.nome} — ${formatarMoeda(opcao.precoUnitario)}` })));
  const tipo = criarCampoSelecao("promocao-tipo", "Tipo", TIPOS);
  const precoOferta = criarCampoTexto("promocao-preco", "Preço de oferta (R$)", "number", false);
  precoOferta.entrada.min = "0.01";
  precoOferta.entrada.step = "0.01";
  const leve = criarCampoTexto("promocao-leve", "Leve", "number", false);
  leve.entrada.min = "2";
  leve.entrada.step = "1";
  leve.entrada.value = "3";
  const pague = criarCampoTexto("promocao-pague", "Pague", "number", false);
  pague.entrada.min = "1";
  pague.entrada.step = "1";
  pague.entrada.value = "2";
  const hoje = hojeIso();
  const inicio = criarCampoTexto("promocao-inicio", "Primeiro dia", "date", true);
  inicio.entrada.value = hoje;
  inicio.entrada.min = hoje;
  const fim = criarCampoTexto("promocao-fim", "Último dia", "date", true);
  fim.entrada.min = hoje;

  const camposOferta = document.createElement("div");
  camposOferta.className = "promocoes__linha";
  camposOferta.append(precoOferta.container);
  const camposLevePague = document.createElement("div");
  camposLevePague.className = "promocoes__linha";
  camposLevePague.append(leve.container, pague.container);
  const periodo = document.createElement("div");
  periodo.className = "promocoes__linha";
  periodo.append(inicio.container, fim.container);

  const tipoEscolhido = (): TipoPromocao => (tipo.selecao.value === "LEVE_PAGUE" ? "LEVE_PAGUE" : "PRECO_OFERTA");
  const atualizarTipo = (): void => {
    const oferta = tipoEscolhido() === "PRECO_OFERTA";
    camposOferta.hidden = !oferta;
    camposLevePague.hidden = oferta;
    precoOferta.entrada.required = oferta;
    leve.entrada.required = !oferta;
    pague.entrada.required = !oferta;
  };
  tipo.selecao.addEventListener("change", atualizarTipo);
  atualizarTipo();

  const titulo = document.createElement("h2");
  titulo.textContent = "Nova promoção";
  const erro = criarMensagemErro();
  const salvar = document.createElement("button");
  salvar.type = "submit";
  salvar.className = "btn btn-primary";
  salvar.textContent = "Criar promoção";
  const voltar = document.createElement("button");
  voltar.type = "button";
  voltar.className = "btn btn-ghost";
  voltar.textContent = "Voltar";
  voltar.addEventListener("click", aoTerminar);
  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(voltar, salvar);

  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao promocoes__formulario";
  formulario.append(titulo, produto.container, tipo.container, camposOferta, camposLevePague, periodo, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    salvar.disabled = true;
    const oferta = tipoEscolhido() === "PRECO_OFERTA";
    criarPromocao({
      produtoId: produto.selecao.value,
      tipo: tipoEscolhido(),
      precoOferta: oferta ? Number(precoOferta.entrada.value) : null,
      leve: oferta ? null : Number(leve.entrada.value),
      pague: oferta ? null : Number(pague.entrada.value),
      inicio: inicio.entrada.value,
      fim: fim.entrada.value,
    })
      .then(aoTerminar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível criar a promoção.");
        salvar.disabled = false;
      });
  });
  area.replaceChildren(produtos.length === 0 ? cartaoEstado("Nenhum produto à venda para colocar em promoção.") : formulario);
}

/** "2026-10-10" como data local (meio-dia evita escorregar de dia pelo fuso). */
function dataDe(iso: string): Date {
  return new Date(`${iso}T12:00:00`);
}

function hojeIso(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, "0");
  const dia = String(agora.getDate()).padStart(2, "0");
  return `${agora.getFullYear()}-${mes}-${dia}`;
}
