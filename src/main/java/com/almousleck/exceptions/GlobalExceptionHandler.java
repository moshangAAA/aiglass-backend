package com.almousleck.exceptions;

import com.almousleck.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException exception,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "用户名或密码错误", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("JSON parse error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "请求格式错误，请检查JSON数据格式", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error occurred at {}: ", request.getRequestURI(), exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误，请稍后再试", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ErrorResponse> handleUserLockedException(
            UserLockedException ex,
            HttpServletRequest request) {
        log.warn("用户账户已被锁定 {}: {} ", request.getRequestURI(), ex.getMessage());

        // Build custom error response with lock time
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.LOCKED.value())
                .error(HttpStatus.LOCKED.getReasonPhrase())
                .message(ex.getMessage() + " 解锁时间: " + ex.getUnlockTime())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(response, HttpStatus.LOCKED);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtpException(
            InvalidOtpException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpiredException(
            OtpExpiredException exception,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(OtpRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleOtpRateLimitException(
            OtpRateLimitException ex,
            HttpServletRequest request) {
        log.warn("OTP 验证频率超过限制 {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(ex.getMessage() + " 请在 " + ex.getRetryAfterSeconds() + " 秒后重试")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPermissionsException(
            InsufficientPermissionsException exception,
            HttpServletRequest request) {
        log.warn("没有足够的权限访问 {}: {}", request.getRequestURI(), exception.getMessage());
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(PhoneNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handlePhoneNotVerifiedException(
            PhoneNotVerifiedException ex,
            HttpServletRequest request) {
        log.warn("Phone not verified login attempt at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ErrorResponse> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex,
            HttpServletRequest request) {
        // Check if the cause is UserLockedException
        if (ex.getCause() instanceof UserLockedException) {
            UserLockedException lockedException = (UserLockedException) ex.getCause();

            log.warn("Account locked attempt at {}: {}", request.getRequestURI(), lockedException.getMessage());

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.LOCKED.value())
                    .error(HttpStatus.LOCKED.getReasonPhrase())
                    .message(lockedException.getMessage() + " 解锁时间: " + lockedException.getUnlockTime())
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.LOCKED);
        }

        // Check if the cause is PhoneNotVerifiedException
        if(ex.getCause() instanceof PhoneNotVerifiedException) {
            PhoneNotVerifiedException phoneNotVerifiedException = (PhoneNotVerifiedException) ex.getCause();
            return buildErrorResponse(
                    HttpStatus.FORBIDDEN, phoneNotVerifiedException.getMessage(), request
            );
        }

        // Other internal auth errors
        log.error("Internal authentication error: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "认证服务内部错误", request);
    }

    // Helper method
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}

