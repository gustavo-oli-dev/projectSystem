package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.Cedula;
import com.empresax.sistema.pdv.caixa.ContagemCedulas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CedulaContadaResponse(Cedula cedula, BigDecimal valorUnitario, int quantidade, BigDecimal subtotal) {

    /** Da menor para a maior cédula (ordem do enum), só as que têm quantidade. */
    static List<CedulaContadaResponse> de(ContagemCedulas contagem) {
        return contagem.quantidades().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> new CedulaContadaResponse(
                        entrada.getKey(),
                        entrada.getKey().valor().valor(),
                        entrada.getValue(),
                        entrada.getKey().valor().multiplicar(entrada.getValue()).valor()))
                .toList();
    }
}
