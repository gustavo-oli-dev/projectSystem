import { cadastrarServico, listarServicos, type NovoServico, type Servico } from "../../api/servicosApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";

interface Campo {
  container: HTMLDivElement;
  entrada: HTMLInputElement;
}

export async function montarListaServicos(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Serviços";

  const botaoNovo = document.createElement("button");
  botaoNovo.type = "button";
  botaoNovo.className = "btn btn-primary btn-pequeno";
  botaoNovo.hidden = !possui("CATALOGO_GERENCIAR");
  botaoNovo.textContent = "+ Novo serviço";

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, botaoNovo);

  const areaFormulario = document.createElement("div");
  const areaLista = document.createElement("div");

  container.replaceChildren(cabecalho, areaFormulario, areaLista);

  botaoNovo.addEventListener("click", () => {
    if (areaFormulario.childElementCount > 0) {
      areaFormulario.replaceChildren();
      return;
    }
    areaFormulario.append(
      criarFormulario(async (novoServico) => {
        await cadastrarServico(novoServico);
        areaFormulario.replaceChildren();
        await carregar(areaLista);
      })
    );
  });

  await carregar(areaLista);
}

async function carregar(areaLista: HTMLElement): Promise<void> {
  areaLista.replaceChildren(elementoCarregando("Carregando serviços..."));
  try {
    const servicos = await listarServicos();
    renderizarLista(areaLista, servicos);
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os serviços.", "erro"));
  }
}

function renderizarLista(areaLista: HTMLElement, servicos: Servico[]): void {
  if (servicos.length === 0) {
    areaLista.replaceChildren(cartaoEstado("Nenhum serviço cadastrado ainda."));
    return;
  }

  const linhas = servicos.map((servico) =>
    criarLinha(
      celula(servico.nome),
      celula(servico.codigoServicoLc116),
      celula(`${servico.aliquotaIss}%`),
      celula(formatarMoeda(servico.precoUnitario)),
      celula(servico.ativo ? "Sim" : "Não")
    )
  );

  areaLista.replaceChildren(
    criarTabela(["Nome", "Código LC 116", "Alíquota ISS", "Preço", "Ativo"], linhas, "serviço(s)")
  );
}

function criarFormulario(aoSalvar: (servico: NovoServico) => Promise<void>): HTMLFormElement {
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao";

  const nome = criarCampo("servico-nome", "Nome", "text", true);
  const descricao = criarCampo("servico-descricao", "Descrição", "text", false);
  const codigo = criarCampo("servico-codigo", "Código LC 116/2003 (formato NN.NN)", "text", true);
  const aliquota = criarCampo("servico-aliquota", "Alíquota de ISS (entre 2 e 5)", "number", true);
  aliquota.entrada.step = "0.01";
  aliquota.entrada.min = "2";
  aliquota.entrada.max = "5";
  const preco = criarCampo("servico-preco", "Preço unitário", "number", true);
  preco.entrada.step = "0.01";
  preco.entrada.min = "0";

  const erro = document.createElement("p");
  erro.className = "estado-erro";
  erro.hidden = true;

  const botaoSalvar = document.createElement("button");
  botaoSalvar.type = "submit";
  botaoSalvar.className = "btn btn-primary btn-pequeno";
  botaoSalvar.textContent = "Salvar";

  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(botaoSalvar);

  formulario.append(
    nome.container,
    descricao.container,
    codigo.container,
    aliquota.container,
    preco.container,
    erro,
    acoes
  );

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    botaoSalvar.disabled = true;

    aoSalvar({
      nome: nome.entrada.value,
      descricao: descricao.entrada.value.trim() === "" ? null : descricao.entrada.value,
      codigoServicoLc116: codigo.entrada.value,
      aliquotaIss: Number(aliquota.entrada.value),
      precoUnitario: Number(preco.entrada.value),
    })
      .catch(() => {
        erro.textContent = "Não foi possível salvar o serviço. Confira os dados e tente novamente.";
        erro.hidden = false;
      })
      .finally(() => {
        botaoSalvar.disabled = false;
      });
  });

  return formulario;
}

function criarCampo(id: string, rotuloTexto: string, tipo: string, obrigatorio: boolean): Campo {
  const container = document.createElement("div");
  container.className = "campo-formulario";

  const rotulo = document.createElement("label");
  rotulo.htmlFor = id;
  rotulo.textContent = rotuloTexto;

  const entrada = document.createElement("input");
  entrada.id = id;
  entrada.type = tipo;
  entrada.required = obrigatorio;

  container.append(rotulo, entrada);
  return { container, entrada };
}
