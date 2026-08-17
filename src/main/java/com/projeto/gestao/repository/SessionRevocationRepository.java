package com.projeto.gestao.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRevocationRepository {
    private final JdbcTemplate jdbcTemplate;

    public SessionRevocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int revokeAll(UUID accountId) {
        return jdbcTemplate.update(
                "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", accountId.toString());
    }
}
