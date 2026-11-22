package com.example.assetcalculator.catalog;

import java.math.BigDecimal;

/**
 * Single source of truth for all securities supported by the calculator
 * together with their unit prices (USD).
 */
public enum Asset {

    IBM("150"),
    MSFT("90"),
    ORCL("220"),
    AAPL("450"),
    HD("70");

    private final BigDecimal unitPrice;

    Asset(String unitPrice) {
        this.unitPrice = new BigDecimal(unitPrice);
    }

    public String getSymbol() {
        return name();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
