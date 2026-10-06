import {
  cadastrarProduto,
  darEntradaEmLote,
  listarProdutos,
  type Embalagem,
  type Produto,
} from "../../api/produtosApi.js";
import { possui } from "../../state/sessaoState.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, celulaComConteudo, criarLinha, criarTabela } from "../tabela.js";

/** Leitor USB/Bluetooth "digita" o código e aperta Enter. */
const SO_DIGITOS = /^\d{8,14}$/;
const MAXIMO_SUGESTOES = 8;
const UNIDADE_PADRAO = "UN";

interface Lida {
  produto: Produto;
  unidades: number;
}

/**
 * Entrada pelo leitor (D46): leia tudo o que chegou, misturado. Cada leitura soma no produto (o
 * fardo soma as unidades dele). Código que não existe abre o cadastro rápido já com o código.
 * "Confirmar" dá entrada em tudo de uma vez.
 */
export async function montarEntradaLeitor(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Entrada pelo leitor";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, area);

  const carregar = async (): Promise<void> => {
    area.replaceChildren(elementoCarregando("Carregando produtos..."));
    try {
      area.replaceChildren(montarTela(await listarProdutos(), () => void carregar()));
    } catch {
      area.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
    }
  };
  await carregar();
}

function montarTela(produtos: Produto[], recomecar: () => void): HTMLElement {
  const porCodigo = new Map<string, { produto: Produto; embalagem: Embalagem | null }>();
  const indexar = (produto: Produto): void => {
    if (produto.codigoBarras !== null) {
      porCodigo.set(produto.codigoBarras, { produto, embalagem: null });
    }
    produto.embalagens.forEach((embalagem) => {
      if (embalagem.codigoBarras !== null) {
        porCodigo.set(embalagem.codigoBarras, { produto, embalagem });
      }
    });
  };
  produtos.forEach(indexar);
  const lidas = new Map<string, Lida>();

  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = "Clique no campo e leia cada unidade que chegou, de qualquer produto. Fardo soma as unidades dele. Produto sem código: digite o nome. Ao terminar, confira a lista e confirme.";

  const entrada = document.createElement("input");
  entrada.type = "text";
  entrada.className = "leitura__campo";
  entrada.placeholder = "Leia o código de barras ou digite o nome do produto";
  entrada.setAttribute("aria-label", "Código de barras ou nome do produto");
  entrada.autocomplete = "off";
  const aviso = document.createElement("p");
  aviso.className = "leitura__aviso";
  aviso.setAttribute("role", "status");
  const sugestoes = document.createElement("div");
  sugestoes.className = "leitura__sugestoes";
  const cadastro = document.createElement("div");
  const lista = document.createElement("div");
  const erro = criarMensagemErro();
  const confirmar = document.createElement("button");
  confirmar.type = "button";
  confirmar.className = "btn btn-primary";

  const somar = (produto: Produto, unidades: number): void => {
    const atual = lidas.get(produto.id);
    lidas.set(produto.id, { produto, unidades: (atual?.unidades ?? 0) + unidades });
    aviso.textContent = `+${unidades} ${produto.nome}`;
    renderizar();
  };

  const renderizar = (): void => {
    const itens = [...lidas.values()];
    const total = itens.reduce((soma, item) => soma + item.unidades, 0);
    confirmar.textContent = `Confirmar entrada de ${total} unidade(s)`;
    confirmar.disabled = itens.length === 0;
    lista.replaceChildren(itens.length === 0
      ? cartaoEstado("Nenhum produto lido ainda.")
      : criarTabela(["Produto", "Lidas", "Estoque agora", "Estoque depois", ""], itens.map((item) => criarLinha(
        celula(item.produto.nome),
        celula(String(item.unidades)),
        celula(String(item.produto.quantidadeEmEstoque)),
        celula(String(item.produto.quantidadeEmEstoque + item.unidades)),
        celulaComConteudo(criarAcoesDaLinha(item, (nova) => {
          if (nova <= 0) {
            lidas.delete(item.produto.id);
          } else {
            lidas.set(item.produto.id, { ...item, unidades: nova });
          }
          renderizar();
          entrada.focus();
        }))
      )), "produto(s)"));
  };

  const abrirCadastro = (codigo: string): void => {
    if (!possui("CATALOGO_GERENCIAR")) {
      aviso.textContent = `Código ${codigo} não cadastrado. Peça a quem cadastra produtos para cadastrá-lo.`;
      return;
    }
    aviso.textContent = `Código ${codigo} não cadastrado: cadastre o produto abaixo para contar.`;
    cadastro.replaceChildren(criarCadastroRapido(codigo, (novo) => {
      produtos.push(novo);
      indexar(novo);
      cadastro.replaceChildren();
      somar(novo, 1);
      entrada.focus();
    }, () => {
      cadastro.replaceChildren();
      entrada.focus();
    }));
  };

  entrada.addEventListener("input", () => {
    const termo = entrada.value.trim().toLowerCase();
    if (termo.length < 2 || SO_DIGITOS.test(termo)) {
      sugestoes.replaceChildren();
      return;
    }
    const achados = produtos.filter((produto) => produto.ativo && produto.nome.toLowerCase().includes(termo)).slice(0, MAXIMO_SUGESTOES);
    sugestoes.replaceChildren(...achados.map((produto) => {
      const botao = document.createElement("button");
      botao.type = "button";
      botao.className = "leitura__sugestao";
      botao.textContent = produto.nome;
      botao.addEventListener("click", () => {
        sugestoes.replaceChildren();
        entrada.value = "";
        somar(produto, 1);
        entrada.focus();
      });
      return botao;
    }));
  });

  entrada.addEventListener("keydown", (evento) => {
    if (evento.key !== "Enter") {
      return;
    }
    evento.preventDefault();
    const codigo = entrada.value.trim();
    if (!SO_DIGITOS.test(codigo)) {
      sugestoes.querySelector<HTMLButtonElement>("button")?.click();
      return;
    }
    entrada.value = "";
    const achado = porCodigo.get(codigo);
    if (achado === undefined) {
      abrirCadastro(codigo);
      return;
    }
    somar(achado.produto, achado.embalagem?.unidades ?? 1);
  });

  confirmar.addEventListener("click", () => {
    erro.hidden = true;
    confirmar.disabled = true;
    const itens = [...lidas.values()].map((item) => ({ produtoId: item.produto.id, quantidade: item.unidades }));
    darEntradaEmLote(itens)
      .then((atualizados) => {
        window.alert(`Entrada feita em ${atualizados.length} produto(s).`);
        recomecar();
      })
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível dar entrada no estoque.");
        confirmar.disabled = false;
      });
  });

  renderizar();
  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(confirmar);
  const leitura = document.createElement("div");
  leitura.className = "leitura";
  leitura.append(entrada, sugestoes, aviso);
  const tela = document.createElement("div");
  tela.className = "entrada-leitor";
  tela.append(instrucao, leitura, cadastro, lista, erro, acoes);
  queueMicrotask(() => entrada.focus());
  return tela;
}

