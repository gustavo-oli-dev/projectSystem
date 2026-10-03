package com.empresax.sistema.cobranca;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CobrancaRepository extends JpaRepository<Cobranca, UUID> {

    List<Cobranca> findByPedidoId(UUID pedidoId);

    Optional<Cobranca> findByReferenciaExterna(String referenciaExterna);

    List<Cobranca> findByStatus(StatusCobranca status);

    List<Cobranca> findByStatusAndCriadoEmBetween(StatusCobranca status, Instant inicio, Instant fim);
}
