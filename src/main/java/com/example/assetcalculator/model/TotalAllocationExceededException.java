package com.example.assetcalculator.model;

import java.math.BigDecimal;

/**
 * Thrown when a change would push the total target allocation above 100%.
 */
public class TotalAllocationExceededException extends IllegalArgumentException {

    private final BigDecimal currentTotal;
    private final BigDecimal newTotal;

    public TotalAllocationExceededException(BigDecimal currentTotal, BigDecimal newTotal) {
        super("Total target allocation cannot exceed 100% (current: " + currentTotal
                + "%, after change: " + newTotal + "%)");
        this.currentTotal = currentTotal;
        this.newTotal = newTotal;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getNewTotal() {
        return newTotal;
    }
}
