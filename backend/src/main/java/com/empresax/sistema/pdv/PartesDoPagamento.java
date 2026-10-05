package com.empresax.sistema.pdv;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.util.List;

/**
 * Pagamento dividido (D36): as partes já recebidas (dinheiro, ou cartão/Pix com o comprovante da
 * maquininha) antes da forma que fecha a venda. Cada parte tem valor; juntas ficam abaixo do total —
 * o que falta é pago pela última forma (dinheiro com troco, maquininha integrada ou contingência).
 */
public record PartesDoPagamento(List<DadosPagamentoPresencial> partes) {

    public static final int MAXIMO_DE_PARTES = 5;

    public PartesDoPagamento {
        partes = partes == null ? List.of() : List.copyOf(partes);
        if (partes.size() > MAXIMO_DE_PARTES) {
            throw new DomainException("Divida o pagamento em no máximo " + (MAXIMO_DE_PARTES + 1) + " formas");
        }
        for (DadosPagamentoPresencial parte : partes) {
            if (parte == null || parte.forma() == null) {
                throw new DomainException("Escolha a forma de cada parte do pagamento");
            }
            if (parte.valor() == null || parte.valor().signum() <= 0) {
                throw new DomainException("Cada parte do pagamento precisa de um valor maior que zero");
            }
        }
    }

    public static PartesDoPagamento nenhuma() {
        return new PartesDoPagamento(List.of());
    }

    public Dinheiro soma() {
        return partes.stream().map(parte -> new Dinheiro(parte.valor())).reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    /** O que falta para fechar a venda depois das partes. Precisa sobrar algo para a última forma. */
    public Dinheiro restante(Dinheiro total) {
        if (!soma().menorQue(total)) {
            throw new DomainException("As partes já somam " + soma().valor() + " de " + total.valor()
                    + ": a última forma precisa pagar o que falta (tire uma parte ou diminua o valor)");
        }
        return total.subtrair(soma());
    }

    public boolean vazia() {
        return partes.isEmpty();
    }
}
