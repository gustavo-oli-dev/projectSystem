import { aoMudarRota, destinoAtual, navegarPara, type Destino, type Rota } from "../../router.js";
import type { Permissao } from "../../api/sessaoApi.js";
import { criarIcone } from "../icones.js";
import { cartaoEstado } from "../estadoCard.js";
import { montarPainel } from "../painel/painelView.js";
import { montarListaPedidos } from "../pedidos/pedidosListView.js";
import { montarListaClientes } from "../clientes/clientesListView.js";
import { montarListaProdutos } from "../produtos/produtosListView.js";
import { montarListaServicos } from "../servicos/servicosListView.js";
import { montarFiscal } from "../fiscal/fiscalView.js";
import { montarListaCobrancas } from "../cobrancas/cobrancasListView.js";
import { montarConversas } from "../atendimento/conversasView.js";
import { montarDetalhePedido } from "../pedidos/pedidoDetalheView.js";
import { montarWizardPedido } from "../pedidoWizard/pedidoWizardView.js";
import { montarEquipe } from "../equipe/equipeView.js";
import { montarGerenciarProdutos } from "../produtos/gerenciarProdutosView.js";
import { montarPdv } from "../pdv/pdvView.js";
import { montarConferenciaCaixas } from "../caixa/conferenciaCaixasView.js";
import { criarBotaoAssistente } from "../assistente/assistenteChatView.js";
import { limparToken } from "../../state/authState.js";
import { barraLateralRecolhida, lembrarBarraLateralRecolhida } from "./barraLateral.js";
import { possui, possuiAlguma, sessaoAtual } from "../../state/sessaoState.js";

interface ItemNav {
  rota: Rota;
  rotulo: string;
  icone: string;
}

interface GrupoNav {
  titulo: string;
  itens: ItemNav[];
}

type Montador = (container: HTMLElement, parametro: string | null) => void | Promise<void>;

const GRUPOS_NAV: GrupoNav[] = [
  {
    titulo: "Vendas",
    itens: [
      { rota: "painel", rotulo: "Painel", icone: "painel" },
      { rota: "pdv", rotulo: "Caixa", icone: "caixa" },
      { rota: "caixas", rotulo: "Conferência de caixa", icone: "conferencia" },
      { rota: "pedidos", rotulo: "Pedidos", icone: "pedidos" },
      { rota: "clientes", rotulo: "Clientes", icone: "clientes" },
    ],
  },
  {
    titulo: "Catálogo",
    itens: [
      { rota: "produtos", rotulo: "Produtos", icone: "produtos" },
      { rota: "gerenciar-produtos", rotulo: "Gerenciar produtos", icone: "gerenciar" },
      { rota: "servicos", rotulo: "Serviços", icone: "servicos" },
    ],
  },
  {
    titulo: "Operação",
    itens: [
      { rota: "fiscal", rotulo: "Fiscal · SEFAZ", icone: "fiscal" },
      { rota: "cobrancas", rotulo: "Cobranças", icone: "cobrancas" },
      { rota: "conversas", rotulo: "Conversas", icone: "atendimento" },
    ],
  },
  {
    titulo: "Administração",
    itens: [{ rota: "equipe", rotulo: "Funcionários e Perfis", icone: "equipe" }],
  },
];

/** Basta uma das permissões listadas para entrar na rota. */
const PERMISSOES_POR_ROTA: Record<Rota, readonly Permissao[]> = {
  painel: ["PAINEL_VER"],
  pedidos: ["PEDIDOS_VER"],
  "pedido-detalhe": ["PEDIDOS_VER"],
  clientes: ["CLIENTES_VER"],
  produtos: ["CATALOGO_VER"],
  servicos: ["CATALOGO_VER"],
  fiscal: ["FISCAL_VER"],
  cobrancas: ["COBRANCAS_VER"],
  conversas: ["CONVERSAS_VER"],
  "novo-pedido": ["PEDIDOS_GERENCIAR"],
  equipe: ["USUARIOS_GERENCIAR", "CARGOS_GERENCIAR"],
  "gerenciar-produtos": ["CATALOGO_GERENCIAR", "ESTOQUE_GERENCIAR"],
  pdv: ["PDV_VENDER", "PDV_CANCELAR"],
  caixas: ["CAIXA_CONFERIR"],
};

