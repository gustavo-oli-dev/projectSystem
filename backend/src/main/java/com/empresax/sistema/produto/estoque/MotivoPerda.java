package com.empresax.sistema.produto.estoque;

/** Por que o produto saiu do estoque sem ser vendido (retirada). "Outro" exige escrever o motivo. */
public enum MotivoPerda {
    VENCIDO(false),
    AVARIADO(false),
    FURTO(false),
    /** Troca com cliente: o produto com defeito sai e o novo vai para o cliente (D37). */
    TROCA(false),
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
