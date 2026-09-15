package com.projeto.gestao.api.controller;

import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.WalletService;
import com.projeto.gestao.service.PurchaseService;
import com.projeto.gestao.service.SaleService;
import com.projeto.gestao.service.TransferService;
import com.projeto.gestao.service.WalletPositionsService;
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
    private final SaleService saleService;
    private final TransferService transferService;
    private final WalletPositionsService walletPositionsService;

    public WalletController(WalletService walletService, PurchaseService purchaseService,
            SaleService saleService, TransferService transferService,
            WalletPositionsService walletPositionsService) {
        this.walletService = walletService;
        this.purchaseService = purchaseService;
        this.saleService = saleService;
        this.transferService = transferService;
        this.walletPositionsService = walletPositionsService;
    }

    @GetMapping("/positions")
    WalletPositionsResponse positions(@AuthenticationPrincipal AccountPrincipal principal) {
        return WalletPositionsResponse.from(walletPositionsService.snapshot(principal.accountId()));
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

    @PostMapping("/withdrawals")
    WalletBalanceResponse withdraw(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody WithdrawalRequest request) {
        return new WalletBalanceResponse(walletService.withdraw(principal.accountId(), request.amount()));
    }

    @PostMapping("/purchases")
    PurchaseResponse purchase(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody PurchaseRequest request) {
        return PurchaseResponse.from(purchaseService.purchase(principal.accountId(),
                request.assetId(), request.brokerId(), request.quantity(), request.unitPrice(),
                request.occurredAt()));
    }

    @PostMapping("/sales")
    SaleResponse sale(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody SaleRequest request) {
        return SaleResponse.from(saleService.sell(principal.accountId(),
                request.assetId(), request.brokerId(), request.quantityAsLong(), request.unitPrice(),
                request.occurredAt()));
    }

    @PostMapping("/transfers")
    TransferResponse transfer(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody TransferRequest request) {
        return TransferResponse.from(transferService.transfer(principal.accountId(),
                request.originBrokerId(), request.destinationBrokerId(), request.assetId(),
                request.quantityAsLong()));
    }
}
