package com.empresax.sistema.compras.contato;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContatoRepository extends JpaRepository<Contato, UUID> {

    Optional<Contato> findByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, UUID id);

    boolean existsByDocumento(String documento);

    List<Contato> findAllByOrderByNomeAsc();
}
