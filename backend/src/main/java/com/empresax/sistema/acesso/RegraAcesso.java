package com.empresax.sistema.acesso;

/**
 * Expressões de @PreAuthorize por permissão. Anotações exigem constantes de compilação, então não
 * dá para usar Permissao.X.name() direto — estas constantes evitam strings soltas pelos controllers.
 * O teste RegraAcessoTest garante que cada uma corresponde a uma Permissao existente.
 */
public final class RegraAcesso {

    public static final String PAINEL_VER = "hasAuthority('PAINEL_VER')";
    public static final String FATURAMENTO_VER = "hasAuthority('FATURAMENTO_VER')";
    public static final String PEDIDOS_VER = "hasAuthority('PEDIDOS_VER')";
    public static final String PEDIDOS_GERENCIAR = "hasAuthority('PEDIDOS_GERENCIAR')";
    public static final String CLIENTES_VER = "hasAuthority('CLIENTES_VER')";
    public static final String CLIENTES_GERENCIAR = "hasAuthority('CLIENTES_GERENCIAR')";
    public static final String CATALOGO_VER = "hasAuthority('CATALOGO_VER')";
    public static final String CATALOGO_GERENCIAR = "hasAuthority('CATALOGO_GERENCIAR')";
    public static final String ESTOQUE_GERENCIAR = "hasAuthority('ESTOQUE_GERENCIAR')";
    public static final String PDV_VENDER = "hasAuthority('PDV_VENDER')";
    public static final String PDV_CANCELAR = "hasAuthority('PDV_CANCELAR')";
    public static final String CAIXA_CONFERIR = "hasAuthority('CAIXA_CONFERIR')";
    public static final String FISCAL_VER = "hasAuthority('FISCAL_VER')";
    public static final String FISCAL_GERENCIAR = "hasAuthority('FISCAL_GERENCIAR')";
    public static final String COBRANCAS_VER = "hasAuthority('COBRANCAS_VER')";
    public static final String COBRANCAS_GERENCIAR = "hasAuthority('COBRANCAS_GERENCIAR')";
    public static final String CONVERSAS_VER = "hasAuthority('CONVERSAS_VER')";
    public static final String CONVERSAS_ATENDER = "hasAuthority('CONVERSAS_ATENDER')";
    public static final String ASSISTENTE_GESTOR_USAR = "hasAuthority('ASSISTENTE_GESTOR_USAR')";
    public static final String USUARIOS_GERENCIAR = "hasAuthority('USUARIOS_GERENCIAR')";
    public static final String CARGOS_GERENCIAR = "hasAuthority('CARGOS_GERENCIAR')";

    private RegraAcesso() {
    }
}
