package com.empresax.sistema.pdv.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.DadosPagamentoPresencial;
import com.empresax.sistema.pdv.DadosVendaBalcao;
import com.empresax.sistema.pdv.PdvService;
import com.empresax.sistema.pedido.ItemPedidoRequerido;
import com.empresax.sistema.pedido.TipoItem;
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
import java.util.UUID;

/** Caixa (venda presencial). Cancelar exige permissão própria: estorno é ponto sensível a fraude. */
@RestController
@RequestMapping("/api/pdv/vendas")
public class PdvController {

    private final PdvService pdvService;

    public PdvController(PdvService pdvService) {
        this.pdvService = pdvService;
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaBalcaoResponse vender(
            @Valid @RequestBody VendaBalcaoRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        DadosVendaBalcao venda = dadosDaVenda(requisicao.itens(), requisicao.cpfNaNota(), requisicao.clienteId());
        PagamentoPresencialRequest pagamento = requisicao.pagamento();
        DadosPagamentoPresencial dados = new DadosPagamentoPresencial(
                pagamento.forma(), pagamento.valorRecebido(), pagamento.bandeira(), pagamento.codigoAutorizacao());
        return VendaBalcaoResponse.de(pdvService.vender(venda, dados, operador.getUsername()));
    }

    /** Maquininha integrada: separa o estoque e manda o valor para a maquininha. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/maquininha")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaBalcaoResponse iniciarNaMaquininha(
            @Valid @RequestBody VendaMaquininhaRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        DadosVendaBalcao venda = dadosDaVenda(requisicao.itens(), requisicao.cpfNaNota(), requisicao.clienteId());
        return VendaBalcaoResponse.de(pdvService.iniciarNaMaquininha(venda, requisicao.forma(), operador.getUsername()));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @GetMapping("/{pedidoId}/maquininha")
    public VendaBalcaoResponse acompanharMaquininha(@PathVariable UUID pedidoId) {
        return VendaBalcaoResponse.de(pdvService.acompanharMaquininha(pedidoId));
    }

    /** Cartão recusado: manda o valor de novo (outro cartão). */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/{pedidoId}/maquininha/tentar-de-novo")
    public VendaBalcaoResponse tentarDeNovoNaMaquininha(@PathVariable UUID pedidoId) {
        return VendaBalcaoResponse.de(pdvService.tentarDeNovoNaMaquininha(pedidoId));
    }

    /** Pix com QR code na tela: separa os produtos do estoque e devolve o QR para o cliente pagar. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaPixResponse iniciarComPix(
            @Valid @RequestBody VendaPixRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        DadosVendaBalcao venda = dadosDaVenda(requisicao.itens(), requisicao.cpfNaNota(), requisicao.clienteId());
        return VendaPixResponse.de(pdvService.iniciarVendaComPix(venda, operador.getUsername()));
    }

    /** O caixa consulta a cada poucos segundos até o Pix cair (ou o operador cancelar). */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @GetMapping("/{pedidoId}/pix")
    public VendaBalcaoResponse acompanharPix(@PathVariable UUID pedidoId) {
        return VendaBalcaoResponse.de(pdvService.acompanharPix(pedidoId));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER + " or " + RegraAcesso.PDV_CANCELAR)
    @GetMapping
    public List<VendaBalcaoResponse> ultimas() {
        return pdvService.ultimasVendas().stream().map(VendaBalcaoResponse::de).toList();
    }

    /** Como a venda foi paga — usado no detalhe do pedido. */
    @PreAuthorize(RegraAcesso.PDV_VENDER + " or " + RegraAcesso.PDV_CANCELAR + " or " + RegraAcesso.PEDIDOS_VER)
    @GetMapping("/{pedidoId}")
    public VendaBalcaoResponse buscar(@PathVariable UUID pedidoId) {
        return VendaBalcaoResponse.de(pdvService.buscarVenda(pedidoId));
    }

    @PreAuthorize(RegraAcesso.PDV_CANCELAR)
    @PostMapping("/{pedidoId}/cancelar")
    public VendaBalcaoResponse cancelar(@PathVariable UUID pedidoId, @AuthenticationPrincipal UserDetails operador) {
        return VendaBalcaoResponse.de(pdvService.cancelar(pedidoId, operador.getUsername()));
    }

    private static DadosVendaBalcao dadosDaVenda(List<ItemVendaBalcaoRequest> itens, String cpfNaNota, UUID clienteId) {
        List<ItemPedidoRequerido> requeridos = itens.stream()
                .map(item -> new ItemPedidoRequerido(TipoItem.PRODUTO, item.produtoId(), item.quantidade()))
                .toList();
        return new DadosVendaBalcao(requeridos, cpfNaNota, clienteId);
    }
}
