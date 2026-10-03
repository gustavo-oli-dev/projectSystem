package com.empresax.sistema.pdv.maquininha;

import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.util.UUID;

/**
 * Porta para a maquininha integrada (D20). O caixa manda o valor; a maquininha mostra ao cliente,
 * cobra o cartão e o sistema consulta o resultado — sem ninguém digitar autorização. Trocar de
 * fornecedor (Mercado Pago Point, Stone, Cielo...) é trocar só o adapter.
 */
public interface Maquininha {

    /** Envia a cobrança para a maquininha. @return identificador da cobrança no fornecedor */
    String enviarCobranca(Dinheiro valor, FormaPagamentoPresencial forma, UUID referenciaVenda);

    SituacaoCobrancaMaquininha consultar(String idCobranca);

    /** Tira da tela da maquininha uma cobrança que ainda não foi paga. */
    void cancelarCobranca(String idCobranca);

    /** Devolve ao cliente um pagamento aprovado (venda cancelada depois de paga). */
    void estornar(String idPagamento, String chaveIdempotencia);
}
