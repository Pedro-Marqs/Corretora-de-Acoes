package com.projeto.gestao.domain.port;

import com.projeto.gestao.domain.model.CompanyRegistration;

public interface CompanyRegistryPort {
    CompanyRegistration findByCnpj(String cnpj);
}
