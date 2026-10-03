package com.empresax.sistema.cobranca.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.cobranca.CobrancaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Visão geral de todas as cobranças (aba Cobranças), além da visão por pedido. */
@RestController
@RequestMapping("/api/cobrancas")
public class CobrancasConsultaController {

    private final CobrancaService cobrancaService;

    public CobrancasConsultaController(CobrancaService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    @PreAuthorize(RegraAcesso.COBRANCAS_VER)
    @GetMapping
    public List<CobrancaResponse> listar() {
        return cobrancaService.listarTodas().stream()
                .map(CobrancaResponse::de)
                .toList();
    }
}
