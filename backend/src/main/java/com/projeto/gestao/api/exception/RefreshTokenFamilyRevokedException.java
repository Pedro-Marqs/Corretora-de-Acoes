package com.projeto.gestao.api.exception;

public final class RefreshTokenFamilyRevokedException extends ApiException {
    private static final long serialVersionUID = 1L;

    public RefreshTokenFamilyRevokedException() {
        super(ApiErrorCode.AUTHENTICATION_ERROR);
    }
}
