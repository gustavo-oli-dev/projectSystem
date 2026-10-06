package com.empresax.sistema.cliente;

import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.shared.documento.Documento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente cadastrar(String nome, Documento documento, String telefoneWhatsapp) {
        Cliente cliente = new Cliente(nome, documento, telefoneWhatsapp);
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    /** Nome de cada cliente, numa consulta só (listas de pedidos, sem N+1). */
    @Transactional(readOnly = true)
    public Map<UUID, String> nomesPorId(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return clienteRepository.findAllById(ids).stream().collect(Collectors.toMap(Cliente::id, Cliente::nome));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não encontrado: " + id));
    }

    @Transactional
    public Cliente atualizarContato(UUID id, String nome, String telefoneWhatsapp) {
        Cliente cliente = buscarPorId(id);
        cliente.atualizarNome(nome);
        cliente.atualizarTelefoneWhatsapp(telefoneWhatsapp);
        return cliente;
    }
}
