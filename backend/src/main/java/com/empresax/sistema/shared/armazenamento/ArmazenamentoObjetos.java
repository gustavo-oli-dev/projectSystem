package com.empresax.sistema.shared.armazenamento;

/**
 * Porta para o object storage de anexos (MinIO, decisão D8 em DECISOES.md). O Postgres guarda só
 * o metadado e a chave do objeto; o conteúdo binário vive aqui, com sua própria política de
 * backup.
 */
public interface ArmazenamentoObjetos {

    String salvar(byte[] conteudo, String nomeArquivo, String mimeType);

    byte[] recuperar(String chaveObjeto);
}
