package com.empresax.sistema.compras.entrada;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** O que interessa de uma NF-e de compra, lido do XML do fornecedor (D34). */
public record NotaDoFornecedor(
        String chaveAcesso,
        String numero,
        String serie,
        Instant emitidaEm,
        Emitente emitente,
        List<ItemDaNota> itens,
        BigDecimal valorTotal,
        List<Parcela> parcelas
) {

    public NotaDoFornecedor {
        itens = List.copyOf(itens);
        parcelas = List.copyOf(parcelas);
    }

    /** Quem vendeu: CNPJ (ou CPF), razão social e telefone, como estão na nota. */
    public record Emitente(String documento, String nome, String telefone) {
    }

    /**
     * Um produto da nota. codigoBarras vazio = "SEM GTIN". A quantidade vem decimal na NF-e; o
     * estoque do sistema é em unidades inteiras.
     */
    public record ItemDaNota(
            int ordem, String codigo, String codigoBarras, String descricao, String unidade,
            BigDecimal quantidade, BigDecimal custoUnitario
    ) {
        public boolean quantidadeInteira() {
            return quantidade.stripTrailingZeros().scale() <= 0;
        }
    }

    /** Duplicata da nota: vira uma conta a pagar. */
    public record Parcela(String numero, LocalDate vencimento, BigDecimal valor) {
    }
}
