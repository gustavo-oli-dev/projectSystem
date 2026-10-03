package com.empresax.sistema.acesso;

import com.empresax.sistema.common.domain.AcessoNegadoException;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;
    private final UsuarioRepository usuarioRepository;

    public CargoService(CargoRepository cargoRepository, UsuarioRepository usuarioRepository) {
        this.cargoRepository = cargoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Cargo> listar() {
        return cargoRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Cargo buscarPorId(UUID id) {
        return cargoRepository.findComPermissoesById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Perfil de acesso não encontrado: " + id));
    }

    @Transactional
    public Cargo criar(String emailAtor, String nome, String descricao, Set<Permissao> permissoes) {
        garantirPodeConceder(emailAtor, permissoes);
        if (nome != null && cargoRepository.existsByNomeIgnoreCase(nome.trim())) {
            throw new DomainException("Já existe um perfil de acesso com este nome");
        }
        return cargoRepository.save(new Cargo(nome, descricao, permissoes));
    }

    @Transactional
    public Cargo atualizar(String emailAtor, UUID id, String nome, String descricao, Set<Permissao> permissoes) {
        garantirPodeConceder(emailAtor, permissoes);
        Cargo cargo = buscarPorId(id);
        cargo.redefinir(nome, descricao, permissoes);
        return cargo;
    }

    @Transactional
    public void excluir(UUID id) {
        Cargo cargo = buscarPorId(id);
        long usuariosNoCargo = usuarioRepository.countByCargoId(id);
        if (usuariosNoCargo > 0) {
            throw new DomainException("Perfil de acesso em uso por " + usuariosNoCargo + " funcionário(s). Troque o perfil deles antes de excluir.");
        }
        cargoRepository.delete(cargo);
    }

    private void garantirPodeConceder(String emailAtor, Set<Permissao> permissoes) {
        Usuario ator = usuarioRepository.findComPermissoesByEmail(emailAtor)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado: " + emailAtor));
        if (!ator.podeConceder(permissoes)) {
            throw new AcessoNegadoException("Você não pode criar um perfil de acesso com permissões que você mesmo não tem");
        }
    }
}
