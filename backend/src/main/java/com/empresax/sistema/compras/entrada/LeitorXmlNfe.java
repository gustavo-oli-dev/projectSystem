package com.empresax.sistema.compras.entrada;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.compras.entrada.NotaDoFornecedor.Emitente;
import com.empresax.sistema.compras.entrada.NotaDoFornecedor.ItemDaNota;
import com.empresax.sistema.compras.entrada.NotaDoFornecedor.Parcela;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lê o XML de uma NF-e (modelo 55, layout 4.00) — o arquivo que o fornecedor manda junto com a
 * mercadoria. Aceita tanto a NF-e pura quanto o "nfeProc" (NF-e + protocolo). O leitor é travado
 * contra XXE: sem DTD, sem entidades externas.
 */
@Component
public class LeitorXmlNfe {

    private static final String NAMESPACE_NFE = "http://www.portalfiscal.inf.br/nfe";
    private static final String PREFIXO_CHAVE = "NFe";
    private static final Pattern CHAVE_VALIDA = Pattern.compile("\\d{44}");
    private static final String SEM_GTIN = "SEM GTIN";
    private static final String NAO_EH_NFE = "Este arquivo não é o XML de uma NF-e (confira se é o .xml da nota, não o PDF/DANFE)";

    public NotaDoFornecedor ler(byte[] xml) {
        if (xml == null || xml.length == 0) {
            throw new DomainException("Envie o arquivo XML da nota");
        }
        Element infNFe = primeiro(documento(xml).getDocumentElement(), "infNFe")
                .orElseThrow(() -> new DomainException(NAO_EH_NFE));
        Element ide = exigir(infNFe, "ide");
        Element emit = exigir(infNFe, "emit");
        return new NotaDoFornecedor(
                chave(infNFe),
                texto(ide, "nNF"),
                texto(ide, "serie"),
                emissao(ide),
                new Emitente(
                        primeiroTexto(emit, "CNPJ").or(() -> primeiroTexto(emit, "CPF"))
                                .orElseThrow(() -> new DomainException("A nota não tem o CNPJ/CPF do fornecedor")),
                        texto(emit, "xNome"),
                        primeiroTexto(emit, "fone").orElse(null)),
                itens(infNFe),
                decimal(exigir(exigir(infNFe, "total"), "ICMSTot"), "vNF"),
                parcelas(infNFe));
    }

    /** DOM seguro: nada de DTD nem de entidades externas (ataque XXE). */
    private static Document documento(byte[] xml) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            DocumentBuilder leitor = fabrica.newDocumentBuilder();
            leitor.setErrorHandler(null);
            return leitor.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException | IOException falha) {
            throw new DomainException(NAO_EH_NFE);
        }
    }

    private static String chave(Element infNFe) {
        String id = infNFe.getAttribute("Id");
        String chave = id.startsWith(PREFIXO_CHAVE) ? id.substring(PREFIXO_CHAVE.length()) : id;
        if (!CHAVE_VALIDA.matcher(chave).matches()) {
            throw new DomainException("A chave de acesso da nota é inválida");
        }
        return chave;
    }

    /** dhEmi (layout atual, com fuso) ou dEmi (layouts antigos, só a data). */
    private static Instant emissao(Element ide) {
        try {
            Optional<String> dataHora = primeiroTexto(ide, "dhEmi");
            if (dataHora.isPresent()) {
                return OffsetDateTime.parse(dataHora.get()).toInstant();
            }
            return LocalDate.parse(texto(ide, "dEmi")).atStartOfDay(PeriodoRelatorio.FUSO_DA_LOJA).toInstant();
        } catch (DateTimeParseException falha) {
            throw new DomainException("Data de emissão da nota inválida");
        }
    }

    private static List<ItemDaNota> itens(Element infNFe) {
        NodeList detalhes = infNFe.getElementsByTagNameNS(NAMESPACE_NFE, "det");
        if (detalhes.getLength() == 0) {
            throw new DomainException("A nota não tem produtos");
        }
        List<ItemDaNota> itens = new ArrayList<>();
        for (int indice = 0; indice < detalhes.getLength(); indice++) {
            Element prod = exigir((Element) detalhes.item(indice), "prod");
            String codigoBarras = primeiroTexto(prod, "cEAN").filter(codigo -> !codigo.isBlank() && !SEM_GTIN.equals(codigo)).orElse(null);
            itens.add(new ItemDaNota(
                    indice + 1, texto(prod, "cProd"), codigoBarras, texto(prod, "xProd"), texto(prod, "uCom"),
                    decimal(prod, "qCom"), decimal(prod, "vUnCom")));
        }
        return itens;
    }

    /** cobr/dup (duplicatas). Nota sem cobrança = paga à vista ou combinada fora da nota. */
    private static List<Parcela> parcelas(Element infNFe) {
        NodeList duplicatas = infNFe.getElementsByTagNameNS(NAMESPACE_NFE, "dup");
        List<Parcela> parcelas = new ArrayList<>();
        for (int indice = 0; indice < duplicatas.getLength(); indice++) {
            Element dup = (Element) duplicatas.item(indice);
            try {
                parcelas.add(new Parcela(
                        primeiroTexto(dup, "nDup").orElse(String.valueOf(indice + 1)),
                        LocalDate.parse(texto(dup, "dVenc")),
                        decimal(dup, "vDup")));
            } catch (DateTimeParseException falha) {
                throw new DomainException("Vencimento de parcela inválido na nota");
            }
        }
        return parcelas;
    }

    private static BigDecimal decimal(Element pai, String tag) {
        try {
            return new BigDecimal(texto(pai, tag));
        } catch (NumberFormatException falha) {
            throw new DomainException("Valor inválido em <" + tag + "> na nota");
        }
    }

    private static String texto(Element pai, String tag) {
        return primeiroTexto(pai, tag).orElseThrow(() -> new DomainException("A nota não tem o campo <" + tag + ">"));
    }

    private static Optional<String> primeiroTexto(Element pai, String tag) {
        return primeiro(pai, tag).map(Node::getTextContent).map(String::trim);
    }

    private static Element exigir(Element pai, String tag) {
        return primeiro(pai, tag).orElseThrow(() -> new DomainException("A nota não tem o grupo <" + tag + ">"));
    }

    private static Optional<Element> primeiro(Element pai, String tag) {
        NodeList encontrados = pai.getElementsByTagNameNS(NAMESPACE_NFE, tag);
        return encontrados.getLength() == 0 ? Optional.empty() : Optional.of((Element) encontrados.item(0));
    }
}
