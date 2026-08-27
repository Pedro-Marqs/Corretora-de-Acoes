package com.projeto.gestao.domain.port;

/** Falhas normalizadas das fontes externas, sem expor detalhes HTTP ao domínio. */
public final class ExternalDataFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;
    private final String source;

    public ExternalDataFailure(Reason reason, String source, String message) {
        super(message);
        this.reason = reason;
        this.source = source;
    }

    public ExternalDataFailure(Reason reason, String source, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.source = source;
    }

    public Reason reason() {
        return reason;
    }

    public String source() {
        return source;
    }

    public enum Reason {
        INVALID_INPUT,
        NOT_FOUND,
        INCOMPLETE_RESPONSE,
        RATE_LIMITED,
        SERVER_ERROR,
        TIMEOUT,
        TRANSPORT_ERROR,
        INVALID_RESPONSE
    }
}
