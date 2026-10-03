package com.empresax.sistema.acesso.web;

import com.empresax.sistema.acesso.Permissao;

public record PermissaoResponse(String codigo, String area, String descricao) {

    public static PermissaoResponse de(Permissao permissao) {
        return new PermissaoResponse(permissao.name(), permissao.area(), permissao.descricao());
    }
}
