package com.empresax.sistema.shared.dinheiro;

import com.empresax.sistema.common.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetário. Nunca negativo, sempre com 2 casas decimais — evita os erros de
 * arredondamento de ponto flutuante que `double`/`float` trariam para dinheiro.
 */
public record Dinheiro(BigDecimal valor) {

    public Dinheiro {
        if (valor == null) {
            throw new DomainException("Valor monetário é obrigatório");
        }
        if (valor.signum() < 0) {
            throw new DomainException("Valor monetário não pode ser negativo");
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinheiro zero() {
        return new Dinheiro(BigDecimal.ZERO);
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro multiplicar(int quantidade) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)));
    }
}
