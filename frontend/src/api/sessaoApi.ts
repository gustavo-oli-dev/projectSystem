import { httpClient } from "./httpClient.js";

/** Catálogo fixo de permissões — espelha o enum Permissao do backend. */
export type Permissao =
  | "PAINEL_VER"
  | "FATURAMENTO_VER"
  | "PEDIDOS_VER"
  | "PEDIDOS_GERENCIAR"
  | "CLIENTES_VER"
  | "CLIENTES_GERENCIAR"
  | "CATALOGO_VER"
  | "CATALOGO_GERENCIAR"
  | "ESTOQUE_GERENCIAR"
  | "PDV_VENDER"
  | "PDV_CANCELAR"
  | "CAIXA_CONFERIR"
  | "FISCAL_VER"
  | "FISCAL_GERENCIAR"
  | "COBRANCAS_VER"
  | "COBRANCAS_GERENCIAR"
  | "CONVERSAS_VER"
  | "CONVERSAS_ATENDER"
  | "ASSISTENTE_GESTOR_USAR"
  | "USUARIOS_GERENCIAR"
  | "CARGOS_GERENCIAR";

export interface Sessao {
  nome: string;
  email: string;
  acessoIrrestrito: boolean;
  cargo: string | null;
  permissoes: Permissao[];
  telefoneWhatsapp: string | null;
}

export function buscarSessao(): Promise<Sessao> {
  return httpClient.get<Sessao>("/me");
}
