package com.empresax.sistema.produto;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    List<Produto> findByAtivoTrue();

    List<Produto> findAllByOrderByNomeAsc();

    Optional<Produto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, UUID id);

    /**
     * Trava a linha do produto até o fim da transação (SELECT ... FOR UPDATE): duas vendas ao mesmo
     * tempo não conseguem baixar a mesma última unidade.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarParaAlterarEstoque(@Param("id") UUID id);
}
