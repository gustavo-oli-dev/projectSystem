package com.empresax.sistema.compras.contas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContaAPagarRepository extends JpaRepository<ContaAPagar, UUID> {

    List<ContaAPagar> findAllByOrderByVencimentoAsc();
}
