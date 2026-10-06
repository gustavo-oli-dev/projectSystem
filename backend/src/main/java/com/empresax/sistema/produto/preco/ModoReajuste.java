package com.empresax.sistema.produto.preco;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Como o preço em lote é calculado para cada produto (D39). */
public enum ModoReajuste {

    /** +5 = 5% mais caro; −10 = 10% mais barato. Arredonda para o centavo. */
    PERCENTUAL {
        @Override
        Dinheiro novoPreco(Dinheiro precoAtual, BigDecimal valor) {
            if (valor.compareTo(MENOR_PERCENTUAL) <= 0 || valor.compareTo(MAIOR_PERCENTUAL) > 0) {
                throw new DomainException("O reajuste precisa ficar acima de −100% e até +" + MAIOR_PERCENTUAL.intValue() + "%");
            }
            BigDecimal fator = BigDecimal.ONE.add(valor.divide(CEM, ESCALA_FATOR, RoundingMode.HALF_UP));
            return new Dinheiro(precoAtual.valor().multiply(fator).setScale(2, RoundingMode.HALF_UP));
        }
    },

    /** Todos os escolhidos passam a custar o mesmo valor (ex.: refrigerantes a R$ 9,99). */
    PRECO_UNICO {
        @Override
        Dinheiro novoPreco(Dinheiro precoAtual, BigDecimal valor) {
            if (valor.signum() <= 0) {
                throw new DomainException("O novo preço precisa ser maior que zero");
            }
            return new Dinheiro(valor);
        }
    };

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal MENOR_PERCENTUAL = BigDecimal.valueOf(-100);
    private static final BigDecimal MAIOR_PERCENTUAL = BigDecimal.valueOf(500);
    private static final int ESCALA_FATOR = 6;

    abstract Dinheiro novoPreco(Dinheiro precoAtual, BigDecimal valor);
}
