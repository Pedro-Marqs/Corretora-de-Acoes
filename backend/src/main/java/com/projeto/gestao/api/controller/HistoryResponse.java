package com.projeto.gestao.api.controller;

import java.util.List;

import com.projeto.gestao.service.HistoryMovementView;
import com.projeto.gestao.service.HistoryPage;

public record HistoryResponse(List<HistoryMovementView> content, int page, int size,
        long totalElements, int totalPages) {
    static HistoryResponse from(HistoryPage result) {
        return new HistoryResponse(result.content(), result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
