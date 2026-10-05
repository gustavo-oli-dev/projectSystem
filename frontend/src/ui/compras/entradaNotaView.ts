import {
  confirmarNota,
  listarNotasLancadas,
  preVisualizarNota,
  type ItemDaNota,
  type NotaEntrada,
  type PreVisualizacaoNota,
} from "../../api/comprasApi.js";
import { listarProdutos, type Produto } from "../../api/produtosApi.js";
import { criarMensagemErro, criarSelecao, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";

const IGNORAR = "";
const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });
const DATA_HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const CUSTO = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL", minimumFractionDigits: 2, maximumFractionDigits: 4 });

interface EscolhaItem {
  item: ItemDaNota;
  produto: HTMLSelectElement;
}

/**
 * Entrada por nota (D34): escolha o XML que o fornecedor mandou, confira o que veio (fornecedor,
 * produtos reconhecidos pelo código de barras, custos, parcelas) e dê entrada. O estoque sobe, o
 * custo pode ser atualizado e as parcelas viram contas a pagar.
 */
export async function montarEntradaNota(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Entrada por nota";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);
  const area = document.createElement("div");
  const ultimas = document.createElement("div");
  container.replaceChildren(cabecalho, area, ultimas);

  const recomecar = (): void => {
    area.replaceChildren(criarEscolhaDoArquivo(area, recomecar));
    void carregarUltimas(ultimas);
  };
  recomecar();
}

function criarEscolhaDoArquivo(area: HTMLElement, recomecar: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = "1. Escolha o XML da nota do fornecedor";
  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = "É o arquivo .xml que o fornecedor manda por e-mail junto com a nota (não o PDF/DANFE). Nada é lançado agora: primeiro você confere tudo.";
  const arquivo = document.createElement("input");
  arquivo.type = "file";
  arquivo.accept = ".xml,application/xml,text/xml";
  arquivo.className = "entrada-nota__arquivo";
  arquivo.setAttribute("aria-label", "Arquivo XML da nota");
  const erro = criarMensagemErro();
  arquivo.addEventListener("change", () => {
    const escolhido = arquivo.files?.[0];
    if (escolhido === undefined) {
      return;
    }
    erro.hidden = true;
    area.replaceChildren(elementoCarregando("Lendo a nota..."));
    Promise.all([preVisualizarNota(escolhido), listarProdutos()])
      .then(([previa, produtos]) => area.replaceChildren(criarConferencia(escolhido, previa, produtos.filter((p) => p.ativo), recomecar)))
      .catch((falha: unknown) => {
        const painel = criarEscolhaDoArquivo(area, recomecar);
        area.replaceChildren(painel);
        const mensagem = painel.querySelector<HTMLElement>(".estado-erro");
        if (mensagem !== null) {
          mostrarErro(mensagem, falha, "Não foi possível ler a nota.");
        }
      });
  });
  const painel = document.createElement("div");
  painel.className = "caixa-painel";
  painel.append(titulo, instrucao, arquivo, erro);
  return painel;
}

function criarConferencia(arquivo: File, previa: PreVisualizacaoNota, produtos: readonly Produto[], recomecar: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = `2. Confira a NF-e nº ${previa.numero} de ${previa.fornecedorNome}`;

  const painel = document.createElement("div");
  painel.className = "caixa-painel";
  painel.append(titulo, criarResumoNota(previa));

  if (previa.jaLancadaEm !== null) {
    const aviso = document.createElement("p");
    aviso.className = "resultado-caixa__situacao resultado-caixa__situacao--falta";
    aviso.textContent = `Esta nota já entrou no estoque em ${DATA_HORA.format(new Date(previa.jaLancadaEm))} (por ${previa.jaLancadaPor ?? "—"}). Ela não pode entrar de novo.`;
    painel.append(aviso, acoes(botao("Escolher outra nota", "btn btn-primary", recomecar)));
    return painel;
  }

  const escolhas: EscolhaItem[] = [];
  const tabela = criarTabela(
    ["#", "Na nota", "Código de barras", "Quantidade", "Custo unitário", "Produto no sistema"],
    previa.itens.map((item) => {
      const produto = criarSelecao([
        { valor: IGNORAR, rotulo: "— Ignorar este item —" },
        ...produtos.map((opcao) => ({ valor: opcao.id, rotulo: opcao.nome })),
      ]);
      produto.value = item.produtoSugeridoId ?? IGNORAR;
      produto.setAttribute("aria-label", `Produto do sistema para ${item.descricao}`);
      produto.className = "entrada-nota__produto";
      produto.disabled = !item.quantidadeInteira;
      escolhas.push({ item, produto });
      return criarLinha(
        celula(String(item.ordem)),
        celula(item.descricao),
        celula(item.codigoBarras ?? "sem código"),
        item.quantidadeInteira
          ? celula(`${formatarInteiro(item.quantidade)} ${item.unidade}`)
          : celulaSelo(`${item.quantidade.toLocaleString("pt-BR")} ${item.unidade} — fracionado, lance à mão`, "estoque_baixo"),
        celula(CUSTO.format(item.custoUnitario)),
        celulaComConteudo(produto)
      );
    }),
    "item(ns) na nota"
  );
  const legenda = document.createElement("p");
  legenda.className = "caixa-painel__instrucao";
  legenda.textContent = "Os produtos com o mesmo código de barras já vêm escolhidos. Escolha o produto dos outros ou deixe \"Ignorar\" (não entra no estoque).";

  const atualizarCusto = document.createElement("input");
  atualizarCusto.type = "checkbox";
  atualizarCusto.id = "entrada-atualizar-custo";
  atualizarCusto.checked = true;
  const rotuloCusto = document.createElement("label");
  rotuloCusto.htmlFor = atualizarCusto.id;
  rotuloCusto.textContent = "Atualizar o custo dos produtos com o custo desta nota (o lucro do Painel passa a usar este custo)";
  const opcaoCusto = document.createElement("div");
  opcaoCusto.className = "entrada-nota__opcao";
  opcaoCusto.append(atualizarCusto, rotuloCusto);

  const erro = criarMensagemErro();
  const confirmar = botao("Dar entrada no estoque", "btn btn-primary", () => {
    erro.hidden = true;
    const ligados = escolhas
      .filter((escolha) => escolha.produto.value !== IGNORAR)
      .map((escolha) => ({ ordem: escolha.item.ordem, produtoId: escolha.produto.value }));
    if (ligados.length === 0) {
      mostrarErro(erro, null, "Escolha o produto de ao menos um item.");
      return;
    }
    confirmar.disabled = true;
    confirmarNota(arquivo, ligados, atualizarCusto.checked)
      .then((nota) => painel.replaceChildren(criarConcluido(nota, previa, recomecar)))
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível dar entrada na nota.");
        confirmar.disabled = false;
      });
  });

  painel.append(legenda, tabela, criarParcelas(previa), opcaoCusto, erro,
    acoes(botao("Escolher outra nota", "btn btn-ghost", recomecar), confirmar));
  return painel;
}

