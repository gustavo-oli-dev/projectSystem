package com.empresax.sistema.compras.entrada.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.contato.ContatoService;
import com.empresax.sistema.compras.entrada.EntradaPorNotaService;
import com.empresax.sistema.compras.entrada.NotaEntrada;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Entrada de mercadoria pelo XML da nota do fornecedor: pré-visualizar e confirmar. */
@RestController
@RequestMapping("/api/compras/notas")
@PreAuthorize(RegraAcesso.ESTOQUE_GERENCIAR)
public class EntradaPorNotaController {

    private final EntradaPorNotaService entradaService;
    private final ContatoService contatoService;

    public EntradaPorNotaController(EntradaPorNotaService entradaService, ContatoService contatoService) {
        this.entradaService = entradaService;
        this.contatoService = contatoService;
    }

    /** Só lê o XML e mostra o que veio — nada é gravado. */
    @PostMapping("/pre-visualizar")
    public PreVisualizacaoNotaResponse preVisualizar(@RequestPart("arquivo") MultipartFile arquivo) {
        return PreVisualizacaoNotaResponse.de(entradaService.preVisualizar(lerBytes(arquivo)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotaEntradaResponse confirmar(
            @RequestPart("arquivo") MultipartFile arquivo,
            @Valid @RequestPart("dados") ConfirmacaoNotaRequest dados,
            @AuthenticationPrincipal UserDetails usuario
    ) {
        Map<Integer, UUID> produtoPorItem = dados.itens().stream().collect(Collectors.toMap(
                ConfirmacaoNotaRequest.ItemLigado::ordem, ConfirmacaoNotaRequest.ItemLigado::produtoId,
                (primeiro, repetido) -> {
                    throw new DomainException("O mesmo item da nota foi ligado duas vezes");
                }));
        NotaEntrada nota = entradaService.confirmar(lerBytes(arquivo), produtoPorItem, dados.atualizarCusto(), usuario.getUsername());
        return NotaEntradaResponse.de(nota, nomesDosContatos());
    }

    @GetMapping
    public List<NotaEntradaResponse> ultimas() {
        Map<UUID, String> nomes = nomesDosContatos();
        return entradaService.ultimasNotas().stream().map(nota -> NotaEntradaResponse.de(nota, nomes)).toList();
    }

    private Map<UUID, String> nomesDosContatos() {
        return contatoService.listar().stream().collect(Collectors.toMap(Contato::id, Contato::nome));
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException falha) {
            throw new DomainException("Não foi possível ler o arquivo enviado");
        }
    }
}
