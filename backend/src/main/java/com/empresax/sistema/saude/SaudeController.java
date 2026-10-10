package com.empresax.sistema.saude;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint público de propósito único: dizer a um orquestrador externo (ex.: o health check do
 * Render) que o processo subiu e está respondendo. Não expõe nenhum dado do negócio — só confirma
 * que a aplicação está viva (liberado em SecurityConfig, ENDPOINTS_PUBLICOS).
 */
@RestController
public class SaudeController {

    @GetMapping("/api/saude")
    public Map<String, String> saude() {
        return Map.of("status", "ok");
    }
}
