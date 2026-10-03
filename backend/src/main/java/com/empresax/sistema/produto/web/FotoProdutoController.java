package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.foto.FotoProduto;
import com.empresax.sistema.produto.foto.FotoProdutoService;
import com.empresax.sistema.produto.foto.ResumoFoto;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos/{produtoId}/fotos")
public class FotoProdutoController {

    private static final Duration CACHE_FOTO = Duration.ofDays(30);

    private final FotoProdutoService fotoProdutoService;

    public FotoProdutoController(FotoProdutoService fotoProdutoService) {
        this.fotoProdutoService = fotoProdutoService;
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse.FotoResponse enviar(@PathVariable UUID produtoId, @RequestParam("arquivo") MultipartFile arquivo) {
        ResumoFoto foto = fotoProdutoService.adicionar(produtoId, lerBytes(arquivo));
        return new ProdutoResponse.FotoResponse(foto.id(), ProdutoResponse.urlDaFoto(produtoId, foto.id()));
    }

    /**
     * Público (justificado, ver SecurityConfig): foto de produto é vitrine — aparece no site de vendas
     * e em tags <img>, que não enviam o token. A foto nunca muda (trocar = enviar outra), daí o cache longo.
     * O tipo vem da assinatura dos bytes, e o Spring Security já envia "X-Content-Type-Options: nosniff".
     */
    @GetMapping("/{fotoId}")
    public ResponseEntity<byte[]> exibir(@PathVariable UUID produtoId, @PathVariable UUID fotoId) {
        FotoProduto foto = fotoProdutoService.buscar(produtoId, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo().tipoMime()))
                .cacheControl(CacheControl.maxAge(CACHE_FOTO).cachePublic().immutable())
                .body(foto.conteudo());
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @DeleteMapping("/{fotoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID produtoId, @PathVariable UUID fotoId) {
        fotoProdutoService.remover(produtoId, fotoId);
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException falha) {
            throw new DomainException("Não foi possível ler o arquivo enviado");
        }
    }
}
