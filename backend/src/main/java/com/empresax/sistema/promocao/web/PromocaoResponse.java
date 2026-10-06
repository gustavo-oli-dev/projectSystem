package com.empresax.sistema.promocao.web;

import com.empresax.sistema.promocao.Promocao;
import com.empresax.sistema.promocao.SituacaoPromocao;
import com.empresax.sistema.promocao.TipoPromocao;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PromocaoResponse(
        UUID id,
        UUID produtoId,
        String produtoNome,
        BigDecimal precoNormal,
        TipoPromocao tipo,
        BigDecimal precoOferta,
        Integer leve,
        Integer pague,
        LocalDate inicio,
        LocalDate fim,
        SituacaoPromocao situacao
) {

    static PromocaoResponse de(Promocao promocao, String produtoNome, Dinheiro precoNormal, LocalDate hoje) {
        return new PromocaoResponse(
                promocao.id(),
                promocao.produtoId(),
                produtoNome,
                precoNormal.valor(),
                promocao.tipo(),
                promocao.precoOferta().map(Dinheiro::valor).orElse(null),
                promocao.leve().orElse(null),
                promocao.pague().orElse(null),
                promocao.inicio(),
                promocao.fim(),
                promocao.situacao(hoje));
    }
}
