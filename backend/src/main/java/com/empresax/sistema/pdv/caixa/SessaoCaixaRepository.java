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

    /** Reposição, sangria e fechamento travam o caixa: duas pessoas não mexem no mesmo saldo ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SessaoCaixa s WHERE s.id = :id")
    Optional<SessaoCaixa> buscarParaAlterar(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SessaoCaixa s WHERE s.operador = :operador AND s.status = com.empresax.sistema.pdv.caixa.StatusSessaoCaixa.ABERTA")
    Optional<SessaoCaixa> buscarAbertaDoOperadorParaAlterar(@Param("operador") String operador);

    /** Caixas abertos agora (tela de gestão): os movimentos vêm junto. */
    @EntityGraph(attributePaths = "movimentos")
    List<SessaoCaixa> findByStatusOrderByAbertaEmAsc(StatusSessaoCaixa status);

    /** Lista da conferência: os movimentos vêm junto (os totais de reposição/sangria saem deles). */
    @EntityGraph(attributePaths = "movimentos")
    List<SessaoCaixa> findTop100ByOrderByAbertaEmDesc();
}
