package com.empresax.sistema.cobranca.pagamento;

import com.empresax.sistema.cliente.Cliente;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.util.UUID;

/**
 * Porta para o provedor de pagamento (Mercado Pago, decisão D4 em DECISOES.md). Isolar a
 * interface permite trocar de provedor sem tocar em CobrancaService. O Cliente é passado porque o
 * Mercado Pago exige identificação do pagador (CPF/CNPJ) para Pix e boleto.
 */
public interface ProvedorPagamento {

    DadosCobrancaExterna criarCobranca(
            Dinheiro valor, String descricao, UUID referenciaPedido, MeioCobranca meio, Cliente cliente);

    StatusPagamentoExterno consultarStatus(String referenciaExterna);

    /**
     * Devolve ao cliente o valor total do pagamento. A chave de idempotência garante que repetir a
     * chamada (ex.: depois de uma falha no meio) não devolve o dinheiro duas vezes.
     */
    void reembolsar(String referenciaExterna, String chaveIdempotencia);

    /** Cancela no provedor uma cobrança ainda não paga, para o cliente não conseguir pagar um pedido cancelado. */
    void cancelarCobranca(String referenciaExterna);
}
