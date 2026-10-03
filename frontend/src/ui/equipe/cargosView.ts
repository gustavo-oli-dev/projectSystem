import {
  atualizarCargo,
  criarCargo,
  excluirCargo,
  listarCargos,
  listarPermissoesDisponiveis,
  type Cargo,
  type DadosCargo,
  type PermissaoDisponivel,
} from "../../api/cargosApi.js";
import type { Permissao } from "../../api/sessaoApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, celulaComConteudo, criarLinha, criarTabela } from "../tabela.js";
import { criarCampoTexto, criarMensagemErro, mostrarErro } from "../camposFormulario.js";

interface Contexto {
  areaFormulario: HTMLElement;
  areaLista: HTMLElement;
  erroAcao: HTMLParagraphElement;
  catalogo: PermissaoDisponivel[];
}

export async function montarAbaCargos(container: HTMLElement): Promise<void> {
  const botaoNovo = document.createElement("button");
  botaoNovo.type = "button";
  botaoNovo.className = "btn btn-primary btn-pequeno";
  botaoNovo.textContent = "+ Novo perfil";

  const barra = document.createElement("div");
  barra.className = "barra-aba";
  barra.append(botaoNovo);

  const areaFormulario = document.createElement("div");
  const erroAcao = criarMensagemErro();
  const areaLista = document.createElement("div");
  container.replaceChildren(barra, areaFormulario, erroAcao, areaLista);
  areaLista.append(elementoCarregando("Carregando perfis de acesso..."));

  let catalogo: PermissaoDisponivel[];
  try {
    catalogo = await listarPermissoesDisponiveis();
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar a lista de permissões.", "erro"));
    return;
  }

  const contexto: Contexto = { areaFormulario, areaLista, erroAcao, catalogo };
  botaoNovo.addEventListener("click", () => abrirFormulario(contexto, null));
  await carregar(contexto);
}

async function carregar(contexto: Contexto): Promise<void> {
  contexto.areaLista.replaceChildren(elementoCarregando("Carregando perfis de acesso..."));
  try {
    const cargos = await listarCargos();
    renderizarLista(contexto, cargos);
  } catch {
    contexto.areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os perfis de acesso.", "erro"));
  }
}

function renderizarLista(contexto: Contexto, cargos: Cargo[]): void {
  if (cargos.length === 0) {
    contexto.areaLista.replaceChildren(cartaoEstado("Nenhum perfil de acesso criado ainda."));
    return;
  }

  const linhas = cargos.map((cargo) =>
    criarLinha(
      celula(cargo.nome),
      celula(cargo.descricao ?? "—"),
      celula(resumirAreas(contexto.catalogo, cargo.permissoes)),
      celulaComConteudo(criarAcoesCargo(contexto, cargo))
    )
  );
  contexto.areaLista.replaceChildren(criarTabela(["Perfil de acesso", "Descrição", "Acessa", ""], linhas, "perfil(is)"));
}

/** "Pedidos, Clientes, Conversas" — áreas que o cargo enxerga, na ordem do catálogo. */
function resumirAreas(catalogo: PermissaoDisponivel[], permissoes: Permissao[]): string {
  const areas = new Set(catalogo.filter((item) => permissoes.includes(item.codigo)).map((item) => item.area));
  return [...areas].join(", ");
}

function criarAcoesCargo(contexto: Contexto, cargo: Cargo): HTMLElement {
  const editar = document.createElement("button");
  editar.type = "button";
  editar.className = "btn btn-outline btn-pequeno";
  editar.textContent = "Editar";
  editar.addEventListener("click", () => abrirFormulario(contexto, cargo));

  const excluir = document.createElement("button");
  excluir.type = "button";
  excluir.className = "btn btn-perigo btn-pequeno";
  excluir.textContent = "Excluir";
  excluir.addEventListener("click", () => {
    if (!window.confirm(`Excluir o perfil de acesso "${cargo.nome}"?`)) {
      return;
    }
    void executarExclusao(contexto, excluir, cargo.id);
  });

  const acoes = document.createElement("div");
  acoes.className = "barra-acoes";
  acoes.append(editar, excluir);
  return acoes;
}

async function executarExclusao(contexto: Contexto, botao: HTMLButtonElement, cargoId: string): Promise<void> {
  botao.disabled = true;
  contexto.erroAcao.hidden = true;
  try {
    await excluirCargo(cargoId);
  } catch (falha) {
    mostrarErro(contexto.erroAcao, falha, "Não foi possível excluir o perfil de acesso.");
  }
  await carregar(contexto);
}

