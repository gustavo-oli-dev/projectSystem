package com.empresax.sistema.pdv.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.DadosPagamentoPresencial;
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
        List<ItemPedidoRequerido> itens = requisicao.itens().stream()
                .map(item -> new ItemPedidoRequerido(TipoItem.PRODUTO, item.produtoId(), item.quantidade()))
                .toList();
        PagamentoPresencialRequest pagamento = requisicao.pagamento();
        DadosPagamentoPresencial dados = new DadosPagamentoPresencial(
                pagamento.forma(), pagamento.valorRecebido(), pagamento.bandeira(), pagamento.codigoAutorizacao());
        return VendaBalcaoResponse.de(pdvService.vender(itens, requisicao.cpfNaNota(), dados, operador.getUsername()));
    }

    /** Pix com QR code na tela: separa os produtos do estoque e devolve o QR para o cliente pagar. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaPixResponse iniciarComPix(
            @Valid @RequestBody VendaPixRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        List<ItemPedidoRequerido> itens = requisicao.itens().stream()
                .map(item -> new ItemPedidoRequerido(TipoItem.PRODUTO, item.produtoId(), item.quantidade()))
                .toList();
        return VendaPixResponse.de(pdvService.iniciarVendaComPix(itens, requisicao.cpfNaNota(), operador.getUsername()));
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

    @PreAuthorize(RegraAcesso.PDV_CANCELAR)
    @PostMapping("/{pedidoId}/cancelar")
    public VendaBalcaoResponse cancelar(@PathVariable UUID pedidoId, @AuthenticationPrincipal UserDetails operador) {
        return VendaBalcaoResponse.de(pdvService.cancelar(pedidoId, operador.getUsername()));
    }
}
