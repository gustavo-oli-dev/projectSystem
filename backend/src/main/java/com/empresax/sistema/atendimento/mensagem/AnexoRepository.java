package com.empresax.sistema.atendimento.mensagem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnexoRepository extends JpaRepository<Anexo, UUID> {

    List<Anexo> findByMensagemId(UUID mensagemId);
}
