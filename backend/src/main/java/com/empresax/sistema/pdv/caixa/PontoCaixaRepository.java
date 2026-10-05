package com.empresax.sistema.pdv.caixa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PontoCaixaRepository extends JpaRepository<PontoCaixa, UUID> {

    List<PontoCaixa> findAllByOrderByNumeroAsc();

    boolean existsByNumero(int numero);
}
