package com.empresax.sistema.acesso.web;

import com.empresax.sistema.acesso.Permissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CargoRequest(
        @NotBlank(message = "Nome do perfil de acesso é obrigatório")
        @Size(max = 80, message = "Nome do perfil de acesso deve ter no máximo 80 caracteres") String nome,
        @Size(max = 300, message = "Descrição deve ter no máximo 300 caracteres") String descricao,
        @NotEmpty(message = "Escolha ao menos uma permissão") Set<Permissao> permissoes
) {
}
