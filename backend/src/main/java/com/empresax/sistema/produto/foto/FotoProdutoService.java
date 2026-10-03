package com.empresax.sistema.produto.foto;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FotoProdutoService {

    static final int FOTOS_POR_PRODUTO = 8;

    private final FotoProdutoRepository fotoRepository;
    private final ProdutoRepository produtoRepository;

    public FotoProdutoService(FotoProdutoRepository fotoRepository, ProdutoRepository produtoRepository) {
        this.fotoRepository = fotoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public ResumoFoto adicionar(UUID produtoId, byte[] conteudo) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new EntidadeNaoEncontradaException("Produto não encontrado: " + produtoId);
        }
        if (fotoRepository.countByProdutoId(produtoId) >= FOTOS_POR_PRODUTO) {
            throw new DomainException("Cada produto pode ter no máximo " + FOTOS_POR_PRODUTO + " fotos");
        }
        int proximaOrdem = fotoRepository.maiorOrdem(produtoId) + 1;
        FotoProduto foto = fotoRepository.save(new FotoProduto(produtoId, conteudo, proximaOrdem));
        return new ResumoFoto(foto.id(), produtoId, proximaOrdem);
    }

    @Transactional(readOnly = true)
    public FotoProduto buscar(UUID produtoId, UUID fotoId) {
        return fotoRepository.findByIdAndProdutoId(fotoId, produtoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Foto não encontrada: " + fotoId));
    }

    @Transactional
    public void remover(UUID produtoId, UUID fotoId) {
        fotoRepository.delete(buscar(produtoId, fotoId));
    }

    /** Uma consulta só para todos os produtos (sem N+1), já na ordem de exibição. */
    @Transactional(readOnly = true)
    public Map<UUID, List<UUID>> idsDasFotosPorProduto() {
        return fotoRepository.listarResumos().stream()
                .collect(Collectors.groupingBy(ResumoFoto::produtoId,
                        Collectors.mapping(ResumoFoto::id, Collectors.toList())));
    }
}
