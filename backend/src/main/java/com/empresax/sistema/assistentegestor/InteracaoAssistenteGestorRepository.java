package com.empresax.sistema.assistentegestor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InteracaoAssistenteGestorRepository extends JpaRepository<InteracaoAssistenteGestor, UUID> {
}