function criarResumoNota(previa: PreVisualizacaoNota): HTMLElement {
  const linhas: Array<[string, string]> = [
    ["Fornecedor", `${previa.fornecedorNome} · ${previa.fornecedorDocumento}${previa.fornecedorCadastradoId === null ? " (novo — será cadastrado em Contatos)" : ""}`],
    ["Emissão", DATA.format(new Date(previa.emitidaEm))],
    ["Valor total da nota", formatarMoeda(previa.valorTotal)],
    ["Chave de acesso", previa.chaveAcesso],
  ];
  const lista = document.createElement("dl");
  lista.className = "resultado-caixa__conta";
  for (const [rotulo, valor] of linhas) {
    const termo = document.createElement("dt");
    termo.textContent = rotulo;
    const definicao = document.createElement("dd");
    definicao.textContent = valor;
    const linha = document.createElement("div");
    linha.className = "resultado-caixa__linha";
    linha.append(termo, definicao);
    lista.append(linha);
  }
  return lista;
}

function criarParcelas(previa: PreVisualizacaoNota): HTMLElement {
  const titulo = document.createElement("h3");
  titulo.className = "abertura-caixas__titulo";
  titulo.textContent = "Vira conta a pagar";
  const secao = document.createElement("section");
  secao.className = "caixa-painel__secao";
  if (previa.parcelas.length === 0) {
    const texto = document.createElement("p");
    texto.className = "caixa-painel__instrucao";
    texto.textContent = `A nota não tem parcelas: entra uma conta de ${formatarMoeda(previa.valorTotal)} com vencimento na emissão (dê baixa em Contas a pagar se já foi paga).`;
    secao.append(titulo, texto);
    return secao;
  }
  secao.append(titulo, criarTabela(["Parcela", "Vencimento", "Valor"], previa.parcelas.map((parcela) => criarLinha(
    celula(parcela.numero), celula(DATA.format(new Date(`${parcela.vencimento}T12:00:00`))), celula(formatarMoeda(parcela.valor))
  )), "parcela(s)"));
  return secao;
}

function criarConcluido(nota: NotaEntrada, previa: PreVisualizacaoNota, recomecar: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = `NF-e nº ${nota.numero} lançada`;
  const situacao = document.createElement("p");
  situacao.className = "resultado-caixa__situacao resultado-caixa__situacao--bateu";
  situacao.textContent = `✓ ${formatarInteiro(nota.unidades)} unidade(s) de ${formatarInteiro(nota.itens)} produto(s) entraram no estoque · `
    + `${Math.max(previa.parcelas.length, 1)} conta(s) a pagar lançada(s) para ${nota.fornecedorNome}`;
  const pagina = document.createElement("div");
  pagina.className = "caixa-painel__secao";
  pagina.append(titulo, situacao, acoes(botao("Lançar outra nota", "btn btn-primary", recomecar)));
  return pagina;
}

async function carregarUltimas(area: HTMLElement): Promise<void> {
  try {
    const notas = await listarNotasLancadas();
    if (notas.length === 0) {
      area.replaceChildren();
      return;
    }
    const titulo = document.createElement("h2");
    titulo.className = "dinheiro-do-dia__titulo entrada-nota__titulo-lista";
    titulo.textContent = "Notas lançadas";
    area.replaceChildren(titulo, criarTabela(
      ["Lançada em", "NF-e", "Fornecedor", "Produtos", "Unidades", "Valor da nota", "Por"],
      notas.map((nota) => criarLinha(
        celula(DATA_HORA.format(new Date(nota.registradaEm))),
        celula(nota.numero),
        celula(nota.fornecedorNome),
        celula(formatarInteiro(nota.itens)),
        celula(formatarInteiro(nota.unidades)),
        celula(formatarMoeda(nota.valorTotal)),
        celula(nota.registradaPor.split("@")[0] ?? nota.registradaPor)
      )),
      "nota(s)"
    ));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar as notas lançadas.", "erro"));
  }
}

function acoes(...botoes: HTMLElement[]): HTMLElement {
  const elemento = document.createElement("div");
  elemento.className = "caixa-painel__acoes";
  elemento.append(...botoes);
  return elemento;
}

function botao(rotulo: string, classe: string, aoClicar: () => void): HTMLButtonElement {
  const elemento = document.createElement("button");
  elemento.type = "button";
  elemento.className = classe;
  elemento.textContent = rotulo;
  elemento.addEventListener("click", aoClicar);
  return elemento;
}
