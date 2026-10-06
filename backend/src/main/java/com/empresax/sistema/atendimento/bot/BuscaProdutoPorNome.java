package com.empresax.sistema.atendimento.bot;

import com.empresax.sistema.produto.Produto;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Acha o produto que o cliente pediu pelo nome, do jeito que ele escreve ("cafe", "Café 500g"):
 * sem diferenciar maiúsculas nem acentos. Nome igual vence; senão, os que contêm o termo (D43).
 */
final class BuscaProdutoPorNome {

    private BuscaProdutoPorNome() {
    }

    static List<Produto> procurar(List<Produto> produtos, String termo) {
        String procurado = normalizar(termo);
        if (procurado.isEmpty()) {
            return List.of();
        }
        List<Produto> iguais = produtos.stream().filter(produto -> normalizar(produto.nome()).equals(procurado)).toList();
        if (!iguais.isEmpty()) {
            return iguais;
        }
        return produtos.stream().filter(produto -> normalizar(produto.nome()).contains(procurado)).toList();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
