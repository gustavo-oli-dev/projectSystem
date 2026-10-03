package com.empresax.sistema.acesso;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CargoRepository extends JpaRepository<Cargo, UUID> {

    @EntityGraph(attributePaths = "permissoes")
    List<Cargo> findAllByOrderByNomeAsc();

    @EntityGraph(attributePaths = "permissoes")
    Optional<Cargo> findComPermissoesById(UUID id);

    boolean existsByNomeIgnoreCase(String nome);
}
