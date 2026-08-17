package com.projeto.gestao.api.exception;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<ApiFieldError> fieldErrors,
        String timestamp,
        String errorId) {

    public ApiErrorResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
