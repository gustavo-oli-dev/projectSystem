package com.empresax.sistema.promocao;

import com.empresax.sistema.shared.dinheiro.Dinheiro;

/** Cada tipo calcula o desconto do seu jeito (sem if/switch espalhado pelo código). */
public enum TipoPromocao {

    /** "De R$ 9,90 por R$ 7,90": cada unidade sai pelo preço de oferta. */
    PRECO_OFERTA {
        @Override
        Dinheiro descontoPara(Promocao promocao, Dinheiro precoNormal, int quantidade) {
            Dinheiro oferta = promocao.precoOferta().orElseThrow();
            if (!oferta.menorQue(precoNormal)) {
                // O preço normal baixou para menos que a oferta: vale o menor, sem desconto.
                return Dinheiro.zero();
            }
            return precoNormal.subtrair(oferta).multiplicar(quantidade);
        }
    },

    /** "Leve 3, pague 2": a cada grupo de "leve" unidades, as que passam de "pague" saem de graça. */
    LEVE_PAGUE {
        @Override
        Dinheiro descontoPara(Promocao promocao, Dinheiro precoNormal, int quantidade) {
            int leve = promocao.leve().orElseThrow();
            int pague = promocao.pague().orElseThrow();
            int unidadesDeGraca = quantidade / leve * (leve - pague);
            return precoNormal.multiplicar(unidadesDeGraca);
        }
    };

    abstract Dinheiro descontoPara(Promocao promocao, Dinheiro precoNormal, int quantidade);
}
