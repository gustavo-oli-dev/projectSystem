package com.empresax.sistema.promocao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PromocaoRepository extends JpaRepository<Promocao, UUID> {

    List<Promocao> findAllByOrderByInicioDesc();

    List<Promocao> findByProdutoId(UUID produtoId);

    /** Promoções não encerradas que valem no dia, dos produtos pedidos (uma consulta, sem N+1). */
    @Query("""
            SELECT p FROM Promocao p
            WHERE p.produtoId IN :produtos AND p.encerradaEm IS NULL AND p.inicio <= :dia AND p.fim >= :dia
            """)
    List<Promocao> valendoNoDia(@Param("produtos") Collection<UUID> produtos, @Param("dia") LocalDate dia);

    @Query("SELECT p FROM Promocao p WHERE p.encerradaEm IS NULL AND p.inicio <= :dia AND p.fim >= :dia")
    List<Promocao> todasValendoNoDia(@Param("dia") LocalDate dia);
}
