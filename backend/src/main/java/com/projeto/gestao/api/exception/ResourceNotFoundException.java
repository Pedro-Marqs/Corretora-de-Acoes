package com.projeto.gestao.api.exception;

public final class ResourceNotFoundException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException() {
        super(ApiErrorCode.NOT_FOUND_ERROR);
    }
}
