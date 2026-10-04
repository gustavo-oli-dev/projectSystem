package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Quantas cédulas/moedas de cada valor. Quantidade zero é descartada (não precisa ser guardada);
 * negativa é recusada. O total é sempre calculado — ninguém digita o total à mão.
 */
public record ContagemCedulas(Map<Cedula, Integer> quantidades) {

    public ContagemCedulas {
        if (quantidades == null) {
            throw new DomainException("Informe a contagem das cédulas");
        }
        EnumMap<Cedula, Integer> validas = new EnumMap<>(Cedula.class);
        quantidades.forEach((cedula, quantidade) -> {
            if (cedula == null || quantidade == null) {
                throw new DomainException("Contagem de cédulas incompleta");
            }
            if (quantidade < 0) {
                throw new DomainException("Quantidade de cédulas não pode ser negativa");
            }
            if (quantidade > 0) {
                validas.put(cedula, quantidade);
            }
        });
        quantidades = Collections.unmodifiableMap(validas);
    }

    public static ContagemCedulas vazia() {
        return new ContagemCedulas(Map.of());
    }

    public Dinheiro total() {
        return quantidades.entrySet().stream()
                .map(entrada -> entrada.getKey().valor().multiplicar(entrada.getValue()))
                .reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    public boolean estaVazia() {
        return quantidades.isEmpty();
    }
}
