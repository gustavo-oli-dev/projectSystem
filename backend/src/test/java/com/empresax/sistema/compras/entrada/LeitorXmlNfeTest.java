package com.empresax.sistema.compras.entrada;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeitorXmlNfeTest {

    /** NF-e fictícia, na estrutura do layout 4.00, dentro de um nfeProc (como o fornecedor manda). */
    static final String NOTA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
              <NFe>
                <infNFe Id="NFe23261011222333000181550010000012341000012345" versao="4.00">
                  <ide><serie>1</serie><nNF>1234</nNF><dhEmi>2026-10-05T09:30:00-03:00</dhEmi></ide>
                  <emit>
                    <CNPJ>11222333000181</CNPJ><xNome>Distribuidora Exemplo Ltda</xNome>
                    <enderEmit><fone>8532221111</fone></enderEmit>
                  </emit>
                  <det nItem="1"><prod>
                    <cProd>A1</cProd><cEAN>7891000000014</cEAN><xProd>Arroz Tipo 1 5kg</xProd>
                    <uCom>UN</uCom><qCom>12.0000</qCom><vUnCom>18.5000</vUnCom>
                  </prod></det>
                  <det nItem="2"><prod>
                    <cProd>B2</cProd><cEAN>SEM GTIN</cEAN><xProd>Queijo a granel</xProd>
                    <uCom>KG</uCom><qCom>2.5000</qCom><vUnCom>40.0000</vUnCom>
                  </prod></det>
                  <total><ICMSTot><vNF>322.00</vNF></ICMSTot></total>
                  <cobr>
                    <dup><nDup>001</nDup><dVenc>2026-11-04</dVenc><vDup>161.00</vDup></dup>
                    <dup><nDup>002</nDup><dVenc>2026-12-04</dVenc><vDup>161.00</vDup></dup>
                  </cobr>
                </infNFe>
              </NFe>
            </nfeProc>
            """;

    private final LeitorXmlNfe leitor = new LeitorXmlNfe();

    @Test
    void leFornecedorItensTotalEParcelasDaNota() {
        NotaDoFornecedor nota = leitor.ler(NOTA.getBytes(StandardCharsets.UTF_8));

        assertThat(nota.chaveAcesso()).isEqualTo("23261011222333000181550010000012341000012345");
        assertThat(nota.numero()).isEqualTo("1234");
        assertThat(nota.emitente().documento()).isEqualTo("11222333000181");
        assertThat(nota.emitente().nome()).isEqualTo("Distribuidora Exemplo Ltda");
        assertThat(nota.emitente().telefone()).isEqualTo("8532221111");
        assertThat(nota.valorTotal()).isEqualByComparingTo("322.00");
        assertThat(nota.itens()).hasSize(2);
        assertThat(nota.itens().get(0).codigoBarras()).isEqualTo("7891000000014");
        assertThat(nota.itens().get(0).quantidade()).isEqualByComparingTo("12");
        assertThat(nota.itens().get(0).quantidadeInteira()).isTrue();
        assertThat(nota.parcelas()).extracting(NotaDoFornecedor.Parcela::vencimento)
                .containsExactly(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 12, 4));
    }

    @Test
    void semGtinViraSemCodigoDeBarrasEQuantidadeFracionadaEhApontada() {
        NotaDoFornecedor.ItemDaNota queijo = leitor.ler(NOTA.getBytes(StandardCharsets.UTF_8)).itens().get(1);

        assertThat(queijo.codigoBarras()).isNull();
        assertThat(queijo.quantidade()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(queijo.quantidadeInteira()).isFalse();
    }

    @Test
    void arquivoQueNaoEhNfeEhRecusado() {
        assertThatThrownBy(() -> leitor.ler("<html><body>DANFE</body></html>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("não é o XML de uma NF-e");
        assertThatThrownBy(() -> leitor.ler("isto não é xml".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void xmlComEntidadeExternaEhRecusadoSemLerArquivoDoServidor() {
        String ataque = """
                <?xml version="1.0"?>
                <!DOCTYPE nfeProc [<!ENTITY segredo SYSTEM "file:///etc/passwd">]>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe"><NFe><infNFe Id="NFe&segredo;"/></NFe></nfeProc>
                """;

        assertThatThrownBy(() -> leitor.ler(ataque.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("não é o XML de uma NF-e");
    }
}
