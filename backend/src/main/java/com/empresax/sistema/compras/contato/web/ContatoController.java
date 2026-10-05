package com.empresax.sistema.compras.contato.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.compras.contato.ContatoService;
import com.empresax.sistema.compras.contato.ContatoService.DadosContato;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Contatos: fornecedores, transportadoras e outros. A lista também serve às contas a pagar. */
@RestController
@RequestMapping("/api/contatos")
public class ContatoController {

    private final ContatoService contatoService;

    public ContatoController(ContatoService contatoService) {
        this.contatoService = contatoService;
    }

    @PreAuthorize(RegraAcesso.CONTATOS_GERENCIAR + " or " + RegraAcesso.CONTAS_PAGAR_GERENCIAR)
    @GetMapping
    public List<ContatoResponse> listar() {
        return contatoService.listar().stream().map(ContatoResponse::de).toList();
    }

    @PreAuthorize(RegraAcesso.CONTATOS_GERENCIAR)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContatoResponse cadastrar(@Valid @RequestBody ContatoRequest requisicao) {
        return ContatoResponse.de(contatoService.cadastrar(dados(requisicao)));
    }

    @PreAuthorize(RegraAcesso.CONTATOS_GERENCIAR)
    @PutMapping("/{id}")
    public ContatoResponse alterar(@PathVariable UUID id, @Valid @RequestBody ContatoRequest requisicao) {
        return ContatoResponse.de(contatoService.alterar(id, dados(requisicao)));
    }

    @PreAuthorize(RegraAcesso.CONTATOS_GERENCIAR)
    @PostMapping("/{id}/desativar")
    public ContatoResponse desativar(@PathVariable UUID id) {
        return ContatoResponse.de(contatoService.definirAtivo(id, false));
    }

    @PreAuthorize(RegraAcesso.CONTATOS_GERENCIAR)
    @PostMapping("/{id}/ativar")
    public ContatoResponse ativar(@PathVariable UUID id) {
        return ContatoResponse.de(contatoService.definirAtivo(id, true));
    }

    private static DadosContato dados(ContatoRequest requisicao) {
        return new DadosContato(requisicao.tipo(), requisicao.nome(), requisicao.documento(), requisicao.telefone(),
                requisicao.email(), requisicao.observacao());
    }
}
