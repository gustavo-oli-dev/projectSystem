package com.empresax.sistema.pdv.autorizacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ItemCanceladoCaixaRepository extends JpaRepository<ItemCanceladoCaixa, UUID> {

    List<ItemCanceladoCaixa> findByCanceladoEmGreaterThanEqualAndCanceladoEmLessThanOrderByCanceladoEmDesc(Instant comeco, Instant fim);
}
