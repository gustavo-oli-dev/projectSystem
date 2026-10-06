package com.empresax.sistema.pedido.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.cliente.ClienteService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.pedido.ItemPedidoRequerido;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.venda.CancelamentoVendaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final CancelamentoVendaService cancelamentoVendaService;
    private final ClienteService clienteService;

    public PedidoController(
            PedidoService pedidoService, CancelamentoVendaService cancelamentoVendaService, ClienteService clienteService
    ) {
        this.pedidoService = pedidoService;
        this.cancelamentoVendaService = cancelamentoVendaService;
        this.clienteService = clienteService;
    }

    @PreAuthorize(RegraAcesso.PEDIDOS_GERENCIAR)
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest requisicao) {
        List<ItemPedidoRequerido> itens = requisicao.itens().stream()
                .map(item -> new ItemPedidoRequerido(item.tipo(), item.referenciaId(), item.quantidade()))
                .toList();
        Pedido pedido = pedidoService.criar(requisicao.clienteId(), itens);
        PedidoResponse resposta = responder(pedido);
        return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.id())).body(resposta);
    }

    @PreAuthorize(RegraAcesso.PEDIDOS_VER)
    @GetMapping
    public List<PedidoResponse> listar() {
        List<Pedido> pedidos = pedidoService.listarTodos();
        Map<UUID, String> nomes = clienteService.nomesPorId(pedidos.stream()
                .flatMap(pedido -> pedido.clienteId().stream()).collect(Collectors.toSet()));
        return pedidos.stream()
                .map(pedido -> PedidoResponse.de(pedido, pedido.clienteId().map(nomes::get).orElse(null)))
                .toList();
    }

    private PedidoResponse responder(Pedido pedido) {
        String clienteNome = pedido.clienteId()
                .map(id -> clienteService.nomesPorId(List.of(id)).get(id))
                .orElse(null);
        return PedidoResponse.de(pedido, clienteNome);
    }

    @PreAuthorize(RegraAcesso.PEDIDOS_VER)
    @GetMapping("/{id}")
    public PedidoResponse buscarPorId(@PathVariable UUID id) {
        return responder(pedidoService.buscarPorId(id));
    }

    /** Confirmar a venda tira os produtos do estoque. */
    @PreAuthorize(RegraAcesso.PEDIDOS_GERENCIAR)
    @PostMapping("/{id}/confirmar")
    public PedidoResponse confirmar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails usuario) {
        return responder(pedidoService.confirmar(id, usuario.getUsername()));
    }

    @PreAuthorize(RegraAcesso.PEDIDOS_GERENCIAR)
    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails usuario) {
        return responder(cancelamentoVendaService.cancelar(id, usuario.getUsername()));
    }

    /** Devolve dinheiro: exige também gerenciar cobranças. */
    @PreAuthorize(RegraAcesso.PEDIDOS_GERENCIAR + " and " + RegraAcesso.COBRANCAS_GERENCIAR)
    @PostMapping("/{id}/reembolsar")
    public PedidoResponse reembolsar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails usuario) {
        return responder(cancelamentoVendaService.reembolsar(id, usuario.getUsername()));
    }
}
