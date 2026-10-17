package com.empresax.sistema.pedido;

import com.empresax.sistema.integracao.PostgresDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresDeTeste.class)
class PedidoRepositoryIntegracaoTest {

    private static final Instant AGORA = Instant.parse("2026-10-17T15:00:00Z");

    private final PedidoRepository pedidoRepository;
    private final JdbcTemplate jdbc;

    @Autowired
    PedidoRepositoryIntegracaoTest(PedidoRepository pedidoRepository, JdbcTemplate jdbc) {
        this.pedidoRepository = pedidoRepository;
        this.jdbc = jdbc;
    }

    @Test
    void ultimasDoBalcaoTrazSoAsMaisNovasDoBalcaoComOsItens() {
        UUID maisAntiga = venderNoBalcao(3);
        UUID intermediaria = venderNoBalcao(2);
        UUID maisNova = venderNoBalcao(1);

        List<UUID> ids = pedidoRepository.findIdsMaisRecentesDoCanal(CanalVenda.BALCAO, PageRequest.of(0, 2));
        List<Pedido> pedidos = pedidoRepository.findComItensByIdInOrderByCriadoEmDesc(ids);

        assertThat(ids).containsExactly(maisNova, intermediaria).doesNotContain(maisAntiga);
        assertThat(pedidos).extracting(Pedido::id).containsExactly(maisNova, intermediaria);
        assertThat(pedidos).allSatisfy(pedido -> assertThat(pedido.itens()).hasSize(1));
    }

    @Test
    void maisRecentesRespeitaOLimitePedido() {
        for (int minutos = 10; minutos > 0; minutos--) {
            venderNoBalcao(minutos);
        }

        assertThat(pedidoRepository.findIdsMaisRecentes(PageRequest.of(0, 4))).hasSize(4);
    }

    private UUID venderNoBalcao(int minutosAtras) {
        UUID id = UUID.randomUUID();
        Timestamp quando = Timestamp.from(AGORA.minus(minutosAtras, ChronoUnit.MINUTES));
        jdbc.update("insert into pedidos (id, status, criado_em, canal, confirmado_em) values (?, 'CONCLUIDO', ?, 'BALCAO', ?)",
                id, quando, quando);
        jdbc.update("insert into itens_pedido (pedido_id, tipo, referencia_id, descricao, preco_unitario, quantidade)"
                + " values (?, 'PRODUTO', ?, 'Arroz 5kg', 27.90, 1)", id, UUID.randomUUID());
        return id;
    }
}
