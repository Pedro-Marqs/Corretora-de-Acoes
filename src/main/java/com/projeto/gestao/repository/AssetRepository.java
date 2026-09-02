package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    Optional<Asset> findByTickerIgnoreCaseAndMarket(String ticker, Market market);

    @Query("select distinct a from Position p join p.asset a "
            + "where p.quantity > 0 and a.status = com.projeto.gestao.domain.model.AssetStatus.ACTIVE "
            + "and a.market = :market")
    List<Asset> findDistinctActiveWithPositivePositionByMarket(@Param("market") Market market);
}
