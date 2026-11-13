package com.projeto.gestao.domain.dto;

import java.util.List;

public record AdminInvestorPage(
        List<AdminInvestorResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) { }
