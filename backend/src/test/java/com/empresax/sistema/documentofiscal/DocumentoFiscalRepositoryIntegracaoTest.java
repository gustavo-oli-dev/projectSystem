package com.empresax.sistema.documentofiscal;

import com.empresax.sistema.integracao.PostgresDeTeste;
import com.empresax.sistema.pedido.StatusPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresDeTeste.class)
class DocumentoFiscalRepositoryIntegracaoTest {

    private static final Timestamp AGORA = Timestamp.from(Instant.parse("2026-10-17T15:00:00Z"));

    private final DocumentoFiscalRepository documentoFiscalRepository;
    private final JdbcTemplate jdbc;

    @Autowired
    DocumentoFiscalRepositoryIntegracaoTest(DocumentoFiscalRepository documentoFiscalRepository, JdbcTemplate jdbc) {
        this.documentoFiscalRepository = documentoFiscalRepository;
        this.jdbc = jdbc;
    }

    @Test
    void contagemPorStatusSomaCadaStatusNoBanco() {
        emitir(pedido("CONCLUIDO"), "AUTORIZADO");
        emitir(pedido("CONCLUIDO"), "AUTORIZADO");
        emitir(pedido("AGUARDANDO_EMISSAO"), "PENDENTE");

        assertThat(documentoFiscalRepository.contarPorStatus()).containsExactlyInAnyOrder(
                new QuantidadePorStatus(StatusDocumentoFiscal.AUTORIZADO, 2),
                new QuantidadePorStatus(StatusDocumentoFiscal.PENDENTE, 1));
    }

    @Test
    void pedidosSemDocumentoTrazSoConfirmadosQueAindaNaoTemNota() {
        UUID semNota = pedido("AGUARDANDO_EMISSAO");
        emitir(pedido("AGUARDANDO_EMISSAO"), "PENDENTE");
        pedido("CONCLUIDO");

        assertThat(documentoFiscalRepository.findIdsDePedidosSemDocumento(StatusPedido.AGUARDANDO_EMISSAO, PageRequest.of(0, 10)))
                .containsExactly(semNota);
    }

    private UUID pedido(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into pedidos (id, status, criado_em, canal, confirmado_em) values (?, ?, ?, 'BALCAO', ?)",
                id, status, AGORA, AGORA);
        return id;
    }

    private void emitir(UUID pedidoId, String status) {
        jdbc.update("insert into documentos_fiscais (id, pedido_id, tipo, status, criado_em, atualizado_em) values (?, ?, 'NFCE', ?, ?, ?)",
                UUID.randomUUID(), pedidoId, status, AGORA, AGORA);
    }
}
