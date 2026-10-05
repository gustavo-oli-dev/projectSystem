package com.empresax.sistema.compras.contato;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.shared.documento.Documento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContatoService {

    private final ContatoRepository contatoRepository;

    public ContatoService(ContatoRepository contatoRepository) {
        this.contatoRepository = contatoRepository;
    }

    @Transactional(readOnly = true)
    public List<Contato> listar() {
        return contatoRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Contato buscarPorId(UUID id) {
        return contatoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Contato não encontrado: " + id));
    }

    @Transactional
    public Contato cadastrar(DadosContato dados) {
        Contato contato = new Contato(dados.tipo(), dados.nome(), dados.documento(), dados.telefone(), dados.email(), dados.observacao());
        contato.documento().ifPresent(documento -> {
            if (contatoRepository.existsByDocumento(documento)) {
                throw new DomainException("Já existe um contato com este CNPJ/CPF");
            }
        });
        return contatoRepository.save(contato);
    }

    @Transactional
    public Contato alterar(UUID id, DadosContato dados) {
        Contato contato = buscarPorId(id);
        contato.alterar(dados.tipo(), dados.nome(), dados.documento(), dados.telefone(), dados.email(), dados.observacao());
        contato.documento().ifPresent(documento -> {
            if (contatoRepository.existsByDocumentoAndIdNot(documento, id)) {
                throw new DomainException("Já existe outro contato com este CNPJ/CPF");
            }
        });
        return contato;
    }

    @Transactional
    public Contato definirAtivo(UUID id, boolean ativo) {
        Contato contato = buscarPorId(id);
        if (ativo) {
            contato.ativar();
        } else {
            contato.desativar();
        }
        return contato;
    }

    @Transactional(readOnly = true)
    public Optional<Contato> buscarPorDocumento(String documento) {
        return contatoRepository.findByDocumento(Documento.criar(documento).valor());
    }

    /** Fornecedor da nota: o já cadastrado pelo CNPJ/CPF ou um novo, com os dados da própria nota. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Contato fornecedorDaNota(String documento, String nome, String telefone) {
        return buscarPorDocumento(documento)
                .orElseGet(() -> contatoRepository.save(new Contato(TipoContato.FORNECEDOR, nome, documento, telefone, null, null)));
    }

    /** Campos editáveis de um contato (vindos da tela). */
    public record DadosContato(TipoContato tipo, String nome, String documento, String telefone, String email, String observacao) {
    }
}
