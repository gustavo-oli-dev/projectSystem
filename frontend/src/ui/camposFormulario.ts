/** Campos de formulário compartilhados (Funcionários, Perfis de acesso, Gerenciar produtos). */

export interface CampoTexto {
  container: HTMLDivElement;
  entrada: HTMLInputElement;
}

export interface CampoSelecao {
  container: HTMLDivElement;
  selecao: HTMLSelectElement;
}

export interface OpcaoSelecao {
  valor: string;
  rotulo: string;
}

export function criarCampoTexto(id: string, rotuloTexto: string, tipo: string, obrigatorio: boolean): CampoTexto {
  const entrada = document.createElement("input");
  entrada.id = id;
  entrada.type = tipo;
  entrada.required = obrigatorio;
  return { container: envolverComRotulo(id, rotuloTexto, entrada), entrada };
}

export function criarCampoSelecao(id: string, rotuloTexto: string, opcoes: readonly OpcaoSelecao[]): CampoSelecao {
  const selecao = criarSelecao(opcoes);
  selecao.id = id;
  selecao.required = true;
  return { container: envolverComRotulo(id, rotuloTexto, selecao), selecao };
}

export function criarSelecao(opcoes: readonly OpcaoSelecao[]): HTMLSelectElement {
  const selecao = document.createElement("select");
  for (const opcao of opcoes) {
    const elemento = document.createElement("option");
    elemento.value = opcao.valor;
    elemento.textContent = opcao.rotulo;
    selecao.append(elemento);
  }
  return selecao;
}

export function criarMensagemErro(): HTMLParagraphElement {
  const erro = document.createElement("p");
  erro.className = "estado-erro";
  erro.setAttribute("role", "alert");
  erro.hidden = true;
  return erro;
}

export function mostrarErro(elemento: HTMLElement, falha: unknown, mensagemPadrao: string): void {
  elemento.textContent = falha instanceof Error && falha.message !== "" ? falha.message : mensagemPadrao;
  elemento.hidden = false;
}

/** Campo opcional: vazio vira null (o backend trata null como "não informado"). */
export function textoOuNulo(valor: string): string | null {
  const limpo = valor.trim();
  return limpo === "" ? null : limpo;
}

function envolverComRotulo(id: string, rotuloTexto: string, controle: HTMLElement): HTMLDivElement {
  const rotulo = document.createElement("label");
  rotulo.htmlFor = id;
  rotulo.textContent = rotuloTexto;

  const container = document.createElement("div");
  container.className = "campo-formulario";
  container.append(rotulo, controle);
  return container;
}