function abrirFormulario(contexto: Contexto, cargoEmEdicao: Cargo | null): void {
  contexto.areaFormulario.replaceChildren(
    criarFormulario(contexto.catalogo, cargoEmEdicao, async (dados) => {
      if (cargoEmEdicao === null) {
        await criarCargo(dados);
      } else {
        await atualizarCargo(cargoEmEdicao.id, dados);
      }
      contexto.areaFormulario.replaceChildren();
      await carregar(contexto);
    }, () => contexto.areaFormulario.replaceChildren())
  );
}

function criarFormulario(
  catalogo: PermissaoDisponivel[],
  cargoEmEdicao: Cargo | null,
  aoSalvar: (dados: DadosCargo) => Promise<void>,
  aoCancelar: () => void
): HTMLFormElement {
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao formulario-cargo";

  const tituloFormulario = document.createElement("h2");
  tituloFormulario.className = "formulario-cargo__titulo";
  tituloFormulario.textContent = cargoEmEdicao === null ? "Novo perfil de acesso" : `Editar perfil: ${cargoEmEdicao.nome}`;

  const nome = criarCampoTexto("cargo-nome", "Nome do perfil (ex.: Atendimento, Financeiro)", "text", true);
  nome.entrada.maxLength = 80;
  nome.entrada.value = cargoEmEdicao?.nome ?? "";

  const descricao = criarCampoTexto("cargo-descricao", "Descrição (opcional)", "text", false);
  descricao.entrada.maxLength = 255;
  descricao.entrada.value = cargoEmEdicao?.descricao ?? "";

  const caixas = new Map<Permissao, HTMLInputElement>();
  const grupos = criarGruposDePermissoes(catalogo, cargoEmEdicao?.permissoes ?? [], caixas);

  const aviso = document.createElement("p");
  aviso.className = "formulario-cargo__aviso";
  aviso.textContent = "Permissões em cinza são as que o seu próprio perfil de acesso não tem — você não pode concedê-las.";

  const erro = criarMensagemErro();

  const botaoCancelar = document.createElement("button");
  botaoCancelar.type = "button";
  botaoCancelar.className = "btn btn-ghost btn-pequeno";
  botaoCancelar.textContent = "Cancelar";
  botaoCancelar.addEventListener("click", aoCancelar);

  const botaoSalvar = document.createElement("button");
  botaoSalvar.type = "submit";
  botaoSalvar.className = "btn btn-primary btn-pequeno";
  botaoSalvar.textContent = cargoEmEdicao === null ? "Criar perfil" : "Salvar alterações";

  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(botaoCancelar, botaoSalvar);

  formulario.append(tituloFormulario, nome.container, descricao.container, grupos, aviso, erro, acoes);

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    const permissoes = [...caixas].filter(([, caixa]) => caixa.checked).map(([codigo]) => codigo);
    if (permissoes.length === 0) {
      mostrarErro(erro, null, "Marque ao menos uma permissão.");
      return;
    }
    botaoSalvar.disabled = true;
    const descricaoInformada = descricao.entrada.value.trim();
    aoSalvar({ nome: nome.entrada.value, descricao: descricaoInformada === "" ? null : descricaoInformada, permissoes })
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível salvar o perfil de acesso."))
      .finally(() => {
        botaoSalvar.disabled = false;
      });
  });

  return formulario;
}

function criarGruposDePermissoes(
  catalogo: PermissaoDisponivel[],
  marcadas: Permissao[],
  caixas: Map<Permissao, HTMLInputElement>
): HTMLElement {
  const grade = document.createElement("div");
  grade.className = "grade-permissoes";

  const porArea = new Map<string, PermissaoDisponivel[]>();
  for (const item of catalogo) {
    porArea.set(item.area, [...(porArea.get(item.area) ?? []), item]);
  }

  for (const [area, itens] of porArea) {
    const legenda = document.createElement("legend");
    legenda.textContent = area;

    const grupo = document.createElement("fieldset");
    grupo.className = "grupo-permissoes";
    grupo.append(legenda);

    for (const item of itens) {
      const caixa = document.createElement("input");
      caixa.type = "checkbox";
      caixa.value = item.codigo;
      caixa.checked = marcadas.includes(item.codigo);
      // Anti-escalada (o backend também recusa): não se concede o que não se tem.
      caixa.disabled = !possui(item.codigo);
      caixas.set(item.codigo, caixa);

      const texto = document.createElement("span");
      texto.textContent = item.descricao;

      const rotulo = document.createElement("label");
      rotulo.className = caixa.disabled ? "opcao-permissao opcao-permissao--bloqueada" : "opcao-permissao";
      rotulo.append(caixa, texto);
      grupo.append(rotulo);
    }
    grade.append(grupo);
  }
  return grade;
}
