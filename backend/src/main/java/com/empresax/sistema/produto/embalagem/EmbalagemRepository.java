package com.empresax.sistema.produto.embalagem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EmbalagemRepository extends JpaRepository<Embalagem, UUID> {

    List<Embalagem> findByAtivaTrue();

    List<Embalagem> findByProdutoIdAndAtivaTrue(UUID produtoId);

    List<Embalagem> findByIdIn(Collection<UUID> ids);

    /** Código único entre embalagens (inclusive as removidas: a coluna é UNIQUE). */
    boolean existsByCodigoBarras(String codigoBarras);
}