const MONTADORES: Record<Rota, Montador> = {
  painel: montarPainel,
  pedidos: montarListaPedidos,
  "pedido-detalhe": montarDetalhePedido,
  clientes: montarListaClientes,
  produtos: montarListaProdutos,
  servicos: montarListaServicos,
  fiscal: montarFiscal,
  cobrancas: montarListaCobrancas,
  conversas: montarConversas,
  "novo-pedido": montarWizardPedido,
  equipe: montarEquipe,
  "gerenciar-produtos": montarGerenciarProdutos,
  pdv: montarPdv,
  caixas: montarConferenciaCaixas,
};

export function montarShell(raiz: HTMLElement): void {
  const links: HTMLButtonElement[] = [];
  const sidebar = criarSidebar(links);

  const conteudo = document.createElement("main");
  conteudo.className = "conteudo-principal";

  const appShell = document.createElement("div");
  appShell.className = "app-shell";
  appShell.classList.toggle("app-shell--recolhida", barraLateralRecolhida());

  const areaPrincipal = document.createElement("div");
  areaPrincipal.className = "area-principal";
  areaPrincipal.append(criarTopbar(appShell), conteudo);
  appShell.append(sidebar, areaPrincipal);

  raiz.replaceChildren(appShell);

  function renderizarRota(): void {
    const destino = destinoAtual();
    atualizarLinkAtivo(links, destino.rota === "pedido-detalhe" ? "pedidos" : destino.rota);
    renderizarConteudo(conteudo, destino);
  }

  aoMudarRota(renderizarRota);
  renderizarRota();
}

function podeAcessar(rota: Rota): boolean {
  return possuiAlguma(PERMISSOES_POR_ROTA[rota]);
}

function criarSidebar(links: HTMLButtonElement[]): HTMLElement {
  const logo = document.createElement("span");
  logo.className = "sidebar__logo";
  logo.textContent = "E";

  const marca = document.createElement("div");
  marca.className = "sidebar__marca";
  marca.append(logo, criarSpan("Empresa X"));

  const nav = document.createElement("nav");
  nav.className = "sidebar__nav";

  for (const grupo of GRUPOS_NAV) {
    const itensVisiveis = grupo.itens.filter((item) => podeAcessar(item.rota));
    if (itensVisiveis.length === 0) {
      continue;
    }

    const tituloGrupo = document.createElement("p");
    tituloGrupo.className = "sidebar__grupo";
    tituloGrupo.textContent = grupo.titulo;
    nav.append(tituloGrupo);

    for (const item of itensVisiveis) {
      const link = criarLinkNav(item);
      links.push(link);
      nav.append(link);
    }
  }

  const botaoSair = document.createElement("button");
  botaoSair.type = "button";
  botaoSair.className = "botao-sair";
  botaoSair.textContent = "Sair";
  botaoSair.addEventListener("click", () => limparToken());

  const rodape = document.createElement("div");
  rodape.className = "sidebar__rodape";
  rodape.append(criarIdentificacaoUsuario(), botaoSair);

  const sidebar = document.createElement("aside");
  sidebar.className = "sidebar";
  sidebar.append(marca, nav, rodape);
  return sidebar;
}

function criarIdentificacaoUsuario(): HTMLElement {
  const sessao = sessaoAtual();
  const nome = criarSpan(sessao?.nome ?? "");
  nome.className = "sidebar__usuario-nome";

  const cargo = criarSpan(sessao?.acessoIrrestrito === true ? "Acesso irrestrito" : sessao?.cargo ?? "");
  cargo.className = "sidebar__usuario-cargo";

  const bloco = document.createElement("div");
  bloco.className = "sidebar__usuario";
  bloco.append(nome, cargo);
  return bloco;
}

