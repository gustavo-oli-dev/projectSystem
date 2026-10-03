package com.empresax.sistema.usuario.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificacaoWhatsAppRepository extends JpaRepository<VerificacaoWhatsApp, UUID> {
}
