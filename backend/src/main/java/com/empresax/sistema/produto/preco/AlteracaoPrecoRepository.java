package com.empresax.sistema.produto.preco;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AlteracaoPrecoRepository extends JpaRepository<AlteracaoPreco, UUID> {

    List<AlteracaoPreco> findByAlteradoEmGreaterThanEqualOrderByAlteradoEmDesc(Instant desde);
}
