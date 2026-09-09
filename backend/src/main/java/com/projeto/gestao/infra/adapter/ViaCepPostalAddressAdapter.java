package com.projeto.gestao.infra.adapter;

import org.springframework.stereotype.Component;

import com.projeto.gestao.domain.model.PostalAddress;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.PostalAddressPort;
import com.projeto.gestao.infra.client.ViaCepClient;
import com.projeto.gestao.infra.client.dto.ViaCepAddressResponse;

@Component
public class ViaCepPostalAddressAdapter implements PostalAddressPort {

    private final ViaCepClient client;

    public ViaCepPostalAddressAdapter(ViaCepClient client) {
        this.client = client;
    }

    @Override
    public PostalAddress findByPostalCode(String postalCode) {
        String normalized = ExternalIdentifierNormalizer.postalCode(postalCode, ViaCepClient.SOURCE);
        ViaCepAddressResponse response = client.findByPostalCode(normalized);
        if (Boolean.TRUE.equals(response.erro())) {
            throw new ExternalDataFailure(ExternalDataFailure.Reason.NOT_FOUND, ViaCepClient.SOURCE,
                    "CEP não encontrado");
        }
        require(response.cep(), "cep");
        require(response.logradouro(), "logradouro");
        require(response.bairro(), "bairro");
        require(response.localidade(), "localidade");
        require(response.uf(), "uf");
        String responsePostalCode = ExternalIdentifierNormalizer.postalCode(response.cep(), ViaCepClient.SOURCE);
        if (!normalized.equals(responsePostalCode)) {
            throw incomplete("cep divergente");
        }
        return new PostalAddress(responsePostalCode, response.logradouro().trim(), optional(response.complemento()),
                response.bairro().trim(), response.localidade().trim(), response.uf().trim());
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw incomplete(field);
        }
    }

    private static ExternalDataFailure incomplete(String field) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INCOMPLETE_RESPONSE, ViaCepClient.SOURCE,
                "Campo obrigatório ausente ou inválido: " + field);
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
