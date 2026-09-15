package com.projeto.gestao.api.exception;

public final class BrokerRuleException extends ApiException {
    private static final long serialVersionUID = 1L;

    private final String message;

    private BrokerRuleException(String message) {
        super(ApiErrorCode.BUSINESS_RULE_ERROR);
        this.message = message;
    }

    public static BrokerRuleException invalidCnpj() {
        return new BrokerRuleException("O CNPJ informado é inválido.");
    }

    public static BrokerRuleException notFound() {
        return new BrokerRuleException("Não foi encontrada uma instituição para o CNPJ informado.");
    }

    public static BrokerRuleException inactiveCompany() {
        return new BrokerRuleException("O CNPJ informado não está cadastralmente ativo.");
    }

    public static BrokerRuleException notAuthorized() {
        return new BrokerRuleException("A instituição não está autorizada como CTVM na CVM.");
    }

    public static BrokerRuleException associationNotFound() {
        return new BrokerRuleException("A associação de corretora não foi encontrada.");
    }

    public static BrokerRuleException openPosition() {
        return new BrokerRuleException("A corretora possui posição aberta e não pode ser removida.");
    }

    @Override
    public String publicMessage() {
        return message;
    }
}
