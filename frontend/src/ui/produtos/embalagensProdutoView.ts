import { adicionarEmbalagem, removerEmbalagem, type Produto } from "../../api/produtosApi.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaComConteudo, criarLinha, criarTabela } from "../tabela.js";
import { criarSecao } from "./secaoEdicao.js";

/**
 * Embalagens do produto (D41): vender a unidade ou a embalagem (ex.: fardo com 12, com preço e
 * código de barras próprios). O estoque continua em unidades — vender 1 fardo baixa 12.
 */
export function criarSecaoEmbalagens(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const explicacao = document.createElement("p");
  explicacao.className = "nota-campo";
  explicacao.textContent = `Para vender também em quantidade (ex.: fardo com 12). O estoque continua em ${produto.unidadeMedida}: vender 1 fardo de 12 tira 12 do estoque.`;

  const lista = produto.embalagens.length === 0
    ? cartaoEstado("Nenhuma embalagem: o produto é vendido só por unidade.")
    : criarTabela(["Embalagem", "Unidades", "Preço", "Código de barras", ""],
      produto.embalagens.map((embalagem) => criarLinha(
        celula(embalagem.nome),
        celula(String(embalagem.unidades)),
        celula(`${formatarMoeda(embalagem.preco)} (${formatarMoeda(embalagem.preco / embalagem.unidades)} cada)`),
        celula(embalagem.codigoBarras ?? "—"),
        celulaComConteudo(criarBotaoRemover(produto, embalagem.id, embalagem.nome, recarregar))
      )), "embalagem(ns)");

  const nome = criarCampoTexto("embalagem-nome", "Nome (ex.: Fardo com 12)", "text", true);
  nome.entrada.maxLength = 60;
  const unidades = criarCampoTexto("embalagem-unidades", "Unidades", "number", true);
  unidades.entrada.min = "2";
  unidades.entrada.step = "1";
  const preco = criarCampoTexto("embalagem-preco", "Preço (R$)", "number", true);
  preco.entrada.min = "0.01";
  preco.entrada.step = "0.01";
  const codigo = criarCampoTexto("embalagem-codigo", "Código de barras", "text", false);
  codigo.entrada.inputMode = "numeric";
  codigo.entrada.maxLength = 14;
  const erro = criarMensagemErro();
  const adicionar = document.createElement("button");
  adicionar.type = "submit";
  adicionar.className = "btn btn-primary";
  adicionar.textContent = "Adicionar embalagem";

  const campos = document.createElement("div");
  campos.className = "embalagens__campos";
  campos.append(nome.container, unidades.container, preco.container, codigo.container);
  const formulario = document.createElement("form");
  formulario.className = "embalagens__formulario";
  formulario.append(campos, erro, adicionar);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    adicionar.disabled = true;
    adicionarEmbalagem(produto.id, {
      nome: nome.entrada.value.trim(),
      unidades: Number(unidades.entrada.value),
      preco: Number(preco.entrada.value),
      codigoBarras: textoOuNulo(codigo.entrada.value),
    })
      .then(() => recarregar())
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível adicionar a embalagem.");
        adicionar.disabled = false;
      });
  });

  return criarSecao("Embalagens", explicacao, lista, formulario);
}

function criarBotaoRemover(produto: Produto, embalagemId: string, nome: string, recarregar: () => Promise<void>): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "link-tabela";
  botao.textContent = "Remover";
  botao.addEventListener("click", () => {
    if (!window.confirm(`Parar de vender "${produto.nome} — ${nome}"? As vendas já feitas continuam registradas.`)) {
      return;
    }
    botao.disabled = true;
    removerEmbalagem(produto.id, embalagemId)
      .then(() => recarregar())
      .catch((falha: unknown) => {
        window.alert(falha instanceof Error && falha.message !== "" ? falha.message : "Não foi possível remover a embalagem.");
        botao.disabled = false;
      });
  });
  return botao;
}
