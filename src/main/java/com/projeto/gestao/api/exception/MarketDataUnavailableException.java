package com.projeto.gestao.api.exception;

public final class MarketDataUnavailableException extends ApiException {
    private static final long serialVersionUID = 1L;
    private final String message;

    public MarketDataUnavailableException(String message) {
        super(ApiErrorCode.BUSINESS_RULE_ERROR);
        this.message = message;
    }

    @Override public String publicMessage() { return message; }
}
