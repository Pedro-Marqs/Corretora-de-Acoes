package com.projeto.gestao.domain.port;

/** Falhas normalizadas das fontes externas, sem expor detalhes HTTP ao domínio. */
public final class ExternalDataFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;
    private final String source;
    private final Integer statusCode;

    public ExternalDataFailure(Reason reason, String source, String message) {
        this(reason, source, message, null, null);
    }

    public ExternalDataFailure(Reason reason, String source, String message, Throwable cause) {
        this(reason, source, message, null, cause);
    }

    public ExternalDataFailure(Reason reason, String source, String message, Integer statusCode) {
        this(reason, source, message, statusCode, null);
    }

    public ExternalDataFailure(Reason reason, String source, String message,
            Integer statusCode, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.source = source;
        this.statusCode = statusCode;
    }

    public Reason reason() {
        return reason;
    }

    public String source() {
        return source;
    }

    public Integer statusCode() {
        return statusCode;
    }

    public enum Reason {
        INVALID_INPUT,
        AUTHENTICATION,
        NOT_FOUND,
        INCOMPLETE_RESPONSE,
        RATE_LIMITED,
        SERVER_ERROR,
        TIMEOUT,
        TRANSPORT_ERROR,
        INVALID_RESPONSE
    }
}
