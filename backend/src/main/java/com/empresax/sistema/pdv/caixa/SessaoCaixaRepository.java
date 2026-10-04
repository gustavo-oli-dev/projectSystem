package com.empresax.sistema.pdv.caixa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoCaixaRepository extends JpaRepository<SessaoCaixa, UUID> {

    Optional<SessaoCaixa> findByOperadorAndStatus(String operador, StatusSessaoCaixa status);

    /** Sangria e fechamento travam o caixa: duas abas não registram ao mesmo tempo sobre o mesmo saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SessaoCaixa s WHERE s.operador = :operador AND s.status = com.empresax.sistema.pdv.caixa.StatusSessaoCaixa.ABERTA")
    Optional<SessaoCaixa> buscarAbertaParaAlterar(@Param("operador") String operador);

    /** Lista da conferência: os movimentos vêm junto (os totais de suprimento/sangria saem deles). */
    @EntityGraph(attributePaths = "movimentos")
    List<SessaoCaixa> findTop100ByOrderByAbertaEmDesc();
}
