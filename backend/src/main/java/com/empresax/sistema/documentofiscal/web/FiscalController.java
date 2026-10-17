package com.empresax.sistema.documentofiscal.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.documentofiscal.ConfiguracaoFiscal;
import com.empresax.sistema.documentofiscal.DocumentoFiscalService;
import com.empresax.sistema.pedido.web.PedidoResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class FiscalController {

    private static final String LIMITE_PADRAO = "200";
    private static final int LIMITE_MAXIMO = 1000;

    private final DocumentoFiscalService documentoFiscalService;
    private final ConfiguracaoFiscal configuracaoFiscal;

    public FiscalController(DocumentoFiscalService documentoFiscalService, ConfiguracaoFiscal configuracaoFiscal) {
        this.documentoFiscalService = documentoFiscalService;
        this.configuracaoFiscal = configuracaoFiscal;
    }

    /** Os mais recentes primeiro (a lista completa cresce com cada venda). */
    @PreAuthorize(RegraAcesso.FISCAL_VER)
    @GetMapping("/api/documentos-fiscais")
    public List<DocumentoFiscalResponse> listar(
            @RequestParam(defaultValue = LIMITE_PADRAO)
            @Min(value = 1, message = "O limite deve ser de pelo menos 1 documento")
            @Max(value = LIMITE_MAXIMO, message = "O limite máximo é de 1000 documentos por vez") int limite
    ) {
        return documentoFiscalService.listarMaisRecentes(limite).stream()
                .map(DocumentoFiscalResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.FISCAL_VER)
    @GetMapping("/api/documentos-fiscais/resumo")
    public List<QuantidadeDocumentosResponse> resumo() {
        return documentoFiscalService.contarPorStatus().stream()
                .map(QuantidadeDocumentosResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.FISCAL_VER)
    @GetMapping("/api/fiscal/pedidos-aguardando")
    public List<PedidoResponse> pedidosAguardando(
            @RequestParam(defaultValue = LIMITE_PADRAO)
            @Min(value = 1, message = "O limite deve ser de pelo menos 1 pedido")
            @Max(value = LIMITE_MAXIMO, message = "O limite máximo é de 1000 pedidos por vez") int limite
    ) {
        return documentoFiscalService.listarPedidosSemDocumento(limite).stream()
                .map(pedido -> PedidoResponse.de(pedido, null))
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
