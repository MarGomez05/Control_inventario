package com.tamachihua_erp.api.exception;

import com.tamachihua_erp.api.dto.common.ApiErrorResponseDto;
import com.tamachihua_erp.api.dto.common.ApiFieldErrorDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Recurso no encontrado: HTTP 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    // 2. Conflicto de negocio: HTTP 409
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResponseDto> handleConflict(
            ResourceConflictException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    // 3. Errores de validación: HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiFieldErrorDto> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ApiFieldErrorDto(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ApiErrorResponseDto response = new ApiErrorResponseDto(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 3.1 Regla de negocio sobre un campo (ej.: measureUnitId inexistente o inactivo): HTTP 400 con errors[]
    @ExceptionHandler(InvalidFieldException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidField(
            InvalidFieldException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponseDto response = new ApiErrorResponseDto(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                List.of(new ApiFieldErrorDto(ex.getField(), ex.getMessage()))
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 4. JSON incorrecto o mal formado: HTTP 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud tiene un formato inválido",
                request
        );
    }

    // 5. Parámetro obligatorio ausente: HTTP 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio: " + ex.getParameterName(),
                request
        );
    }

    // 6. Parámetro con tipo incorrecto: HTTP 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor o tipo inválido",
                request
        );
    }

    // 7. Metodo HTTP no permitido: HTTP 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "El método HTTP utilizado no está permitido para este recurso",
                request
        );
    }

    // 8. Restricciones de integridad de PostgreSQL
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String sqlState = findSqlState(ex);

        // 23505 = unique_violation
        if ("23505".equals(sqlState)) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "Ya existe un registro con los datos únicos proporcionados",
                    request
            );
        }

        // 23503 = foreign_key_violation
        if ("23503".equals(sqlState)) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "No se puede completar la operación porque existe un conflicto con los registros relacionados",
                    request
            );
        }

        // 23514 = check_violation; 23502 = not_null_violation
        if ("23514".equals(sqlState) || "23502".equals(sqlState)) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "Uno o más datos no cumplen las restricciones requeridas",
                    request
            );
        }

        log.error("Error de integridad de datos no clasificado", ex);

        return buildResponse(
                HttpStatus.CONFLICT,
                "No se pudo completar la operación debido a una restricción de datos",
                request
        );
    }

    // 9. Cualquier error inesperado: HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Error interno no controlado en la API", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor",
                request
        );
    }

    // Construye la respuesta estándar para errores generales
    private ResponseEntity<ApiErrorResponseDto> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponseDto response = new ApiErrorResponseDto(
                Instant.now(),
                status.value(),
                status.name(),
                message,
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(status).body(response);
    }

    // Busca el SQLState dentro de la cadena de causas de la excepción
    private String findSqlState(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }

            current = current.getCause();
        }

        return null;
    }

    // 10. Usuario autenticado sin permiso sobre el recurso o la interfaz: HTTP 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    // 11. Error de autenticación: HTTP 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponseDto response = new ApiErrorResponseDto(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Credenciales inválidas",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
}