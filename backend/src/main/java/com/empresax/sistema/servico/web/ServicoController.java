package com.empresax.sistema.servico.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.servico.Servico;
import com.empresax.sistema.servico.ServicoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PostMapping
    public ResponseEntity<ServicoResponse> cadastrar(@Valid @RequestBody CriarServicoRequest requisicao) {
        Servico servico = servicoService.cadastrar(
                requisicao.nome(),
                requisicao.descricao(),
                requisicao.codigoServicoLc116(),
                requisicao.aliquotaIss(),
                new Dinheiro(requisicao.precoUnitario()));
        ServicoResponse resposta = ServicoResponse.de(servico);
        return ResponseEntity.created(URI.create("/api/servicos/" + servico.id())).body(resposta);
    }

    @PreAuthorize(RegraAcesso.CATALOGO_VER)
    @GetMapping
    public List<ServicoResponse> listar() {
        return servicoService.listarTodos().stream()
                .map(ServicoResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.CATALOGO_VER)
    @GetMapping("/{id}")
    public ServicoResponse buscarPorId(@PathVariable UUID id) {
        return ServicoResponse.de(servicoService.buscarPorId(id));
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PutMapping("/{id}")
    public ServicoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarServicoRequest requisicao) {
        Servico servico = servicoService.atualizar(
                id, requisicao.nome(), requisicao.descricao(), new Dinheiro(requisicao.precoUnitario()));
        return ServicoResponse.de(servico);
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        servicoService.desativar(id);
    }
}
