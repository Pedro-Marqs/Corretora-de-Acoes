package com.projeto.gestao.security;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public record AccountPrincipal(UUID accountId) implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return accountId.toString();
    }
}
