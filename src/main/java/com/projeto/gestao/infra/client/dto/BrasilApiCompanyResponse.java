package com.projeto.gestao.infra.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiCompanyResponse(
        String cnpj,
        @JsonProperty("razao_social") String legalName,
        @JsonProperty("nome_fantasia") String tradeName,
        @JsonProperty("descricao_situacao_cadastral") String registrationStatus,
        String cep) {
}
