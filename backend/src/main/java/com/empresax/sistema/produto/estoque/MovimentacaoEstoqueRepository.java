package com.empresax.sistema.produto.estoque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    List<MovimentacaoEstoque> findTop50ByProdutoIdOrderByCriadaEmDesc(UUID produtoId);
}
