package com.empresax.sistema.produto.foto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FotoProdutoRepository extends JpaRepository<FotoProduto, UUID> {

    @Query("select new com.empresax.sistema.produto.foto.ResumoFoto(f.id, f.produtoId, f.ordem) "
            + "from FotoProduto f order by f.produtoId, f.ordem")
    List<ResumoFoto> listarResumos();

    @Query("select coalesce(max(f.ordem), -1) from FotoProduto f where f.produtoId = :produtoId")
    int maiorOrdem(UUID produtoId);

    long countByProdutoId(UUID produtoId);

    Optional<FotoProduto> findByIdAndProdutoId(UUID id, UUID produtoId);
}
