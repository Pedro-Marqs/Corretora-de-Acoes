package com.projeto.gestao.repository;

import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Market;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    Optional<Asset> findByTickerIgnoreCaseAndMarket(String ticker, Market market);
}
