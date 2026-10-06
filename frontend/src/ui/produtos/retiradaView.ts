import { listarProdutos, registrarPerda, type MotivoPerda, type Produto } from "../../api/produtosApi.js";
import { criarCampoSelecao, criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { criarCampoLeitura } from "../pdv/leituraView.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";
import { ROTULO_MOTIVO_PERDA } from "./motivosPerda.js";

const MOTIVO_ESCRITO: MotivoPerda = "OUTRO";

interface Retirada {
  produto: string;
  quantidade: number;
  motivo: string;
  estoqueDepois: number;
}

/**
 * Retirada de produtos (D37): vencido, avariado, troca com cliente… Leia o código de barras (ou
 * digite o nome), diga quantas unidades saem e o motivo — da lista ou escrito. O estoque baixa na
 * hora e a retirada entra no relatório de perdas do Painel.
 */
export async function montarRetirada(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Retirada de produtos";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);
  const area = document.createElement("div");
  container.replaceChildren(cabecalho, area);

  area.replaceChildren(elementoCarregando("Carregando produtos..."));
  try {
    const produtos = await listarProdutos();
    area.replaceChildren(produtos.length === 0
      ? cartaoEstado("Nenhum produto cadastrado.")
      : montarFormulario(produtos));
  } catch {
    area.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
  }
}

function montarFormulario(produtos: readonly Produto[]): HTMLElement {
  const feitas: Retirada[] = [];
  let escolhido: Produto | null = null;

  const instrucao = document.createElement("p");
  instrucao.className = "caixa-painel__instrucao";
  instrucao.textContent = "Leia o código de barras do produto que vai sair do estoque (vencido, avariado, troca com cliente...). Escolha o motivo na lista ou escreva.";

  const produtoEscolhido = document.createElement("p");
  produtoEscolhido.className = "retirada__produto";
  const quantidade = criarCampoTexto("retirada-quantidade", "Quantidade", "number", true);
  quantidade.entrada.min = "1";
  quantidade.entrada.step = "1";
  quantidade.entrada.value = "1";
  const motivo = criarCampoSelecao("retirada-motivo", "Motivo", Object.entries(ROTULO_MOTIVO_PERDA)
    .map(([valor, rotulo]) => ({ valor, rotulo: valor === MOTIVO_ESCRITO ? "Outro (escrever o motivo)" : rotulo })));
  const escrito = criarCampoTexto("retirada-observacao", "Motivo escrito", "text", false);
  escrito.entrada.maxLength = 200;
  escrito.entrada.placeholder = "Ex.: embalagem estufada";
  const erro = criarMensagemErro();
  const retirar = document.createElement("button");
  retirar.type = "submit";
  retirar.className = "btn btn-primary";
  retirar.textContent = "Retirar do estoque";

  const campos = document.createElement("div");
  campos.className = "retirada__campos";
  campos.append(quantidade.container, motivo.container, escrito.container);
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao retirada";
  formulario.hidden = true;
  formulario.append(produtoEscolhido, campos, erro, retirar);

  const historico = document.createElement("div");

  const atualizarEscrito = (): void => {
    const exige = motivo.selecao.value === MOTIVO_ESCRITO;
    escrito.entrada.required = exige;
    escrito.container.querySelector("label")?.replaceChildren(exige ? "Motivo escrito" : "Observação (opcional)");
  };
  motivo.selecao.addEventListener("change", atualizarEscrito);
  atualizarEscrito();

  const leitura = criarCampoLeitura(produtos, (produto, embalagem) => {
    escolhido = produto;
    erro.hidden = true;
    produtoEscolhido.textContent = `${produto.nome} · ${produto.quantidadeEmEstoque} em estoque`;
    // Leu o código do fardo: já sugere as unidades dele (o estoque é contado em unidades).
    quantidade.entrada.value = String(embalagem?.unidades ?? 1);
    quantidade.entrada.max = String(produto.quantidadeEmEstoque);
    formulario.hidden = false;
    quantidade.entrada.focus();
    quantidade.entrada.select();
  });

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    if (escolhido === null) {
      return;
    }
    const produto = escolhido;
    const unidades = Number(quantidade.entrada.value);
    if (!Number.isInteger(unidades) || unidades < 1) {
      mostrarErro(erro, null, "Digite quantas unidades saem (1 ou mais).");
      return;
    }
    // Valor vem de uma lista fixa de opções (ROTULO_MOTIVO_PERDA), então sempre é um motivo válido.
    const motivoEscolhido = motivo.selecao.value as MotivoPerda;
    const texto = escrito.entrada.value.trim();
    retirar.disabled = true;
    erro.hidden = true;
    registrarPerda(produto.id, unidades, motivoEscolhido, texto === "" ? null : texto)
      .then((atualizado) => {
        feitas.unshift({
          produto: atualizado.nome,
          quantidade: unidades,
          motivo: motivoEscolhido === MOTIVO_ESCRITO ? texto : ROTULO_MOTIVO_PERDA[motivoEscolhido],
          estoqueDepois: atualizado.quantidadeEmEstoque,
        });
        escolhido = null;
        formulario.hidden = true;
        escrito.entrada.value = "";
        historico.replaceChildren(criarHistorico(feitas));
        leitura.focar();
      })
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível retirar do estoque."))
      .finally(() => {
        retirar.disabled = false;
      });
  });

  const pagina = document.createElement("div");
  pagina.className = "retirada__pagina";
  pagina.append(instrucao, leitura.elemento, formulario, historico);
  queueMicrotask(() => leitura.focar());
  return pagina;
}

function criarHistorico(feitas: readonly Retirada[]): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.className = "retirada__titulo-historico";
  titulo.textContent = "Retiradas feitas agora";
  const tabela = criarTabela(
    ["Produto", "Quantidade", "Motivo", "Estoque depois"],
    feitas.map((retirada) => criarLinha(
      celula(retirada.produto),
      celula(String(retirada.quantidade)),
      celula(retirada.motivo),
      celula(String(retirada.estoqueDepois))
    )),
    "retirada(s)"
  );
  const bloco = document.createElement("section");
  bloco.append(titulo, tabela);
  return bloco;
}
