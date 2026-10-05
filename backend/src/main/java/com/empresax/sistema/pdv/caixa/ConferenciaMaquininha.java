package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * O que o relatório da maquininha mostra no fim do turno (crédito, débito, Pix), comparado com o que
 * o sistema registrou em cada forma. Toda forma da maquininha precisa ser informada — zero vale;
 * deixar em branco não (viraria uma "diferença" falsa).
 */
public record ConferenciaMaquininha(List<ConferenciaForma> conferencias) {

    public ConferenciaMaquininha {
        conferencias = List.copyOf(conferencias);
    }

    public static ConferenciaMaquininha de(
            Map<FormaPagamentoPresencial, Dinheiro> registradoNoSistema,
            Map<FormaPagamentoPresencial, Dinheiro> informadoDaMaquininha
    ) {
        if (informadoDaMaquininha == null) {
            throw new DomainException("Informe os valores do relatório da maquininha");
        }
        return new ConferenciaMaquininha(formasDaMaquininha().stream()
                .map(forma -> {
                    Dinheiro informado = informadoDaMaquininha.get(forma);
                    if (informado == null) {
                        throw new DomainException("Informe o valor de cada forma de pagamento no relatório da maquininha");
                    }
                    return new ConferenciaForma(forma, registradoNoSistema.getOrDefault(forma, Dinheiro.zero()), informado);
                })
                .toList());
    }

    /** Crédito, débito e Pix na maquininha — o que tem relatório para conferir. */
    public static List<FormaPagamentoPresencial> formasDaMaquininha() {
        return Arrays.stream(FormaPagamentoPresencial.values()).filter(FormaPagamentoPresencial::passaPelaMaquininha).toList();
    }
}
