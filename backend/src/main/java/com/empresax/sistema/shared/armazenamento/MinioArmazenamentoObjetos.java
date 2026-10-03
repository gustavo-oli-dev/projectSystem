package com.empresax.sistema.shared.armazenamento;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Adapter real para MinIO.
 *
 * NOTA (2026-10-02): a MinIO Inc. restringiu o acesso às imagens Docker pré-construídas
 * (docker.io/minio/minio e quay.io/minio/minio exigem login/assinatura) — por isso o serviço
 * `minio` pode não estar disponível em todo ambiente local. A preparação do bucket, abaixo, é
 * deliberadamente não-fatal: anexo de WhatsApp é uma funcionalidade periférica, e todo o resto do
 * sistema (cadastros, pedidos, fiscal, financeiro) não depende do MinIO estar de pé. Falhar o
 * boot inteiro da aplicação por isso seria acoplar a disponibilidade de tudo a uma integração
 * secundária — ver PENDENCIAS.md para achar um object storage que funcione (self-host via build
 * próprio do MinIO, ou outro S3-compatível como Garage/SeaweedFS).
 */
@Service
public class MinioArmazenamentoObjetos implements ArmazenamentoObjetos {

    private static final Logger LOG = LoggerFactory.getLogger(MinioArmazenamentoObjetos.class);

    private final MinioClient cliente;
    private final String bucket;

    public MinioArmazenamentoObjetos(
            @Value("${armazenamento.minio.endpoint}") String endpoint,
            @Value("${armazenamento.minio.access-key}") String accessKey,
            @Value("${armazenamento.minio.secret-key}") String secretKey,
            @Value("${armazenamento.minio.bucket}") String bucket
    ) {
        this.cliente = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
    }

    @PostConstruct
    void garantirBucket() {
        try {
            boolean existe = cliente.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                cliente.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception excecao) {
            // Não-fatal de propósito: ver nota da classe. salvar()/recuperar() continuam
            // lançando ArmazenamentoObjetosException normalmente quando alguém de fato usar.
            LOG.warn("MinIO indisponível ao iniciar — anexos não vão funcionar até o bucket '{}' existir", bucket, excecao);
        }
    }

    @Override
    public String salvar(byte[] conteudo, String nomeArquivo, String mimeType) {
        String chaveObjeto = UUID.randomUUID() + "-" + nomeArquivo;
        try (InputStream entrada = new ByteArrayInputStream(conteudo)) {
            cliente.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chaveObjeto)
                    .stream(entrada, conteudo.length, -1)
                    .contentType(mimeType)
                    .build());
            return chaveObjeto;
        } catch (Exception excecao) {
            throw new ArmazenamentoObjetosException("Falha ao salvar anexo no MinIO", excecao);
        }
    }

    @Override
    public byte[] recuperar(String chaveObjeto) {
        try (InputStream objeto = cliente.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(chaveObjeto)
                .build())) {
            return objeto.readAllBytes();
        } catch (Exception excecao) {
            throw new ArmazenamentoObjetosException("Falha ao recuperar anexo do MinIO: " + chaveObjeto, excecao);
        }
    }
}
