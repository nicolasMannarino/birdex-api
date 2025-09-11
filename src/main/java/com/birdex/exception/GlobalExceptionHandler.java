package com.birdex.exception;

import com.birdex.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest req) {
        log.warn("User not found: {}", ex.getEmail());
        ErrorResponse body = baseBuilder(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(), req)
                .details(Map.of("email", ex.getEmail()))
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }


    @ExceptionHandler(BirdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBirdNotFound(BirdNotFoundException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        if (ex.getBirdName() != null) details.put("birdName", ex.getBirdName());
        if (ex.getBirdId() != null)   details.put("birdId", ex.getBirdId());

        ErrorResponse body = baseBuilder(HttpStatus.NOT_FOUND, "BIRD_NOT_FOUND", ex.getMessage(), req)
                .details(details.isEmpty() ? null : details)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.Violation> violations = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toViolation)
                .collect(Collectors.toList());

        ErrorResponse body = baseBuilder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", req)
                .violations(violations)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ErrorResponse.Violation> violations = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.Violation.builder()
                        .field(cv.getPropertyPath().toString())
                        .reason(cv.getMessage())
                        .rejectedValue(cv.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse body = baseBuilder(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", "Validation failed", req)
                .violations(violations)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ErrorResponse body = baseBuilder(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed JSON request body", req)
                .details(Map.of(
                        "cause", Objects.requireNonNull(Optional.of(ex.getMostSpecificCause())
                                .map(Throwable::getClass)
                                .map(Class::getSimpleName)
                                .orElse(null))))
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        ErrorResponse body = baseBuilder(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", ex.getMessage(), req)
                .details(Map.of("parameterName", ex.getParameterName(), "parameterType", ex.getParameterType()))
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        ErrorResponse body = baseBuilder(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", req)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ErrorResponse.ErrorResponseBuilder baseBuilder(HttpStatus status, String code, String message, HttpServletRequest req) {
        String traceId = Optional.ofNullable(MDC.get("traceId"))
                .orElseGet(() -> req.getHeader("X-Trace-Id"));

        return ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(req.getRequestURI())
                .method(req.getMethod())
                .traceId(traceId);
    }

    private ErrorResponse.Violation toViolation(FieldError fe) {
        return ErrorResponse.Violation.builder()
                .field(fe.getField())
                .reason(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue())
                .build();
    }
}
