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

    public static ConflictException reactivationUnavailable() {
        return new ConflictException("A conta não pode ser reativada com os dados informados.");
    }

    public static ConflictException invalidAccountStatusTransition() {
        return new ConflictException("A transição de estado da conta não é válida.");
    }

    public static ConflictException invalidAdminStatusTransition() {
        return new ConflictException("Invalid administrator status transition.");
    }

    public static ConflictException lastActiveAdministratorCannotBeInactivated() {
        return new ConflictException("The last active administrator cannot be inactivated.");
    }

    @Override
    public String publicMessage() {
        return message;
    }
}
