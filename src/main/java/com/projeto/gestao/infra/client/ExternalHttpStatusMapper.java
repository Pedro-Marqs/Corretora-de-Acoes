package com.projeto.gestao.infra.client;

import com.projeto.gestao.domain.port.ExternalDataFailure;

public final class ExternalHttpStatusMapper {

    private ExternalHttpStatusMapper() {
    }

    public static void requireSuccess(int status, String source) {
        if (status >= 200 && status < 300) {
            return;
        }
        ExternalDataFailure.Reason reason = switch (status) {
            case 400 -> ExternalDataFailure.Reason.INVALID_INPUT;
            case 404 -> ExternalDataFailure.Reason.NOT_FOUND;
            case 429 -> ExternalDataFailure.Reason.RATE_LIMITED;
            default -> status >= 500
                    ? ExternalDataFailure.Reason.SERVER_ERROR
                    : ExternalDataFailure.Reason.INVALID_RESPONSE;
        };
        throw new ExternalDataFailure(reason, source,
                "Resposta HTTP " + status + " recebida de " + source);
    }
}
