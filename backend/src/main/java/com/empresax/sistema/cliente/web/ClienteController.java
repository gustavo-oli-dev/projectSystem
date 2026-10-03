package com.empresax.sistema.cliente.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.cliente.Cliente;
import com.empresax.sistema.cliente.ClienteService;
import com.empresax.sistema.shared.documento.Documento;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PreAuthorize(RegraAcesso.CLIENTES_GERENCIAR)
    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody CriarClienteRequest requisicao) {
        Documento documento = Documento.criar(requisicao.documento());
        Cliente cliente = clienteService.cadastrar(requisicao.nome(), documento, requisicao.telefoneWhatsapp());
        ClienteResponse resposta = ClienteResponse.de(cliente);
        return ResponseEntity.created(URI.create("/api/clientes/" + cliente.id())).body(resposta);
    }

    @PreAuthorize(RegraAcesso.CLIENTES_VER)
    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listarTodos().stream()
                .map(ClienteResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.CLIENTES_VER)
    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable UUID id) {
        return ClienteResponse.de(clienteService.buscarPorId(id));
    }

    @PreAuthorize(RegraAcesso.CLIENTES_GERENCIAR)
    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarClienteRequest requisicao) {
        Cliente cliente = clienteService.atualizarContato(id, requisicao.nome(), requisicao.telefoneWhatsapp());
        return ClienteResponse.de(cliente);
    }
}
