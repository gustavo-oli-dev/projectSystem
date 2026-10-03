package com.empresax.sistema.relatorios.vendas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportacaoCsvTest {

    @Test
    void textoQueComecaComoFormulaNaoViraFormulaNoExcel() {
        assertThat(ExportacaoCsv.texto("=HYPERLINK(\"http://x\")")).startsWith("'=");
        assertThat(ExportacaoCsv.texto("+5511999")).startsWith("'+");
        assertThat(ExportacaoCsv.texto("@soma")).startsWith("'@");
    }

    @Test
    void textoComumFicaComoEsta() {
        assertThat(ExportacaoCsv.texto("Caneca personalizada")).isEqualTo("Caneca personalizada");
    }
}
