package com.empresax.sistema.pdv.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.DadosVendaBalcao;
import com.empresax.sistema.pdv.PdvService;
import com.empresax.sistema.pdv.VendaBalcao;
import com.empresax.sistema.pedido.ItemPedidoRequerido;
import com.empresax.sistema.pedido.TipoItem;
import com.empresax.sistema.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Caixa (venda presencial). Cancelar exige permissão própria: estorno é ponto sensível a fraude. */
@RestController
@RequestMapping("/api/pdv/vendas")
public class PdvController {

    private final PdvService pdvService;
    private final UsuarioService usuarioService;

    public PdvController(PdvService pdvService, UsuarioService usuarioService) {
        this.pdvService = pdvService;
        this.usuarioService = usuarioService;
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaBalcaoResponse vender(
            @Valid @RequestBody VendaBalcaoRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        DadosVendaBalcao venda = dadosDaVenda(requisicao.itens(), requisicao.cpfNaNota(), requisicao.clienteId(), requisicao.desconto());
        return responder(pdvService.vender(venda, PagamentoPresencialRequest.partes(requisicao.partes()),
                requisicao.pagamento().paraDados(), operador.getUsername()));
    }

    /** Maquininha integrada: separa o estoque e manda o valor para a maquininha. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/maquininha")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaBalcaoResponse iniciarNaMaquininha(
            @Valid @RequestBody VendaMaquininhaRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        DadosVendaBalcao venda = dadosDaVenda(requisicao.itens(), requisicao.cpfNaNota(), requisicao.clienteId(), requisicao.desconto());
        return responder(pdvService.iniciarNaMaquininha(
                venda, PagamentoPresencialRequest.partes(requisicao.partes()), requisicao.forma(), operador.getUsername()));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @GetMapping("/{pedidoId}/maquininha")
    public VendaBalcaoResponse acompanharMaquininha(@PathVariable UUID pedidoId) {
        return responder(pdvService.acompanharMaquininha(pedidoId));
    }

    /** Cartão recusado: manda o valor de novo (outro cartão). */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/{pedidoId}/maquininha/tentar-de-novo")
    public VendaBalcaoResponse tentarDeNovoNaMaquininha(@PathVariable UUID pedidoId) {
        return responder(pdvService.tentarDeNovoNaMaquininha(pedidoId));
    }

    /** Pix com QR code na tela: separa os produtos do estoque e devolve o QR para o cliente pagar. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaPixResponse iniciarComPix(
            @Valid @RequestBody VendaPixRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        DadosVendaBalcao venda = dadosDaVenda(requisicao.itens(), requisicao.cpfNaNota(), requisicao.clienteId(), requisicao.desconto());
        return VendaPixResponse.de(pdvService.iniciarVendaComPix(venda, operador.getUsername()));
    }

    /** O caixa consulta a cada poucos segundos até o Pix cair (ou o operador cancelar). */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @GetMapping("/{pedidoId}/pix")
    public VendaBalcaoResponse acompanharPix(@PathVariable UUID pedidoId) {
        return responder(pdvService.acompanharPix(pedidoId));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER + " or " + RegraAcesso.PDV_CANCELAR)
    @GetMapping
    public List<VendaBalcaoResponse> ultimas() {
        List<VendaBalcao> vendas = pdvService.ultimasVendas();
        Map<String, String> nomes = usuarioService.nomesPorEmail(
                vendas.stream().flatMap(venda -> venda.pessoas().stream()).collect(Collectors.toSet()));
        return vendas.stream().map(venda -> VendaBalcaoResponse.de(venda, nomes)).toList();
    }

    /** Como a venda foi paga — usado no detalhe do pedido. */
    @PreAuthorize(RegraAcesso.PDV_VENDER + " or " + RegraAcesso.PDV_CANCELAR + " or " + RegraAcesso.PEDIDOS_VER)
    @GetMapping("/{pedidoId}")
    public VendaBalcaoResponse buscar(@PathVariable UUID pedidoId) {
        return responder(pdvService.buscarVenda(pedidoId));
    }

    @PreAuthorize(RegraAcesso.PDV_CANCELAR)
    @PostMapping("/{pedidoId}/cancelar")
    public VendaBalcaoResponse cancelar(@PathVariable UUID pedidoId, @AuthenticationPrincipal UserDetails operador) {
        return responder(pdvService.cancelar(pedidoId, operador.getUsername()));
    }

    private static DadosVendaBalcao dadosDaVenda(
            List<ItemVendaBalcaoRequest> itens, String cpfNaNota, UUID clienteId, DescontoVendaRequest desconto
    ) {
        List<ItemPedidoRequerido> requeridos = itens.stream()
                .map(item -> new ItemPedidoRequerido(TipoItem.PRODUTO, item.produtoId(), item.quantidade(), item.embalagemId()))
                .toList();
        DadosVendaBalcao.Desconto descontoAutorizado = desconto == null
                ? null : new DadosVendaBalcao.Desconto(desconto.valor(), desconto.tokenAutorizacao());
        return new DadosVendaBalcao(requeridos, cpfNaNota, clienteId, descontoAutorizado);
    }

    /** Uma venda só: resolve o nome de quem vendeu (a venda guarda o e-mail). */
    private VendaBalcaoResponse responder(VendaBalcao venda) {
        return VendaBalcaoResponse.de(venda, usuarioService.nomesPorEmail(venda.pessoas()));
    }
}
