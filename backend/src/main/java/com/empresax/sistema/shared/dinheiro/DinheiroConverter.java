package com.empresax.sistema.shared.dinheiro;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * autoApply=true: todo atributo do tipo Dinheiro é convertido automaticamente, inclusive dentro
 * de @Embeddable — não precisa de @Convert em cada campo.
 */
@Converter(autoApply = true)
public class DinheiroConverter implements AttributeConverter<Dinheiro, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Dinheiro dinheiro) {
        return dinheiro == null ? null : dinheiro.valor();
    }

    @Override
    public Dinheiro convertToEntityAttribute(BigDecimal coluna) {
        return coluna == null ? null : new Dinheiro(coluna);
    }
}
