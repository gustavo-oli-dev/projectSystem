package com.empresax.sistema.pedido;

import com.empresax.sistema.documentofiscal.TipoDocumentoFiscal;

/**
 * Por onde a venda aconteceu. O canal decide o documento fiscal do produto: venda presencial ao
 * consumidor é NFC-e (modelo 65); venda à distância é NF-e (modelo 55). Serviço é sempre NFS-e.
 */
public enum CanalVenda {
    PAINEL(TipoDocumentoFiscal.NFE),
    BALCAO(TipoDocumentoFiscal.NFCE);

    private final TipoDocumentoFiscal documentoDeProduto;

    CanalVenda(TipoDocumentoFiscal documentoDeProduto) {
        this.documentoDeProduto = documentoDeProduto;
    }

    public TipoDocumentoFiscal documentoPara(TipoItem tipoItem) {
        return tipoItem == TipoItem.PRODUTO ? documentoDeProduto : tipoItem.documentoFiscal();
    }
}
