package com.empresax.sistema.acesso;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Garante que toda permissão do catálogo tem regra de @PreAuthorize e que nenhuma regra tem erro de digitação. */
class RegraAcessoTest {

    @Test
    void cadaPermissaoTemUmaRegraQueExigeExatamenteEla() throws IllegalAccessException {
        Map<String, String> regras = regrasDeclaradas();

        assertThat(regras.keySet())
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(Permissao.values()).map(Enum::name).toList());
        for (Map.Entry<String, String> regra : regras.entrySet()) {
            assertThat(regra.getValue()).isEqualTo("hasAuthority('" + regra.getKey() + "')");
        }
    }

    private static Map<String, String> regrasDeclaradas() throws IllegalAccessException {
        Map<String, String> regras = new HashMap<>();
        for (Field campo : RegraAcesso.class.getDeclaredFields()) {
            if (Modifier.isStatic(campo.getModifiers()) && campo.getType() == String.class) {
                regras.put(campo.getName(), (String) campo.get(null));
            }
        }
        return regras;
    }
}
