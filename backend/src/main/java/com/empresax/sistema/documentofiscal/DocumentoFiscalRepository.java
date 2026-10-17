package com.empresax.sistema.documentofiscal;

import com.empresax.sistema.pedido.StatusPedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentoFiscalRepository extends JpaRepository<DocumentoFiscal, UUID> {

    List<DocumentoFiscal> findByPedidoId(UUID pedidoId);

    List<DocumentoFiscal> findByOrderByAtualizadoEmDesc(Pageable pagina);

    @Query("select new com.empresax.sistema.documentofiscal.QuantidadePorStatus(d.status, count(d))"
            + " from DocumentoFiscal d group by d.status")
    List<QuantidadePorStatus> contarPorStatus();

    @Query("select p.id from Pedido p where p.status = :status"
            + " and not exists (select d.id from DocumentoFiscal d where d.pedidoId = p.id)"
            + " order by p.criadoEm desc")
    List<UUID> findIdsDePedidosSemDocumento(@Param("status") StatusPedido status, Pageable pagina);
}
