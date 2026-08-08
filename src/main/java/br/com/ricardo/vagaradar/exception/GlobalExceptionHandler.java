package br.com.ricardo.vagaradar.exception;

import jakarta.servlet.http.HttpServletRequest;
import br.com.ricardo.vagaradar.integration.gmail.GmailIntegrationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VagaNaoEncontradaException.class)
    public ResponseEntity<ApiErrorResponse> handleVagaNaoEncontrada(
            VagaNaoEncontradaException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(VagaDuplicadaException.class)
    public ResponseEntity<ApiErrorResponse> handleVagaDuplicada(
            VagaDuplicadaException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return response(HttpStatus.BAD_REQUEST, "A requisição contém campos inválidos.", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "O corpo da requisição está inválido.", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "O recurso viola uma regra de integridade dos dados.", request, Map.of());
    }

    @ExceptionHandler(OpenAiIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleOpenAiIntegration(
            OpenAiIntegrationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DiscordIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleDiscordIntegration(
            DiscordIntegrationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(GmailIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleGmailIntegration(
            GmailIntegrationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fields
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fields
        );
        return ResponseEntity.status(status).body(body);
    }
}
