package com.empresax.sistema.pdv;

/**
 * Como o consumidor pagou no balcão. Cartão e Pix passam pela maquininha e exigem o código de
 * autorização (vai para a NFC-e); cartão exige também a bandeira. Dinheiro exige o valor recebido.
 */
public enum FormaPagamentoPresencial {
    DINHEIRO(false, false),
    CARTAO_CREDITO(true, true),
    CARTAO_DEBITO(true, true),
    PIX(true, false);

    private final boolean passaPelaMaquininha;
    private final boolean exigeBandeira;

    FormaPagamentoPresencial(boolean passaPelaMaquininha, boolean exigeBandeira) {
        this.passaPelaMaquininha = passaPelaMaquininha;
        this.exigeBandeira = exigeBandeira;
    }

    public boolean passaPelaMaquininha() {
        return passaPelaMaquininha;
    }

    public boolean exigeBandeira() {
        return exigeBandeira;
    }
}
