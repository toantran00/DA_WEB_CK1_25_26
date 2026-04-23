package vn.iotstar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.iotstar.model.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler with dual behavior:
 * - REST API endpoints (/api/**): returns structured JSON ({ success, message, ... })
 * - MVC/Thymeleaf pages: returns an error view
 *
 * This centralizes error handling across all controllers, eliminating scattered try-catch blocks.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========================================================================================
    //  REST API Exception Handlers (JSON responses)
    // ========================================================================================

    /**
     * Handles @Valid / @Validated constraint violations on @RequestBody.
     * Returns a 400 with a map of field -> error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Giá trị không hợp lệ",
                        (existing, replacement) -> existing // keep first error per field
                ));

        logger.warn("Validation failed for request [{}]: {}", request.getDescription(false), fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Dữ liệu không hợp lệ", List.copyOf(
                        fieldErrors.entrySet().stream()
                                .map(e -> e.getKey() + ": " + e.getValue())
                                .collect(Collectors.toList())
                )));
    }

    /**
     * Handles authentication failures (wrong password, bad credentials).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email hoặc mật khẩu không đúng"));
    }

    /**
     * Handles access denied (insufficient role/permission).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied for request [{}]: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này"));
    }

    /**
     * Handles file upload size exceeded (spring.servlet.multipart.max-file-size).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File quá lớn. Kích thước tối đa là 10MB"));
    }

    /**
     * Handles known business logic errors thrown as RuntimeException.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Business error on request [{}]: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles IllegalArgumentException — typically from bad input parameters.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Tham số không hợp lệ: " + ex.getMessage()));
    }

    /**
     * Catch-all fallback handler for unexpected errors.
     * Returns 500 without exposing internal details to the caller.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception on request [{}]", request.getDescription(false), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Có lỗi hệ thống xảy ra. Vui lòng thử lại sau."));
    }
}
