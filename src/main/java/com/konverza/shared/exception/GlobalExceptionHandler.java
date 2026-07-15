package com.konverza.shared.exception;

import com.konverza.auth.exception.AccountDisabledException;
import com.konverza.auth.exception.EmailAlreadyExistsException;
import com.konverza.auth.exception.InvalidAvatarFileException;
import com.konverza.auth.exception.InvalidCredentialsException;
import com.konverza.auth.exception.InvalidCurrentPasswordException;
import com.konverza.auth.exception.InvalidRefreshTokenException;
import com.konverza.auth.exception.UserNotFoundException;
import com.konverza.empresa.exception.EmpresaNotFoundException;
import com.konverza.productos.exception.ProductoNotFoundException;
import com.konverza.productos.exception.ServicioNotFoundException;
import com.konverza.sessions.exception.SessionNotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProductoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductoNotFound(ProductoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ServicioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleServicioNotFound(ServicioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EmpresaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEmpresaNotFound(EmpresaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage(), "code", "EMAIL_ALREADY_EXISTS"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage(), "code", "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<Map<String, String>> handleAccountDisabled(AccountDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage(), "code", "ACCOUNT_DISABLED"));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage(), "code", "INVALID_REFRESH_TOKEN"));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage(), "code", "INVALID_CURRENT_PASSWORD"));
    }

    @ExceptionHandler(InvalidAvatarFileException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAvatarFile(InvalidAvatarFileException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage(), "code", "INVALID_AVATAR_FILE"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "El archivo supera el tamano maximo permitido (5MB)", "code", "FILE_TOO_LARGE"));
    }

    /**
     * @PreAuthorize denials (method security, add-rbac-permission-matrix) are
     * thrown as AccessDeniedException and resolved here by Spring MVC's
     * exception handling — they never reach SecurityConfig's
     * accessDeniedHandler because that only fires for denials at the filter
     * chain / authorizeHttpRequests level, which this app doesn't use for
     * role checks (all role enforcement is @PreAuthorize-based).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No autorizado para esta accion", "code", "ACCESS_DENIED"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalido"
                ));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validacion fallida", "fields", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}
