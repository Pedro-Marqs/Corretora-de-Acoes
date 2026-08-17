package com.projeto.gestao.api.exception;

public final class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;

    private final String message;

    private ConflictException(String message) {
        super(ApiErrorCode.CONFLICT_ERROR);
        this.message = message;
    }

    public static ConflictException brokerAlreadyRegistered() {
        return new ConflictException("A corretora já está cadastrada na conta.");
    }

    public static ConflictException activeAccountAlreadyExists() {
        return new ConflictException("Já existe uma conta ativa com o CPF ou e-mail informado.");
    }

    @Override
    public String publicMessage() {
        return message;
    }
}
