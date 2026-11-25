package com.projeto.gestao.service;

import java.util.concurrent.locks.ReentrantLock;

final class MarketDataRefreshLock {
    static final ReentrantLock USD_BRL = new ReentrantLock();

    private MarketDataRefreshLock() {
    }
}
