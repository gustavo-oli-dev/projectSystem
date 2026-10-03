package com.empresax.sistema.documentofiscal.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.documentofiscal.ConfiguracaoFiscal;
import com.empresax.sistema.documentofiscal.DocumentoFiscalService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FiscalController {

    private final DocumentoFiscalService documentoFiscalService;
    private final ConfiguracaoFiscal configuracaoFiscal;

    public FiscalController(DocumentoFiscalService documentoFiscalService, ConfiguracaoFiscal configuracaoFiscal) {
        this.documentoFiscalService = documentoFiscalService;
        this.configuracaoFiscal = configuracaoFiscal;
    }

    @PreAuthorize(RegraAcesso.FISCAL_VER)
    @GetMapping("/api/documentos-fiscais")
    public List<DocumentoFiscalResponse> listar() {
        return documentoFiscalService.listarTodos().stream()
                .map(DocumentoFiscalResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.FISCAL_VER)
    @GetMapping("/api/pedidos/{pedidoId}/documentos-fiscais")
    public List<DocumentoFiscalResponse> listarDoPedido(@PathVariable UUID pedidoId) {
        return documentoFiscalService.listarPorPedido(pedidoId).stream()
                .map(DocumentoFiscalResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.FISCAL_GERENCIAR)
    @PostMapping("/api/pedidos/{pedidoId}/documentos-fiscais")
    @ResponseStatus(HttpStatus.CREATED)
    public List<DocumentoFiscalResponse> gerarPendentes(@PathVariable UUID pedidoId) {
        return documentoFiscalService.gerarPendentes(pedidoId).stream()
                .map(DocumentoFiscalResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.FISCAL_VER)
    @GetMapping("/api/fiscal/configuracao")
    public ConfiguracaoFiscalResponse configuracao() {
        return ConfiguracaoFiscalResponse.de(configuracaoFiscal);
    }
}
