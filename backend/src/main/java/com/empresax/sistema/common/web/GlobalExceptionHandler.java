package com.empresax.sistema.common.web;

import com.empresax.sistema.common.domain.AcessoNegadoException;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.common.domain.IntegracaoExternaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;

/**
 * Tratamento de erro centralizado: nenhum controller trata exceção individualmente, e nenhuma
 * resposta ao cliente vaza stack trace ou detalhe interno da aplicação.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> tratarDomainException(DomainException excecao) {
        return construirResposta(HttpStatus.UNPROCESSABLE_ENTITY, excecao.getMessage());
    }

    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public ResponseEntity<ApiErrorResponse> tratarEntidadeNaoEncontrada(EntidadeNaoEncontradaException excecao) {
        return construirResposta(HttpStatus.NOT_FOUND, excecao.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> tratarAutenticacao(AuthenticationException excecao) {
        return construirResposta(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> tratarValidacao(MethodArgumentNotValidException excecao) {
        String mensagens = excecao.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .reduce((primeira, segunda) -> primeira + "; " + segunda)
                .orElse("Dados inválidos");
        return construirResposta(HttpStatus.BAD_REQUEST, mensagens);
    }

    @ExceptionHandler(IntegracaoExternaException.class)
    public ResponseEntity<ApiErrorResponse> tratarIntegracaoExterna(IntegracaoExternaException excecao) {
        LOG.warn("Falha em integração externa: {}", excecao.getMessage(), excecao);
        return construirResposta(HttpStatus.BAD_GATEWAY, excecao.mensagemPublica());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> tratarArquivoGrande(MaxUploadSizeExceededException excecao) {
        return construirResposta(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo grande demais. O limite é 5 MB por foto.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> tratarArquivoAusente(MissingServletRequestPartException excecao) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Nenhum arquivo foi enviado");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException excecao) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + excecao.getName());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> tratarAcessoNegado(AccessDeniedException excecao) {
        return construirResposta(HttpStatus.FORBIDDEN, "Seu perfil de acesso não permite esta ação");
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ApiErrorResponse> tratarAcessoNegadoPorRegra(AcessoNegadoException excecao) {
        return construirResposta(HttpStatus.FORBIDDEN, excecao.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> tratarErroInesperado(Exception excecao) {
        // O cliente recebe mensagem genérica (sem vazar detalhe interno); o detalhe fica no log,
        // senão um 500 fica invisível pra quem opera o sistema.
        LOG.error("Erro inesperado ao processar requisição", excecao);
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Contate o suporte.");
    }

    private ResponseEntity<ApiErrorResponse> construirResposta(HttpStatus status, String mensagem) {
        ApiErrorResponse corpo = new ApiErrorResponse(Instant.now(), status.value(), mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
