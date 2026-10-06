import { definirTributacao, type Produto } from "../../api/produtosApi.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro, textoOuNulo } from "../camposFormulario.js";
import { criarSecao } from "./secaoEdicao.js";

/** Tabela de origem da mercadoria da NF-e (0 a 8). */
const ORIGENS: ReadonlyArray<{ valor: string; rotulo: string }> = [
  { valor: "0", rotulo: "0 — Nacional" },
  { valor: "1", rotulo: "1 — Estrangeira, importação direta" },
  { valor: "2", rotulo: "2 — Estrangeira, comprada no mercado interno" },
  { valor: "3", rotulo: "3 — Nacional, com mais de 40% de conteúdo importado" },
  { valor: "4", rotulo: "4 — Nacional, processo produtivo básico" },
  { valor: "5", rotulo: "5 — Nacional, até 40% de conteúdo importado" },
  { valor: "6", rotulo: "6 — Estrangeira, importação direta, sem similar nacional" },
  { valor: "7", rotulo: "7 — Estrangeira, mercado interno, sem similar nacional" },
  { valor: "8", rotulo: "8 — Nacional, mais de 70% de conteúdo importado" },
];

/**
 * Tributação do produto na nota (D42). Sem ela a NFC-e não pode ser transmitida. CST (2 dígitos,
 * regime normal) ou CSOSN (3 dígitos, Simples Nacional) — o regime da empresa ainda está em aberto.
 */
export function criarSecaoTributacao(produto: Produto, recarregar: () => Promise<void>): HTMLElement {
  const atual = produto.tributacao;
  const situacao = document.createElement("p");
  situacao.className = atual === null ? "nota-campo tributacao__pendente" : "nota-campo";
  situacao.textContent = atual === null
    ? "Tributação ainda não definida: a NFC-e deste produto não poderá ser transmitida."
    : `Definida · ${atual.simplesNacional ? "Simples Nacional (CSOSN)" : "regime normal (CST)"}`;

  const origem = criarCampoSelecao("tributacao-origem", "Origem da mercadoria", ORIGENS);
  origem.selecao.value = String(atual?.origem ?? 0);
  const cst = criarCampoTexto("tributacao-cst", "CST (2 dígitos) ou CSOSN (3)", "text", true);
  cst.entrada.inputMode = "numeric";
  cst.entrada.maxLength = 3;
  cst.entrada.value = atual?.cstIcms ?? "";
  const aliquota = criarCampoTexto("tributacao-aliquota", "Alíquota de ICMS (%)", "number", false);
  aliquota.entrada.min = "0";
  aliquota.entrada.max = "100";
  aliquota.entrada.step = "0.01";
  aliquota.entrada.value = atual === null || atual.aliquotaIcms === null ? "" : String(atual.aliquotaIcms);
  const classificacao = criarCampoTexto("tributacao-classificacao", "Classificação IBS/CBS (cClassTrib)", "text", false);
  classificacao.entrada.inputMode = "numeric";
  classificacao.entrada.maxLength = 6;
  classificacao.entrada.value = atual?.classificacaoTributaria ?? "";
  const cest = criarCampoTexto("tributacao-cest", "CEST (obrigatório com ST)", "text", false);
  cest.entrada.inputMode = "numeric";
  cest.entrada.maxLength = 7;
  cest.entrada.value = atual?.cest ?? "";
  const substituicao = criarMarcacao("tributacao-st", "Substituição tributária (ICMS já recolhido antes)", atual?.substituicaoTributaria ?? false);
  const cestaBasica = criarMarcacao("tributacao-cesta", "Cesta básica (alíquota zero de IBS/CBS — LC 214/2025)", atual?.cestaBasica ?? false);

  const campos = document.createElement("div");
  campos.className = "tributacao__campos";
  campos.append(origem.container, cst.container, aliquota.container, classificacao.container, cest.container);
  const marcacoes = document.createElement("div");
  marcacoes.className = "tributacao__marcacoes";
  marcacoes.append(substituicao.rotulo, cestaBasica.rotulo);
  const erro = criarMensagemErro();
  const salvar = document.createElement("button");
  salvar.type = "submit";
  salvar.className = "btn btn-primary";
  salvar.textContent = "Salvar tributação";

  const formulario = document.createElement("form");
  formulario.className = "tributacao";
  formulario.append(situacao, campos, marcacoes, erro, salvar);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    salvar.disabled = true;
    definirTributacao(produto.id, {
      origem: Number(origem.selecao.value),
      cstIcms: cst.entrada.value.trim(),
      aliquotaIcms: aliquota.entrada.value === "" ? null : Number(aliquota.entrada.value),
      substituicaoTributaria: substituicao.caixa.checked,
      cest: textoOuNulo(cest.entrada.value),
      cestaBasica: cestaBasica.caixa.checked,
      classificacaoTributaria: textoOuNulo(classificacao.entrada.value),
    })
      .then(() => recarregar())
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível salvar a tributação.");
        salvar.disabled = false;
      });
  });
  return criarSecao("Tributação (nota fiscal)", formulario);
}

function criarMarcacao(id: string, texto: string, marcada: boolean): { rotulo: HTMLLabelElement; caixa: HTMLInputElement } {
  const caixa = document.createElement("input");
  caixa.type = "checkbox";
  caixa.id = id;
  caixa.checked = marcada;
  const rotulo = document.createElement("label");
  rotulo.className = "tributacao__marcacao";
  rotulo.htmlFor = id;
  rotulo.append(caixa, document.createTextNode(texto));
  return { rotulo, caixa };
}
