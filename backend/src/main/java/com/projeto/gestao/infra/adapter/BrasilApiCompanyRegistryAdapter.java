package com.projeto.gestao.infra.adapter;

import org.springframework.stereotype.Component;

import com.projeto.gestao.domain.model.CompanyRegistration;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.BrasilApiClient;
import com.projeto.gestao.infra.client.dto.BrasilApiCompanyResponse;

@Component
public class BrasilApiCompanyRegistryAdapter implements CompanyRegistryPort {

    private final BrasilApiClient client;

    public BrasilApiCompanyRegistryAdapter(BrasilApiClient client) {
        this.client = client;
    }

    @Override
    public CompanyRegistration findByCnpj(String cnpj) {
        String normalized = ExternalIdentifierNormalizer.cnpj(cnpj, BrasilApiClient.SOURCE);
        BrasilApiCompanyResponse response = client.findByCnpj(normalized);
        require(response.cnpj(), "cnpj");
        require(response.legalName(), "razao_social");
        require(response.registrationStatus(), "descricao_situacao_cadastral");
        require(response.cep(), "cep");

        String responseCnpj = ExternalIdentifierNormalizer.cnpj(response.cnpj(), BrasilApiClient.SOURCE);
        if (!normalized.equals(responseCnpj)) {
            throw incomplete("cnpj divergente");
        }
        String postalCode = ExternalIdentifierNormalizer.postalCode(response.cep(), BrasilApiClient.SOURCE);
        return new CompanyRegistration(responseCnpj, response.legalName().trim(), optional(response.tradeName()),
                response.registrationStatus().trim(), postalCode);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw incomplete(field);
        }
    }

    private static ExternalDataFailure incomplete(String field) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INCOMPLETE_RESPONSE, BrasilApiClient.SOURCE,
                "Campo obrigatório ausente ou inválido: " + field);
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
