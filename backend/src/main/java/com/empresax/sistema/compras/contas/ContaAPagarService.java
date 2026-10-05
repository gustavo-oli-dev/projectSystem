package com.empresax.sistema.compras.contas;

import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.compras.contato.ContatoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ContaAPagarService {

    private final ContaAPagarRepository contaRepository;
    private final ContatoService contatoService;

    public ContaAPagarService(ContaAPagarRepository contaRepository, ContatoService contatoService) {
        this.contaRepository = contaRepository;
        this.contatoService = contatoService;
    }

    @Transactional(readOnly = true)
    public List<ContaAPagar> listar() {
        return contaRepository.findAllByOrderByVencimentoAsc();
    }

    @Transactional
    public ContaAPagar lancar(UUID contatoId, String descricao, Dinheiro valor, LocalDate vencimento, String criadaPor) {
        if (contatoId != null) {
            contatoService.buscarPorId(contatoId);
        }
        return contaRepository.save(ContaAPagar.lancar(contatoId, descricao, valor, vencimento, criadaPor));
    }

    /** Parcelas da nota de entrada: criadas junto com a entrada no estoque (mesma transação). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lancarParcelaDaNota(UUID fornecedorId, UUID notaId, String descricao, Dinheiro valor, LocalDate vencimento, String criadaPor) {
        contaRepository.save(ContaAPagar.parcelaDaNota(fornecedorId, notaId, descricao, valor, vencimento, criadaPor));
    }

    @Transactional
    public ContaAPagar pagar(UUID id, String quem) {
        ContaAPagar conta = buscar(id);
        conta.pagar(quem);
        return conta;
    }

    @Transactional
    public ContaAPagar cancelar(UUID id) {
        ContaAPagar conta = buscar(id);
        conta.cancelar();
        return conta;
    }

    private ContaAPagar buscar(UUID id) {
        return contaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta não encontrada: " + id));
    }
}