/** − e "tirar": corrige uma leitura a mais sem recomeçar. */
function criarAcoesDaLinha(item: Lida, aoMudar: (novaQuantidade: number) => void): HTMLElement {
  const menos = document.createElement("button");
  menos.type = "button";
  menos.className = "btn btn-ghost btn-pequeno";
  menos.textContent = "−1";
  menos.setAttribute("aria-label", `Tirar uma unidade de ${item.produto.nome}`);
  menos.addEventListener("click", () => aoMudar(item.unidades - 1));
  const tirar = document.createElement("button");
  tirar.type = "button";
  tirar.className = "link-tabela";
  tirar.textContent = "tirar";
  tirar.addEventListener("click", () => aoMudar(0));
  const acoes = document.createElement("div");
  acoes.className = "entrada-leitor__acoes";
  acoes.append(menos, tirar);
  return acoes;
}

/** Cadastro rápido do produto que chegou sem cadastro: o código lido já vem preenchido. */
function criarCadastroRapido(codigo: string, aoCadastrar: (produto: Produto) => void, aoCancelar: () => void): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.textContent = `Cadastrar produto ${codigo}`;
  const nome = criarCampoTexto("rapido-nome", "Nome", "text", true);
  const ncm = criarCampoTexto("rapido-ncm", "NCM (8 dígitos, está na nota)", "text", true);
  ncm.entrada.inputMode = "numeric";
  ncm.entrada.maxLength = 8;
  const unidade = criarCampoTexto("rapido-unidade", "Unidade", "text", true);
  unidade.entrada.value = UNIDADE_PADRAO;
  unidade.entrada.maxLength = 6;
  const preco = criarCampoTexto("rapido-preco", "Preço de venda (R$)", "number", true);
  preco.entrada.min = "0.01";
  preco.entrada.step = "0.01";
  const erro = criarMensagemErro();
  const salvar = document.createElement("button");
  salvar.type = "submit";
  salvar.className = "btn btn-primary";
  salvar.textContent = "Cadastrar e contar";
  const cancelar = document.createElement("button");
  cancelar.type = "button";
  cancelar.className = "btn btn-ghost";
  cancelar.textContent = "Cancelar";
  cancelar.addEventListener("click", aoCancelar);
  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(cancelar, salvar);
  const campos = document.createElement("div");
  campos.className = "entrada-leitor__campos";
  campos.append(nome.container, ncm.container, unidade.container, preco.container);

  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao entrada-leitor__cadastro";
  formulario.append(titulo, campos, erro, acoes);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    salvar.disabled = true;
    cadastrarProduto({
      nome: nome.entrada.value.trim(),
      descricao: null,
      ncm: ncm.entrada.value.trim(),
      unidadeMedida: unidade.entrada.value.trim(),
      precoUnitario: Number(preco.entrada.value),
      codigoBarras: codigo,
      custoUnitario: null,
    })
      .then(aoCadastrar)
      .catch((falha: unknown) => {
        mostrarErro(erro, falha, "Não foi possível cadastrar o produto.");
        salvar.disabled = false;
      });
  });
  queueMicrotask(() => nome.entrada.focus());
  return formulario;
}
