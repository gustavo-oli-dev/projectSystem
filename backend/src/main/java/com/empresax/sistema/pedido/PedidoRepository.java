package com.empresax.sistema.pedido;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os itens do pedido são uma coleção lazy. Toda consulta cujo resultado é convertido em DTO (fora da
 * transação, já que open-in-view está desligado) carrega os itens junto via @EntityGraph — evita
 * LazyInitializationException e o problema de N+1 (uma query extra por pedido).
 *
 * Listas com limite buscam primeiro só os ids (o LIMIT vai para o SQL) e depois os pedidos com itens:
 * limite junto com @EntityGraph de coleção faz o Hibernate carregar a tabela inteira e cortar em memória.
 */
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    long countByStatus(StatusPedido status);

    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findComItensById(UUID id);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findComItensByIdInOrderByCriadoEmDesc(Collection<UUID> ids);

    @Query("select p.id from Pedido p order by p.criadoEm desc")
    List<UUID> findIdsMaisRecentes(Pageable pagina);

    @Query("select p.id from Pedido p where p.canal = :canal order by p.criadoEm desc")
    List<UUID> findIdsMaisRecentesDoCanal(@Param("canal") CanalVenda canal, Pageable pagina);

    @Query("select p.id from Pedido p where p.clienteId = :clienteId order by p.criadoEm desc")
    List<UUID> findIdsMaisRecentesDoCliente(@Param("clienteId") UUID clienteId, Pageable pagina);
}
