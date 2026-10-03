package com.empresax.sistema.pedido;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os itens do pedido são uma coleção lazy. Toda consulta cujo resultado é convertido em DTO (fora da
 * transação, já que open-in-view está desligado) carrega os itens junto via @EntityGraph — evita
 * LazyInitializationException e o problema de N+1 (uma query extra por pedido).
 */
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    long countByStatus(StatusPedido status);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findComItensById(UUID id);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findTop10ByClienteIdOrderByCriadoEmDesc(UUID clienteId);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findTop50ByCanalOrderByCriadoEmDesc(CanalVenda canal);
}
