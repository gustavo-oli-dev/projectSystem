import { listarCargos, type Cargo } from "../../api/cargosApi.js";
import {
  ativarFuncionario,
  cadastrarFuncionario,
  desativarFuncionario,
  listarFuncionarios,
  trocarCargoDoFuncionario,
  type Funcionario,
  type NovoFuncionario,
} from "../../api/usuariosApi.js";
import { sessaoAtual } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { celula, celulaComConteudo, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import {
  criarCampoSelecao,
  criarCampoTexto,
  criarMensagemErro,
  criarSelecao,
  mostrarErro,
  type OpcaoSelecao,
} from "../camposFormulario.js";

/** Valor da opção "acesso irrestrito" no select de cargo (não é um id de cargo). */
const OPCAO_ACESSO_IRRESTRITO = "__irrestrito__";
const TAMANHO_MINIMO_SENHA = 8;

interface Contexto {
  areaLista: HTMLElement;
  erroAcao: HTMLParagraphElement;
  cargos: Cargo[];
}

export async function montarAbaFuncionarios(container: HTMLElement): Promise<void> {
  const botaoNovo = document.createElement("button");
  botaoNovo.type = "button";
  botaoNovo.className = "btn btn-primary btn-pequeno";
  botaoNovo.textContent = "+ Novo funcionário";

  const barra = document.createElement("div");
  barra.className = "barra-aba";
  barra.append(botaoNovo);

  const areaFormulario = document.createElement("div");
  const erroAcao = criarMensagemErro();
  const areaLista = document.createElement("div");
  container.replaceChildren(barra, areaFormulario, erroAcao, areaLista);
  areaLista.append(elementoCarregando("Carregando funcionários..."));

  let cargos: Cargo[];
  try {
    cargos = await listarCargos();
  } catch {
    areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os perfis de acesso.", "erro"));
    return;
  }

  const contexto: Contexto = { areaLista, erroAcao, cargos };

  botaoNovo.addEventListener("click", () => {
    if (areaFormulario.childElementCount > 0) {
      areaFormulario.replaceChildren();
      return;
    }
    areaFormulario.append(
      criarFormulario(cargos, async (novo) => {
        await cadastrarFuncionario(novo);
        areaFormulario.replaceChildren();
        await carregar(contexto);
      })
    );
  });

  await carregar(contexto);
}

async function carregar(contexto: Contexto): Promise<void> {
  contexto.areaLista.replaceChildren(elementoCarregando("Carregando funcionários..."));
  try {
    const funcionarios = await listarFuncionarios();
    renderizarLista(contexto, funcionarios);
  } catch {
    contexto.areaLista.replaceChildren(cartaoEstado("Não foi possível carregar os funcionários.", "erro"));
  }
}

function renderizarLista(contexto: Contexto, funcionarios: Funcionario[]): void {
  if (funcionarios.length === 0) {
    contexto.areaLista.replaceChildren(cartaoEstado("Nenhum funcionário cadastrado ainda."));
    return;
  }

  const linhas = funcionarios.map((funcionario) =>
    criarLinha(
      celula(ehVoce(funcionario) ? `${funcionario.nome} (você)` : funcionario.nome),
      celula(funcionario.email),
      celulaComConteudo(criarControleCargo(contexto, funcionario)),
      celulaSelo(funcionario.ativo ? "Ativo" : "Inativo", funcionario.ativo ? "ativo" : "inativo"),
      celulaComConteudo(criarBotaoAtivacao(contexto, funcionario))
    )
  );

  contexto.areaLista.replaceChildren(
    criarTabela(["Nome", "E-mail", "Perfil de acesso", "Situação", ""], linhas, "funcionário(s)")
  );
}

/** Troca de cargo direto na linha; texto fixo quando o backend recusaria a troca. */
function criarControleCargo(contexto: Contexto, funcionario: Funcionario): HTMLElement {
  const rotuloAtual = funcionario.acessoIrrestrito ? "Acesso irrestrito" : funcionario.cargoNome ?? "—";
  if (!podeAlterar(funcionario)) {
    return criarTexto(rotuloAtual);
  }

  const opcoes: OpcaoSelecao[] = contexto.cargos.map((cargo) => ({ valor: cargo.id, rotulo: cargo.nome }));
  if (funcionario.acessoIrrestrito) {
    opcoes.unshift({ valor: OPCAO_ACESSO_IRRESTRITO, rotulo: "Acesso irrestrito" });
  }

  const selecao = criarSelecao(opcoes);
  selecao.className = "selecao-compacta";
  selecao.setAttribute("aria-label", `Perfil de acesso de ${funcionario.nome}`);
  selecao.value = funcionario.acessoIrrestrito ? OPCAO_ACESSO_IRRESTRITO : funcionario.cargoId ?? "";

  selecao.addEventListener("change", () => {
    if (selecao.value === OPCAO_ACESSO_IRRESTRITO) {
      return;
    }
    if (funcionario.acessoIrrestrito && !window.confirm(`Remover o acesso irrestrito de ${funcionario.nome}?`)) {
      selecao.value = OPCAO_ACESSO_IRRESTRITO;
      return;
    }
    void executar(contexto, selecao, () => trocarCargoDoFuncionario(funcionario.id, selecao.value));
  });
  return selecao;
}

function criarBotaoAtivacao(contexto: Contexto, funcionario: Funcionario): HTMLElement {
  if (!podeAlterar(funcionario)) {
    return criarTexto("");
  }
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = funcionario.ativo ? "btn btn-ghost btn-pequeno" : "btn btn-outline btn-pequeno";
  botao.textContent = funcionario.ativo ? "Desativar" : "Reativar";
  botao.addEventListener("click", () => {
    if (funcionario.ativo && !window.confirm(`Desativar ${funcionario.nome}? O acesso dele é cortado na hora.`)) {
      return;
    }
    const acao = funcionario.ativo ? desativarFuncionario : ativarFuncionario;
    void executar(contexto, botao, () => acao(funcionario.id));
  });
  return botao;
}

async function executar(
  contexto: Contexto,
  controle: HTMLButtonElement | HTMLSelectElement,
  acao: () => Promise<unknown>
): Promise<void> {
  controle.disabled = true;
  contexto.erroAcao.hidden = true;
  try {
    await acao();
  } catch (falha) {
    mostrarErro(contexto.erroAcao, falha, "Não foi possível concluir a ação.");
  }
  await carregar(contexto);
}

/** Espelha as regras do backend: ninguém altera a si mesmo; só irrestrito altera irrestrito. */
function podeAlterar(funcionario: Funcionario): boolean {
  const ator = sessaoAtual();
  if (ator === null || ehVoce(funcionario)) {
    return false;
  }
  return !funcionario.acessoIrrestrito || ator.acessoIrrestrito;
}

function ehVoce(funcionario: Funcionario): boolean {
  return sessaoAtual()?.email === funcionario.email;
}

function criarTexto(texto: string): HTMLSpanElement {
  const span = document.createElement("span");
  span.textContent = texto;
  return span;
}

function criarFormulario(cargos: Cargo[], aoSalvar: (novo: NovoFuncionario) => Promise<void>): HTMLFormElement {
  const formulario = document.createElement("form");
  formulario.className = "formulario-cartao";

  const nome = criarCampoTexto("funcionario-nome", "Nome", "text", true);
  const email = criarCampoTexto("funcionario-email", "E-mail de acesso", "email", true);
  const senha = criarCampoTexto("funcionario-senha", "Senha inicial (mín. 8 caracteres)", "password", true);
  senha.entrada.minLength = TAMANHO_MINIMO_SENHA;
  senha.entrada.autocomplete = "new-password";

  const opcoesCargo: OpcaoSelecao[] = cargos.map((cargo) => ({ valor: cargo.id, rotulo: cargo.nome }));
  if (sessaoAtual()?.acessoIrrestrito === true) {
    opcoesCargo.push({ valor: OPCAO_ACESSO_IRRESTRITO, rotulo: "Acesso irrestrito (dono / programador)" });
  }
  const cargo = criarCampoSelecao("funcionario-cargo", "Perfil de acesso", opcoesCargo);

  const erro = criarMensagemErro();

  const botaoSalvar = document.createElement("button");
  botaoSalvar.type = "submit";
  botaoSalvar.className = "btn btn-primary btn-pequeno";
  botaoSalvar.textContent = "Cadastrar";

  const acoes = document.createElement("div");
  acoes.className = "formulario-cartao__acoes";
  acoes.append(botaoSalvar);

  formulario.append(nome.container, email.container, senha.container, cargo.container, erro, acoes);

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    erro.hidden = true;
    botaoSalvar.disabled = true;
    const irrestrito = cargo.selecao.value === OPCAO_ACESSO_IRRESTRITO;

    aoSalvar({
      nome: nome.entrada.value,
      email: email.entrada.value,
      senha: senha.entrada.value,
      acessoIrrestrito: irrestrito,
      cargoId: irrestrito ? null : cargo.selecao.value,
    })
      .catch((falha: unknown) => mostrarErro(erro, falha, "Não foi possível cadastrar. Confira os dados."))
      .finally(() => {
        botaoSalvar.disabled = false;
      });
  });

  return formulario;
}
