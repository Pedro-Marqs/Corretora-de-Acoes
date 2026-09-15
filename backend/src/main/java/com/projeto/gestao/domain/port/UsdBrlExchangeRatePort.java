package com.projeto.gestao.domain.port;

import com.projeto.gestao.domain.model.UsdBrlRate;

public interface UsdBrlExchangeRatePort {
    UsdBrlRate currentRate();
}
