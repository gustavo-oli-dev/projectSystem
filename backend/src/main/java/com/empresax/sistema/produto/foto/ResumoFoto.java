package com.empresax.sistema.produto.foto;

import java.util.UUID;

/** Dados da foto sem os bytes — para listar produtos sem carregar megabytes de imagem. */
public record ResumoFoto(UUID id, UUID produtoId, int ordem) {
}
