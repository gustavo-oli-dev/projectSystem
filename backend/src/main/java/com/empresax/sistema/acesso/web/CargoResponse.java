package com.empresax.sistema.acesso.web;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.Permissao;

import java.util.List;
import java.util.UUID;

public record CargoResponse(UUID id, String nome, String descricao, List<String> permissoes) {

    public static CargoResponse de(Cargo cargo) {
        return new CargoResponse(
                cargo.id(),
                cargo.nome(),
                cargo.descricao(),
                cargo.permissoes().stream().map(Permissao::name).sorted().toList());
    }
}
