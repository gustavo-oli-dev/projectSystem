import type { Pedido, TipoItem } from "../api/pedidosApi.js";
import type { Cobranca } from "../api/cobrancasApi.js";

export interface ItemSelecionado {
  tipo: TipoItem;
  referenciaId: string;
  descricao: string;
  precoUnitario: number;
  quantidade: number;
}

export interface EstadoWizard {
  etapaAtual: number;
  clienteId: string | null;
  clienteNome: string | null;
  itens: ItemSelecionado[];
  pedidoCriado: Pedido | null;
  cobrancaCriada: Cobranca | null;
}

const TOTAL_ETAPAS = 4;

type Ouvinte = (estado: EstadoWizard) => void;

let estado: EstadoWizard = estadoInicial();
const ouvintes: Ouvinte[] = [];

function estadoInicial(): EstadoWizard {
  return {
    etapaAtual: 1,
    clienteId: null,
    clienteNome: null,
    itens: [],
    pedidoCriado: null,
    cobrancaCriada: null,
  };
}

export function obterEstado(): EstadoWizard {
  return estado;
}

export function totalEtapas(): number {
  return TOTAL_ETAPAS;
}

export function definirCliente(id: string, nome: string): void {
  estado = { ...estado, clienteId: id, clienteNome: nome };
  notificar();
}

export function adicionarItem(item: ItemSelecionado): void {
  estado = { ...estado, itens: [...estado.itens, item] };
  notificar();
}

export function removerItem(indice: number): void {
  estado = { ...estado, itens: estado.itens.filter((_, i) => i !== indice) };
  notificar();
}

export function definirPedidoCriado(pedido: Pedido): void {
  estado = { ...estado, pedidoCriado: pedido };
  notificar();
}

export function definirCobrancaCriada(cobranca: Cobranca): void {
  estado = { ...estado, cobrancaCriada: cobranca };
  notificar();
}

export function irParaEtapa(etapa: number): void {
  estado = { ...estado, etapaAtual: etapa };
  notificar();
}

export function reiniciar(): void {
  estado = estadoInicial();
  notificar();
}

export function aoMudar(ouvinte: Ouvinte): void {
  ouvintes.push(ouvinte);
}

function notificar(): void {
  for (const ouvinte of ouvintes) {
    ouvinte(estado);
  }
}
