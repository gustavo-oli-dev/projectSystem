package com.empresax.sistema.usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas que viram autorização ou DTO carregam cargo e permissões juntos (open-in-view está
 * desligado; sem isso haveria LazyInitializationException ou N+1).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = {"cargo", "cargo.permissoes"})
    Optional<Usuario> findComPermissoesByEmail(String email);

    @EntityGraph(attributePaths = {"cargo", "cargo.permissoes"})
    Optional<Usuario> findComPermissoesById(UUID id);

    @EntityGraph(attributePaths = {"cargo", "cargo.permissoes"})
    List<Usuario> findAllByOrderByNomeAsc();

    @EntityGraph(attributePaths = {"cargo", "cargo.permissoes"})
    Optional<Usuario> findComPermissoesByTelefoneWhatsapp(String telefoneWhatsapp);

    boolean existsByTelefoneWhatsappAndIdNot(String telefoneWhatsapp, UUID id);

    long countByCargoId(UUID cargoId);
}
