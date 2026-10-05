package com.empresax.sistema.produto.estoque;

/** Por que o produto saiu do estoque sem ser vendido. "Outro" exige explicar na observação. */
public enum MotivoPerda {
    VENCIDO(false),
    AVARIADO(false),
    FURTO(false),
    USO_INTERNO(false),
    OUTRO(true);

    private final boolean exigeObservacao;

    MotivoPerda(boolean exigeObservacao) {
        this.exigeObservacao = exigeObservacao;
    }

    public boolean exigeObservacao() {
        return exigeObservacao;
    }
}
