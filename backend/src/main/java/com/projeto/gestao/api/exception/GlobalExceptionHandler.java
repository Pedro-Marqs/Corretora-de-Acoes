package com.projeto.gestao.api.exception;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.service.AdminAuditService;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX");
    private final Clock clock;
    private final AdminAuditService adminAuditService;

    @Autowired
    public GlobalExceptionHandler(Clock clock, AdminAuditService adminAuditService) {
        this.clock = clock;
        this.adminAuditService = adminAuditService;
    }

    public GlobalExceptionHandler(Clock clock) {
        this(clock, null);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        ApiErrorCode code = exception.getErrorCode();
        String errorId = newErrorId();
        LOGGER.warn("API request rejected with errorId={} code={} exception={}",
                errorId, code, exception.getClass().getSimpleName());
        return response(code, exception.publicMessage(), List.of(), errorId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        auditAdminInvestorListValidation(request);
        List<ApiFieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .sorted(Comparator.comparing(ApiFieldError::field))
                .toList();
        return validationResponse(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintValidation(
            ConstraintViolationException exception, HttpServletRequest request) {
        auditAdminInvestorListValidation(request);
        List<ApiFieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(
                        lastPathSegment(violation.getPropertyPath().toString()), violation.getMessage()))
                .sorted(Comparator.comparing(ApiFieldError::field))
                .toList();
        return validationResponse(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        auditAdminInvestorListValidation(request);
        List<ApiFieldError> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String field = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new ApiFieldError(field == null ? "parameter" : field, safeMessage(error)));
            }
        });
        errors.sort(Comparator.comparing(ApiFieldError::field));
        return validationResponse(errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        auditAdminInvestorListValidation(request);
        return validationResponse(List.of(new ApiFieldError(exception.getParameterName(), "Parâmetro obrigatório.")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        auditAdminInvestorListValidation(request);
        return validationResponse(List.of(new ApiFieldError(exception.getName(), "Formato inválido.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        auditAdminInvestorListValidation(request);
        String errorId = newErrorId();
        LOGGER.warn("API request rejected with errorId={} code={} exception={}",
                errorId, ApiErrorCode.VALIDATION_ERROR, exception.getClass().getSimpleName());
        return response(ApiErrorCode.VALIDATION_ERROR,
                ApiErrorCode.VALIDATION_ERROR.defaultMessage(), List.of(), errorId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return handleApiException(new AuthorizationException());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        String errorId = newErrorId();
        LOGGER.error("Unexpected API error with errorId={} code={} exception={}",
                errorId, ApiErrorCode.INTERNAL_ERROR, exception.getClass().getSimpleName());
        return response(ApiErrorCode.INTERNAL_ERROR,
                ApiErrorCode.INTERNAL_ERROR.defaultMessage(), List.of(), errorId);
    }

    private ResponseEntity<ApiErrorResponse> validationResponse(List<ApiFieldError> errors) {
        return response(ApiErrorCode.VALIDATION_ERROR, ApiErrorCode.VALIDATION_ERROR.defaultMessage(), errors);
    }

    private ResponseEntity<ApiErrorResponse> response(
            ApiErrorCode code, String message, List<ApiFieldError> errors) {
        return response(code, message, errors, newErrorId());
    }

    private ResponseEntity<ApiErrorResponse> response(
            ApiErrorCode code, String message, List<ApiFieldError> errors, String errorId) {
        ApiErrorResponse body = new ApiErrorResponse(
                code.name(), message, errors, OffsetDateTime.now(clock).format(TIMESTAMP_FORMAT), errorId);
        return ResponseEntity.status(code.status()).body(body);
    }

    private String newErrorId() {
        return UUID.randomUUID().toString();
    }

    private ApiFieldError toFieldError(FieldError error) {
        return new ApiFieldError(error.getField(), safeMessage(error));
    }

    private String safeMessage(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message == null ? "Valor inválido." : message;
    }

    private String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }

    private void auditAdminInvestorListValidation(HttpServletRequest request) {
        if (adminAuditService == null
                || request == null
                || !"GET".equals(request.getMethod())
                || !"/api/admin/investors".equals(request.getRequestURI())) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actorAdminId = authentication != null
                && authentication.getPrincipal() instanceof AdminPrincipal principal
                ? principal.adminId() : null;
        try {
            adminAuditService.recordInNewTransaction(actorAdminId,
                    actorAdminId == null ? AuditActorRole.ANONYMOUS : AuditActorRole.ADMIN,
                    AdminAuditAction.LIST_INVESTORS, null, null, AdminAuditOutcome.DENIED,
                    AdminAuditService.correlationId(request.getHeader("X-Correlation-ID")));
        } catch (RuntimeException auditException) {
            LOGGER.error("Could not persist administrative list validation audit",
                    auditException);
        }
    }
}
