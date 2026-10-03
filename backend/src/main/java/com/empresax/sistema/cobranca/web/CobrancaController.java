package com.empresax.sistema.cobranca.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.cobranca.CobrancaCriada;
import com.empresax.sistema.cobranca.CobrancaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos/{pedidoId}/cobrancas")
public class CobrancaController {

    private final CobrancaService cobrancaService;

    public CobrancaController(CobrancaService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    @PreAuthorize(RegraAcesso.COBRANCAS_GERENCIAR)
    @PostMapping
    public ResponseEntity<CobrancaResponse> criar(
            @PathVariable UUID pedidoId, @Valid @RequestBody CriarCobrancaRequest requisicao
    ) {
        CobrancaCriada cobrancaCriada = cobrancaService.criar(pedidoId, requisicao.meio());
        CobrancaResponse resposta = CobrancaResponse.de(cobrancaCriada);
        URI local = URI.create("/api/pedidos/" + pedidoId + "/cobrancas/" + cobrancaCriada.cobranca().id());
        return ResponseEntity.created(local).body(resposta);
    }

    @PreAuthorize(RegraAcesso.COBRANCAS_VER)
    @GetMapping
    public List<CobrancaResponse> listar(@PathVariable UUID pedidoId) {
        return cobrancaService.listarPorPedido(pedidoId).stream()
                .map(CobrancaResponse::de)
                .toList();
    }
}
