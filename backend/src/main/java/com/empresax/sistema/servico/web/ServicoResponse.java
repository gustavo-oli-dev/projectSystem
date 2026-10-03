package com.empresax.sistema.servico.web;

import com.empresax.sistema.servico.Servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        String nome,
        String descricao,
        String codigoServicoLc116,
        BigDecimal aliquotaIss,
        BigDecimal precoUnitario,
        boolean ativo
) {

    public static ServicoResponse de(Servico servico) {
        return new ServicoResponse(
                servico.id(),
                servico.nome(),
                servico.descricao(),
                servico.codigoServicoLc116(),
                servico.aliquotaIss(),
                servico.precoUnitario().valor(),
                servico.ativo()
        );
    }
}
