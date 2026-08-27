package com.projeto.gestao.api.controller;

import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.service.AssetCatalogService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetCatalogService service;

    public AssetController(AssetCatalogService service) { this.service = service; }

    @GetMapping("/search")
    AssetPriceResponse search(
            @RequestParam @Pattern(regexp = "(?i)[A-Z0-9]{1,12}", message = "Ticker invÃ¡lido.") String ticker,
            @RequestParam Market market) {
        return AssetPriceResponse.from(service.find(ticker, market));
    }
}
