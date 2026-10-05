package com.empresax.sistema.pdv;

import com.empresax.sistema.pedido.ItemPedidoRequerido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * O que toda venda do caixa informa: produtos, CPF na nota (opcional), cliente cadastrado (opcional)
 * e desconto autorizado por um gerente (opcional).
 */
public record DadosVendaBalcao(List<ItemPedidoRequerido> itens, String cpfNaNota, UUID clienteId, Desconto desconto) {

    /** Valor em reais + token da autorização (D35). */
    public record Desconto(BigDecimal valor, String tokenAutorizacao) {
    }
}
