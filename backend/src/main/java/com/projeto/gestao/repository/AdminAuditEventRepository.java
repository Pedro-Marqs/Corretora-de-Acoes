package com.projeto.gestao.repository;

import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditEventRepository extends JpaRepository<AdminAuditEvent, UUID> { }
