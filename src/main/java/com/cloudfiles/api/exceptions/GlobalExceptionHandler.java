
package com.cloudfiles.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trata erros de validação do upload
    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Map<String, String>> tratarUpload(
            UploadException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(Map.of("erro", exception.getMessage()));
    }

    // Trata arquivos que ultrapassam o limite de tamanho
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tratarTamanhoExcedido(
            MaxUploadSizeExceededException exception) {

        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("erro", "O arquivo excede o limite de 10 MB."));
    }

    // Trata requisições com formato diferente do esperado
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> tratarFormatoInvalido(
            HttpMediaTypeNotSupportedException exception) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "erro",
                        "Nenhum arquivo foi enviado. Utilize multipart/form-data."
                ));
    }
}
