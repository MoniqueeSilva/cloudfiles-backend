package com.cloudfiles.api.controllers;

import com.cloudfiles.api.dtos.ArquivoResponseDTO;
import com.cloudfiles.api.exceptions.UploadException;
import com.cloudfiles.api.services.S3Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/arquivos")
public class ArquivoController {

    private final S3Service s3Service;

    @Value("${app.upload.max-file-size}")
    private long tamanhoMaximo;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public ArquivoController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    // Recebe o arquivo e valida a requisição antes de enviar ao S3
    @PostMapping
    public ResponseEntity<ArquivoResponseDTO> enviarArquivo(
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        if (contentType == null ||
                !contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            throw new UploadException(
                    "Envie o arquivo utilizando o formato multipart/form-data.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (arquivo == null) {
            throw new UploadException(
                    "Nenhum arquivo foi enviado.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (arquivo.isEmpty()) {
            throw new UploadException(
                    "O arquivo está vazio.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (arquivo.getOriginalFilename() == null ||
                arquivo.getOriginalFilename().isBlank()) {
            throw new UploadException(
                    "O nome do arquivo é inválido.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (arquivo.getSize() > tamanhoMaximo) {
            throw new UploadException(
                    "O arquivo excede o limite de 10 MB.",
                    HttpStatus.CONTENT_TOO_LARGE
            );
        }

        if (arquivo.getContentType() == null ||
                !TIPOS_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new UploadException(
                    "Tipo de arquivo não permitido.",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE
            );
        }

        ArquivoResponseDTO resposta = s3Service.upload(arquivo);

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    // Retorna a lista de arquivos armazenados no S3
    @GetMapping
    public ResponseEntity<List<ArquivoResponseDTO>> listarArquivos() {
        return ResponseEntity.ok(s3Service.listarArquivos());
    }
}
