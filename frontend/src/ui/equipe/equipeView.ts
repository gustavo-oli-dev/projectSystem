import { navegarPara } from "../../router.js";
import { possui } from "../../state/sessaoState.js";
import { montarAbaCargos } from "./cargosView.js";
import { montarAbaFuncionarios } from "./funcionariosView.js";

type Aba = "funcionarios" | "cargos";

interface DefinicaoAba {
  aba: Aba;
  rotulo: string;
  visivel: () => boolean;
  montar: (container: HTMLElement) => Promise<void>;
}

const ABAS: readonly DefinicaoAba[] = [
  { aba: "funcionarios", rotulo: "Funcionários", visivel: () => possui("USUARIOS_GERENCIAR"), montar: montarAbaFuncionarios },
  { aba: "cargos", rotulo: "Perfis de acesso", visivel: () => possui("CARGOS_GERENCIAR"), montar: montarAbaCargos },
];

/** Área "Funcionários e Perfis de acesso": cada aba aparece só para quem tem a permissão correspondente. */
export async function montarEquipe(container: HTMLElement, abaPedida: string | null): Promise<void> {
  const abasVisiveis = ABAS.filter((definicao) => definicao.visivel());
  const abaAtiva = abasVisiveis.find((definicao) => definicao.aba === abaPedida) ?? abasVisiveis[0];
  if (abaAtiva === undefined) {
    return;
  }

  const titulo = document.createElement("h1");
  titulo.textContent = "Funcionários e Perfis de acesso";

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const conteudoAba = document.createElement("div");
  container.replaceChildren(cabecalho, criarBarraAbas(abasVisiveis, abaAtiva.aba), conteudoAba);

  await abaAtiva.montar(conteudoAba);
}

function criarBarraAbas(abas: readonly DefinicaoAba[], abaAtiva: Aba): HTMLElement {
  const barra = document.createElement("div");
  barra.className = "abas";
  barra.setAttribute("role", "tablist");

  for (const definicao of abas) {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = definicao.aba === abaAtiva ? "abas__item abas__item--ativa" : "abas__item";
    botao.setAttribute("role", "tab");
    botao.setAttribute("aria-selected", String(definicao.aba === abaAtiva));
    botao.textContent = definicao.rotulo;
    botao.addEventListener("click", () => navegarPara("equipe", definicao.aba));
    barra.append(botao);
  }
  return barra;
}
