package com.github.dimitryivaniuta.gateway.web.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Global REST exception handler for the gateway (non-GraphQL endpoints).
 *
 * <p>Produces a stable JSON error structure for all exceptions.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Standard error response payload.
     */
    public record ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path
    ) { }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode  = ex.getStatusCode();
        HttpStatus status = (statusCode instanceof HttpStatus httpStatus)
                ? httpStatus
                : HttpStatus.valueOf(statusCode.value());
        log.debug("ResponseStatusException [{}] on {} {}: {}",
                status.value(), request.getMethod(), request.getRequestURI(), ex.getReason(), ex);

        return buildResponse(status, ex.getReason(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : err.toString())
                .orElse("Validation failed");

        log.debug("Validation failed on {} {}: {}", request.getMethod(), request.getRequestURI(), message, ex);

        return buildResponse(status, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.debug("IllegalArgumentException on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(status, ex.getMessage(), request);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(
            ErrorResponseException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        log.warn("ErrorResponseException [{}] on {} {}: {}",
                status.value(), request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(status, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(status, "Internal server error", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message != null ? message : "",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
