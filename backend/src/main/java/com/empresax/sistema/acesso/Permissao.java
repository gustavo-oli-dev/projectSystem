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
    CAIXA_GERENCIAR("Caixa (balcão)", "Abrir e fechar caixas, repor troco e fazer sangria em qualquer caixa"),
    CAIXA_CONFERIR("Caixa (balcão)", "Conferir abertura e fechamento de todos os caixas e definir o fundo de troco"),
    PAINEL_VER("Painel", "Ver o painel"),
    FATURAMENTO_VER("Faturamento", "Ver faturamento"),
    PEDIDOS_VER("Pedidos", "Ver pedidos"),
    PEDIDOS_GERENCIAR("Pedidos", "Criar, confirmar e cancelar pedidos"),
    CLIENTES_VER("Clientes", "Ver clientes"),
    CLIENTES_GERENCIAR("Clientes", "Cadastrar e editar clientes"),
    CATALOGO_VER("Catálogo", "Ver produtos e serviços"),
    CATALOGO_GERENCIAR("Catálogo", "Cadastrar e editar produtos, preços, fotos e serviços"),
    ESTOQUE_GERENCIAR("Catálogo", "Dar entrada de mercadoria no estoque"),
    FISCAL_VER("Fiscal · SEFAZ", "Ver documentos fiscais"),
    FISCAL_GERENCIAR("Fiscal · SEFAZ", "Gerar documentos fiscais"),
    COBRANCAS_VER("Cobranças", "Ver cobranças"),
    COBRANCAS_GERENCIAR("Cobranças", "Gerar cobranças"),
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
