package com.empresax.sistema.cliente;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.documento.Cnpj;
import com.empresax.sistema.shared.documento.Cpf;
import com.empresax.sistema.shared.documento.Documento;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Serializa o Documento (Cpf ou Cnpj) numa única coluna, prefixada pelo tipo, para round-trip
 * fiel sem expor a decisão de tipo fora deste pacote.
 */
@Converter(autoApply = false)
class DocumentoConverter implements AttributeConverter<Documento, String> {

    private static final String SEPARADOR = ":";

    @Override
    public String convertToDatabaseColumn(Documento documento) {
        if (documento == null) {
            return null;
        }
        String tipo = documento instanceof Cpf ? "CPF" : "CNPJ";
        return tipo + SEPARADOR + documento.valor();
    }

    @Override
    public Documento convertToEntityAttribute(String coluna) {
        if (coluna == null) {
            return null;
        }
        String[] partes = coluna.split(SEPARADOR, 2);
        String tipo = partes[0];
        String valor = partes[1];
        return switch (tipo) {
            case "CPF" -> new Cpf(valor);
            case "CNPJ" -> new Cnpj(valor);
            default -> throw new DomainException("Tipo de documento desconhecido: " + tipo);
        };
    }
}
