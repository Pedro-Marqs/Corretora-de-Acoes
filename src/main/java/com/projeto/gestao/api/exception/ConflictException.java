package com.projeto.gestao.api.exception;

public final class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;

    private ConflictException() {
        super(ApiErrorCode.CONFLICT_ERROR);
    }

    public static ConflictException brokerAlreadyRegistered() {
        return new ConflictException();
    }

    @Override
    public String publicMessage() {
        return "A corretora já está cadastrada na conta.";
    }
}
