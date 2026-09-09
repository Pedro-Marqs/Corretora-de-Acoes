package com.projeto.gestao.service;

import java.util.List;

public record HistoryPage(List<HistoryMovementView> content, int page, int size,
        long totalElements, int totalPages) { }
