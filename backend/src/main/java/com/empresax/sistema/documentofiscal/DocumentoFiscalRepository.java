package com.empresax.sistema.documentofiscal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoFiscalRepository extends JpaRepository<DocumentoFiscal, UUID> {

    List<DocumentoFiscal> findByPedidoId(UUID pedidoId);
}
