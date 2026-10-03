package com.empresax.sistema.servico;

import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Transactional
    public Servico cadastrar(
            String nome, String descricao, String codigoServicoLc116, BigDecimal aliquotaIss, Dinheiro precoUnitario
    ) {
        Servico servico = new Servico(nome, descricao, codigoServicoLc116, aliquotaIss, precoUnitario);
        return servicoRepository.save(servico);
    }

    @Transactional(readOnly = true)
    public List<Servico> listarTodos() {
        return servicoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Servico buscarPorId(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Serviço não encontrado: " + id));
    }

    @Transactional
    public Servico atualizar(UUID id, String nome, String descricao, Dinheiro precoUnitario) {
        Servico servico = buscarPorId(id);
        servico.atualizar(nome, descricao, precoUnitario);
        return servico;
    }

    @Transactional
    public void desativar(UUID id) {
        buscarPorId(id).desativar();
    }
}
