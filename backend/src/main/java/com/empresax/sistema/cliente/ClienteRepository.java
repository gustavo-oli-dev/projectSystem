package com.empresax.sistema.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByTelefoneWhatsapp(String telefoneWhatsapp);
}
