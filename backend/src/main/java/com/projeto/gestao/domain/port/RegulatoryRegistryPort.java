package com.projeto.gestao.domain.port;

import com.projeto.gestao.domain.model.RegulatoryRegistration;

public interface RegulatoryRegistryPort {
    RegulatoryRegistration findByCnpj(String cnpj);
}
