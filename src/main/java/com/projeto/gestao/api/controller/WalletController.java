package com.projeto.gestao.api.controller;

import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.WalletService;
import com.projeto.gestao.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;
    private final PurchaseService purchaseService;

    public WalletController(WalletService walletService, PurchaseService purchaseService) {
        this.walletService = walletService;
        this.purchaseService = purchaseService;
    }

    @GetMapping
    WalletBalanceResponse balance(@AuthenticationPrincipal AccountPrincipal principal) {
        return new WalletBalanceResponse(walletService.balance(principal.accountId()));
    }

    @PostMapping("/deposits")
    WalletBalanceResponse deposit(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody DepositRequest request) {
        return new WalletBalanceResponse(walletService.deposit(principal.accountId(), request.amount()));
    }

    @PostMapping("/purchases")
    PurchaseResponse purchase(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody PurchaseRequest request) {
        return PurchaseResponse.from(purchaseService.purchase(principal.accountId(),
                request.assetId(), request.brokerId(), request.quantity()));
    }
}
