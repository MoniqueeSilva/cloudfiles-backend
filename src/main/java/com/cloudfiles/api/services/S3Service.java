package com.cloudfiles.api.services;

import com.cloudfiles.api.dtos.ArquivoResponseDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.bucketName}")
    private String bucketName;

    @Value("${cloudfront.domain}")
    private String cloudfrontDomain;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public ArquivoResponseDTO upload(MultipartFile arquivo) {
        try {
            String nomeOriginal = arquivo.getOriginalFilename();

            String extensao = "";
            if (nomeOriginal != null && nomeOriginal.contains(".")) {
                extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
            }

            String key = UUID.randomUUID() + extensao;

            String nomeCodificado = Base64.getEncoder()
                    .encodeToString(nomeOriginal.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(
                            arquivo.getContentType() != null
                                    ? arquivo.getContentType()
                                    : "application/octet-stream")
                    .metadata(Map.of("nome-original", nomeCodificado))
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));

            String url = gerarUrlCloudFront(key);

            return new ArquivoResponseDTO(
                    key,
                    nomeOriginal,
                    url,
                    arquivo.getSize(),
                    arquivo.getContentType());

        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar o arquivo", e);
        }
    }

    public List<ArquivoResponseDTO> listarArquivos() {
        List<ArquivoResponseDTO> arquivos = new ArrayList<>();

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response response;

        do {
            response = s3Client.listObjectsV2(request);

            for (S3Object objeto : response.contents()) {
                HeadObjectResponse detalhes = s3Client.headObject(
                        HeadObjectRequest.builder()
                                .bucket(bucketName)
                                .key(objeto.key())
                                .build());

                String nomeOriginal = detalhes.metadata()
                        .getOrDefault("nome-original", objeto.key());

                try {
                    nomeOriginal = new String(
                            Base64.getDecoder().decode(nomeOriginal),
                            java.nio.charset.StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    // Mantém o nome armazenado caso não esteja codificado.
                }

                String url = gerarUrlCloudFront(objeto.key());

                arquivos.add(new ArquivoResponseDTO(
                        objeto.key(),
                        nomeOriginal,
                        url,
                        objeto.size(),
                        detalhes.contentType()));
            }

            request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();

        } while (response.isTruncated());

        return arquivos;
    }

    // NOVO: Buscar um arquivo específico pelo ID
    public ArquivoResponseDTO buscarPorId(String id) {
        try {
            HeadObjectResponse detalhes = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucketName).key(id).build());

            String nomeOriginal = detalhes.metadata().getOrDefault("nome-original", id);
            try {
                nomeOriginal = new String(Base64.getDecoder().decode(nomeOriginal), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) { /* Mantém */ }

            String url = gerarUrlCloudFront(id);
            return new ArquivoResponseDTO(id, nomeOriginal, url, detalhes.contentLength(), detalhes.contentType());

        } catch (NoSuchKeyException e) {
            return null; // Retorna null para o Controller tratar como 404
        }
    }

    // NOVO: Renomear o arquivo (atualiza o metadado)
    public ArquivoResponseDTO renomear(String id, String novoNome) {
        HeadObjectResponse detalhes = s3Client.headObject(
                HeadObjectRequest.builder().bucket(bucketName).key(id).build());

        String novoNomeCodificado = Base64.getEncoder()
                .encodeToString(novoNome.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(id)
                .destinationBucket(bucketName)
                .destinationKey(id)
                .metadata(Map.of("nome-original", novoNomeCodificado))
                .metadataDirective(MetadataDirective.REPLACE)
                .contentType(detalhes.contentType())
                .build();

        s3Client.copyObject(copyRequest);

        String url = gerarUrlCloudFront(id);
        return new ArquivoResponseDTO(id, novoNome, url, detalhes.contentLength(), detalhes.contentType());
    }

    // NOVO: Deletar um arquivo
    public void deletar(String id) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(id).build());
    }

    private String gerarUrlCloudFront(String key) {
        return String.format("https://%s/%s", cloudfrontDomain, key);
    }
}