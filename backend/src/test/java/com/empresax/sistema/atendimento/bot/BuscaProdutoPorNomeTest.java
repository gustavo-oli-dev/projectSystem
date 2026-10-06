package com.empresax.sistema.atendimento.bot;

import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuscaProdutoPorNomeTest {

    private final Produto cafe = produto("Café Torrado 500g");
    private final Produto cafeComLeite = produto("Café com Leite");
    private final Produto arroz = produto("Arroz");
    private final List<Produto> catalogo = List.of(cafe, cafeComLeite, arroz);

    @Test
    void nomeIgualVenceMesmoSemAcentoOuMaiusculas() {
        assertThat(BuscaProdutoPorNome.procurar(catalogo, "ARROZ")).containsExactly(arroz);
        assertThat(BuscaProdutoPorNome.procurar(catalogo, "cafe torrado 500g")).containsExactly(cafe);
    }

    @Test
    void parteDoNomeTrazTodosOsParecidosParaPerguntarQual() {
        assertThat(BuscaProdutoPorNome.procurar(catalogo, "cafe")).containsExactly(cafe, cafeComLeite);
    }

    @Test
    void termoVazioOuSemParecidoNaoTrazNada() {
        assertThat(BuscaProdutoPorNome.procurar(catalogo, " ")).isEmpty();
        assertThat(BuscaProdutoPorNome.procurar(catalogo, "feijão")).isEmpty();
    }

    private static Produto produto(String nome) {
        return new Produto(nome, null, "09012100", "UN", new Dinheiro(new BigDecimal("10.00")));
    }
}
