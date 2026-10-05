package com.empresax.sistema.compras.entrada;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotaEntradaRepository extends JpaRepository<NotaEntrada, UUID> {

    Optional<NotaEntrada> findByChaveAcesso(String chaveAcesso);

    List<NotaEntrada> findTop50ByOrderByRegistradaEmDesc();
}
