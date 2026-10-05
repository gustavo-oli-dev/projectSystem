package com.empresax.sistema.compras.contas.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.compras.contas.ContaAPagar;
import com.empresax.sistema.compras.contas.ContaAPagarService;
import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.contato.ContatoService;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Contas a pagar da empresa: parcelas das notas de fornecedor e despesas lançadas à mão. */
@RestController
@RequestMapping("/api/contas-a-pagar")
@PreAuthorize(RegraAcesso.CONTAS_PAGAR_GERENCIAR)
public class ContaAPagarController {

    private final ContaAPagarService contaService;
    private final ContatoService contatoService;

    public ContaAPagarController(ContaAPagarService contaService, ContatoService contatoService) {
        this.contaService = contaService;
        this.contatoService = contatoService;
    }

    @GetMapping
    public List<ContaAPagarResponse> listar() {
        Map<UUID, String> nomes = nomesDosContatos();
        LocalDate hoje = hoje();
        return contaService.listar().stream().map(conta -> ContaAPagarResponse.de(conta, nomes, hoje)).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaAPagarResponse lancar(@Valid @RequestBody ContaAPagarRequest requisicao, @AuthenticationPrincipal UserDetails usuario) {
        ContaAPagar conta = contaService.lancar(requisicao.contatoId(), requisicao.descricao(), new Dinheiro(requisicao.valor()),
                requisicao.vencimento(), usuario.getUsername());
        return ContaAPagarResponse.de(conta, nomesDosContatos(), hoje());
    }

    @PostMapping("/{id}/pagar")
    public ContaAPagarResponse pagar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails usuario) {
        return ContaAPagarResponse.de(contaService.pagar(id, usuario.getUsername()), nomesDosContatos(), hoje());
    }

    @PostMapping("/{id}/cancelar")
    public ContaAPagarResponse cancelar(@PathVariable UUID id) {
        return ContaAPagarResponse.de(contaService.cancelar(id), nomesDosContatos(), hoje());
    }

    private Map<UUID, String> nomesDosContatos() {
        return contatoService.listar().stream().collect(Collectors.toMap(Contato::id, Contato::nome));
    }

    /** "Vencida" é pelo dia da loja, não do servidor. */
    private static LocalDate hoje() {
        return LocalDate.now(PeriodoRelatorio.FUSO_DA_LOJA);
    }
}
