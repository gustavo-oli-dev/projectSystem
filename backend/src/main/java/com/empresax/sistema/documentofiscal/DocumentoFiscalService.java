package com.empresax.sistema.documentofiscal;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gera os documentos fiscais de um pedido confirmado, no status PENDENTE. A transmissão à
 * SEFAZ/prefeitura não acontece aqui: depende de certificado A1, regime tributário e credenciais
 * do provedor (pendências em DECISOES.md) — ver ConfiguracaoFiscal.
 */
@Service
public class DocumentoFiscalService {

    private final DocumentoFiscalRepository documentoFiscalRepository;
    private final PedidoService pedidoService;

    public DocumentoFiscalService(DocumentoFiscalRepository documentoFiscalRepository, PedidoService pedidoService) {
        this.documentoFiscalRepository = documentoFiscalRepository;
        this.pedidoService = pedidoService;
    }

    @Transactional(readOnly = true)
    public List<DocumentoFiscal> listarTodos() {
        return documentoFiscalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DocumentoFiscal> listarPorPedido(UUID pedidoId) {
        return documentoFiscalRepository.findByPedidoId(pedidoId);
    }

    @Transactional
    public List<DocumentoFiscal> gerarPendentes(UUID pedidoId) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        if (pedido.status() != StatusPedido.AGUARDANDO_EMISSAO) {
            throw new DomainException("Só é possível gerar documentos fiscais de um pedido confirmado");
        }
        if (!documentoFiscalRepository.findByPedidoId(pedidoId).isEmpty()) {
            throw new DomainException("Os documentos fiscais deste pedido já foram gerados");
        }

        List<DocumentoFiscal> documentos = pedido.documentosFiscaisNecessarios().stream()
                .map(tipo -> new DocumentoFiscal(pedidoId, tipo))
                .toList();
        return documentoFiscalRepository.saveAll(documentos);
    }

    /** Chamado ao desfazer uma venda, na mesma transação do cancelamento. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void descartarPendentes(UUID pedidoId) {
        documentoFiscalRepository.findByPedidoId(pedidoId).forEach(DocumentoFiscal::descartarPendente);
    }
}
