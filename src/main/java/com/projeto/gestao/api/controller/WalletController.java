package com.projeto.gestao.api.controller;

import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.WalletService;
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

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
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
}
