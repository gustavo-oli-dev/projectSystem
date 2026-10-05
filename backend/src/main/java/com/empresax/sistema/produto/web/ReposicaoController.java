package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.estoque.ReposicaoService;
import com.empresax.sistema.produto.estoque.SugestaoReposicao;
import com.empresax.sistema.relatorios.vendas.ExportacaoCsv;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Sugestão de compra (produtos no estoque mínimo). Sem valores em dinheiro: basta ver o catálogo. */
@RestController
@RequestMapping("/api/estoque/reposicao")
@PreAuthorize(RegraAcesso.CATALOGO_VER)
public class ReposicaoController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final List<String> CABECALHO = List.of(
            "Produto", "Código de barras", "Unidade", "Estoque", "Estoque mínimo", "Vendidos em 30 dias", "Comprar");

    private final ReposicaoService reposicaoService;

    public ReposicaoController(ReposicaoService reposicaoService) {
        this.reposicaoService = reposicaoService;
    }

    @GetMapping
    public List<SugestaoReposicaoResponse> sugerir() {
        return reposicaoService.sugerir().stream().map(SugestaoReposicaoResponse::de).toList();
    }

    public record SugestaoReposicaoResponse(
            UUID produtoId, String nome, String codigoBarras, String unidadeMedida, int estoque, int estoqueMinimo,
            boolean minimoDefinido, int vendidosEm30Dias, int quantidadeSugerida
    ) {
        static SugestaoReposicaoResponse de(SugestaoReposicao sugestao) {
            return new SugestaoReposicaoResponse(
                    sugestao.produtoId(), sugestao.nome(), sugestao.codigoBarras(), sugestao.unidadeMedida(), sugestao.estoque(),
                    sugestao.estoqueMinimo(), sugestao.minimoDefinido(), sugestao.vendidosEm30Dias(), sugestao.quantidadeSugerida());
        }
    }

    /** CSV para Excel — a lista de compra para mandar ao fornecedor. */
    @GetMapping("/lista-de-compra.csv")
    public ResponseEntity<byte[]> exportar() {
        byte[] arquivo = ExportacaoCsv.montar(CABECALHO, reposicaoService.sugerir().stream().map(sugestao -> List.of(
                ExportacaoCsv.texto(sugestao.nome()),
                ExportacaoCsv.texto(sugestao.codigoBarras() == null ? "" : sugestao.codigoBarras()),
                ExportacaoCsv.texto(sugestao.unidadeMedida()),
                String.valueOf(sugestao.estoque()),
                String.valueOf(sugestao.estoqueMinimo()),
                String.valueOf(sugestao.vendidosEm30Dias()),
                String.valueOf(sugestao.quantidadeSugerida()))));
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("lista-de-compra.csv").build().toString())
                .body(arquivo);
    }
}
