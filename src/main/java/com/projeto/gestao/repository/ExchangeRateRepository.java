package com.projeto.gestao.repository;

import com.projeto.gestao.domain.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExchangeRate> findByCurrencyPair(String currencyPair);
}
