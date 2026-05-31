package com.rapidocurier.clientsservice.infrastructure.config;

import com.rapidocurier.clientsservice.domain.exception.ConflictException;
import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.clientsservice.infrastructure.common.ApiResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, List<String>> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errores.computeIfAbsent(fieldName, k -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        });
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Validación fallida", errores));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(ExternalServiceException ex) {
        return ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuito abierto para RENIEC: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, "RENIEC no disponible, intente más tarde");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