function criarTopbar(appShell: HTMLElement): HTMLElement {
  const esquerda = document.createElement("div");
  esquerda.className = "topbar__esquerda";
  esquerda.append(criarBotaoRecolher(appShell));

  const direita = document.createElement("div");
  direita.className = "topbar__direita";

  if (possui("ASSISTENTE_GESTOR_USAR")) {
    direita.append(criarBotaoAssistente());
  }

  if (possui("PEDIDOS_GERENCIAR")) {
    const botaoNovoPedido = document.createElement("button");
    botaoNovoPedido.type = "button";
    botaoNovoPedido.className = "btn btn-primary btn-pequeno topbar__acao";
    botaoNovoPedido.append(criarIcone("mais"), criarSpan("Novo pedido"));
    botaoNovoPedido.addEventListener("click", () => navegarPara("novo-pedido"));
    direita.append(botaoNovoPedido);
  }

  direita.append(criarAvatar());

  const topbar = document.createElement("header");
  topbar.className = "topbar";
  topbar.append(esquerda, direita);
  return topbar;
}

/** Recolhe a barra lateral para só os ícones (mais espaço para o conteúdo). */
function criarBotaoRecolher(appShell: HTMLElement): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "botao-recolher";
  botao.append(criarIcone("menu"));

  const atualizarRotulo = (): void => {
    const recolhida = appShell.classList.contains("app-shell--recolhida");
    const rotulo = recolhida ? "Mostrar barra lateral" : "Ocultar barra lateral";
    botao.title = rotulo;
    botao.setAttribute("aria-label", rotulo);
    botao.setAttribute("aria-expanded", String(!recolhida));
  };
  botao.addEventListener("click", () => {
    const recolhida = appShell.classList.toggle("app-shell--recolhida");
    lembrarBarraLateralRecolhida(recolhida);
    atualizarRotulo();
  });
  atualizarRotulo();
  return botao;
}

function criarAvatar(): HTMLElement {
  const nome = sessaoAtual()?.nome ?? "";
  const avatar = document.createElement("span");
  avatar.className = "topbar__avatar";
  avatar.textContent = nome.trim().charAt(0).toUpperCase() || "?";
  avatar.title = nome;
  return avatar;
}

function criarLinkNav(item: ItemNav): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "sidebar__link";
  botao.dataset["rota"] = item.rota;
  botao.title = item.rotulo;
  botao.append(criarIcone(item.icone), criarSpan(item.rotulo));
  botao.addEventListener("click", () => navegarPara(item.rota));
  return botao;
}

function criarSpan(texto: string): HTMLSpanElement {
  const span = document.createElement("span");
  span.textContent = texto;
  return span;
}

function atualizarLinkAtivo(links: HTMLButtonElement[], rotaAtiva: Rota): void {
  for (const link of links) {
    link.classList.toggle("sidebar__link--ativo", link.dataset["rota"] === rotaAtiva);
  }
}

function renderizarConteudo(conteudo: HTMLElement, destino: Destino): void {
  if (podeAcessar(destino.rota)) {
    void MONTADORES[destino.rota](conteudo, destino.parametro);
    return;
  }

  // Rota padrão (painel) sem permissão: leva à primeira área que o cargo enxerga.
  const primeiraAcessivel = GRUPOS_NAV.flatMap((grupo) => grupo.itens).find((item) => podeAcessar(item.rota));
  if (destino.rota === "painel" && primeiraAcessivel !== undefined) {
    navegarPara(primeiraAcessivel.rota);
    return;
  }
  conteudo.replaceChildren(cartaoEstado("Seu perfil de acesso não permite entrar nesta área.", "erro"));
}
