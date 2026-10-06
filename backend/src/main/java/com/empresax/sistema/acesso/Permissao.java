package com.empresax.sistema.acesso;

/**
 * Catálogo fixo de permissões. É fixo (definido no código) porque cada permissão corresponde a uma
 * checagem real no servidor; o que é configurável pela tela são os cargos que as agrupam.
 * Separa "ver" de "gerenciar" em cada área.
 */
public enum Permissao {
    // Primeira de propósito: aparece no topo do formulário de cargo. Ordem não afeta o banco (EnumType.STRING).
    /** Uma permissão só: quem vê o assistente já pode conversar com ele (painel e WhatsApp interno). */
    ASSISTENTE_GESTOR_USAR("Assistente de IA", "Ver e conversar com o assistente (painel e WhatsApp interno)"),
    PDV_VENDER("Caixa (balcão)", "Vender no caixa: ler produtos e receber o pagamento"),
    PDV_CANCELAR("Caixa (balcão)", "Cancelar venda do caixa (estorno e devolução ao estoque)"),
    PDV_AUTORIZAR("Caixa (balcão)", "Autorizar desconto e cancelamento de item no caixa (com e-mail e senha na hora)"),
    CAIXA_GERENCIAR("Caixa (balcão)", "Abrir e fechar caixas, repor troco e fazer sangria em qualquer caixa"),
    CAIXA_CONFERIR("Caixa (balcão)", "Conferir abertura e fechamento de todos os caixas e definir o fundo de troco"),
    PAINEL_VENDAS("Painel", "Aba Vendas: faturamento, formas de pagamento e canais"),
    PAINEL_PRODUTOS("Painel", "Aba Produtos: mais vendidos e perdas"),
    PAINEL_HORARIOS("Painel", "Aba Horários: dias da semana e horário de pico"),
    PAINEL_CAIXA("Painel", "Aba Caixa: fechamentos, resultado por operador e itens cancelados"),
    PAINEL_DINHEIRO_DO_DIA("Painel", "Aba Dinheiro do dia: conferência da gaveta com o vendido em dinheiro"),
    FINANCEIRO_VER("Painel", "Aba Financeiro: entradas, saídas, saldo e exportação"),
    PAINEL_OPERACAO("Painel", "Aba Operação agora: estoque baixo, pedidos e recebimentos pendentes"),
    FATURAMENTO_VER("Faturamento", "Ver custo e lucro dos produtos e perguntar o faturamento ao assistente"),
    PEDIDOS_VER("Pedidos", "Ver pedidos"),
    PEDIDOS_GERENCIAR("Pedidos", "Criar, confirmar e cancelar pedidos"),
    CLIENTES_VER("Clientes", "Ver clientes"),
    CLIENTES_GERENCIAR("Clientes", "Cadastrar e editar clientes"),
    CATALOGO_VER("Catálogo", "Ver produtos e serviços"),
    CATALOGO_GERENCIAR("Catálogo", "Cadastrar e editar produtos, preços, fotos e serviços"),
    PROMOCOES_GERENCIAR("Catálogo", "Criar e encerrar promoções (preço de oferta e leve X pague Y)"),
    ESTOQUE_GERENCIAR("Catálogo", "Dar entrada de mercadoria no estoque (também pelo XML da nota), perdas e inventário"),
    CONTATOS_GERENCIAR("Contatos", "Cadastrar e editar fornecedores, transportadoras e outros contatos"),
    CONTAS_PAGAR_GERENCIAR("Contas a pagar", "Ver, lançar e dar baixa nas contas a pagar"),
    FISCAL_VER("Fiscal · SEFAZ", "Ver documentos fiscais"),
    FISCAL_GERENCIAR("Fiscal · SEFAZ", "Gerar documentos fiscais"),
    COBRANCAS_VER("Recebimentos", "Ver recebimentos (Pix e boleto do Mercado Pago)"),
    COBRANCAS_GERENCIAR("Recebimentos", "Gerar cobranças de Pix e boleto"),
    CONVERSAS_VER("Conversas", "Ver conversas do WhatsApp"),
    CONVERSAS_ATENDER("Conversas", "Assumir, responder e encerrar conversas"),
    USUARIOS_GERENCIAR("Funcionários e Perfis", "Cadastrar funcionários, trocar o perfil de acesso e desativar"),
    CARGOS_GERENCIAR("Funcionários e Perfis", "Criar e editar perfis de acesso");

    private final String area;
    private final String descricao;

    Permissao(String area, String descricao) {
        this.area = area;
        this.descricao = descricao;
    }

    public String area() {
        return area;
    }

    public String descricao() {
        return descricao;
    }
}
