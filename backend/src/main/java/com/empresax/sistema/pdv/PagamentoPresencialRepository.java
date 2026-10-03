package com.empresax.sistema.pdv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoPresencialRepository extends JpaRepository<PagamentoPresencial, UUID> {

    Optional<PagamentoPresencial> findByPedidoId(UUID pedidoId);

    List<PagamentoPresencial> findByPedidoIdIn(Collection<UUID> pedidoIds);
}
