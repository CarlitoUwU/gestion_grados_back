package com.claudecoders.grades.shared.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ResponseEntity<ApiError> handleStatus(
            org.springframework.web.server.ResponseStatusException exception) {
        return response(
                HttpStatus.valueOf(exception.getStatusCode().value()),
                exception.getReason(),
                Map.of());
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraints(
            jakarta.validation.ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception
                .getConstraintViolations()
                .forEach(
                        violation ->
                                errors.put(
                                        violation.getPropertyPath().toString(),
                                        violation.getMessage()));
        return response(
                HttpStatus.BAD_REQUEST, "La solicitud contiene parámetros inválidos", errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException exception) {
        return response(
                HttpStatus.NOT_FOUND,
                "No existe endpoint " + exception.getHttpMethod() + " " + exception.getRequestURL(),
                Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        return response(
                HttpStatus.CONFLICT,
                "El recurso entra en conflicto con datos existentes",
                Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String message, Map<String, String> validationErrors) {
        ApiError error =
                new ApiError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        validationErrors);
        return ResponseEntity.status(status).body(error);
    }
}
