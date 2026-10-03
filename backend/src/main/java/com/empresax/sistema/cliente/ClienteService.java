package com.empresax.sistema.cliente;

import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.shared.documento.Documento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
