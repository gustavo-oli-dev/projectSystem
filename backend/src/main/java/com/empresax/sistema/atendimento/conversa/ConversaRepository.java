package com.empresax.sistema.atendimento.conversa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

    Optional<Conversa> findByTelefoneWhatsappAndStatus(String telefoneWhatsapp, StatusConversa status);

    List<Conversa> findByStatus(StatusConversa status);
}
