package com.example.assetcalculator.model;

import com.example.assetcalculator.catalog.Asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable holding of a single asset in a {@link Portfolio}.
 * Allocations are percentages of the whole portfolio and always within 0–100%.
 *
 * @param asset             the held asset
 * @param targetAllocation  desired share of the portfolio (%)
 * @param currentAllocation actual share of the portfolio (%)
 */
public record Position(Asset asset, BigDecimal targetAllocation, BigDecimal currentAllocation) {

    public Position {
        Objects.requireNonNull(asset, "asset must not be null");
        Percentages.requireAllocation("targetAllocation", targetAllocation);
        Percentages.requireAllocation("currentAllocation", currentAllocation);
    }

    public String symbol() {
        return asset.getSymbol();
    }

    public BigDecimal unitPrice() {
        return asset.getUnitPrice();
    }

    /**
     * Target minus current allocation (%): positive means underweight, negative overweight.
     */
    public BigDecimal variance() {
        return targetAllocation.subtract(currentAllocation);
    }

    public BigDecimal investedAmount(BigDecimal portfolioValue) {
        return Percentages.of(portfolioValue, currentAllocation);
    }

    public BigDecimal shares(BigDecimal portfolioValue) {
        return toShares(investedAmount(portfolioValue));
    }

    /**
     * Shares to buy (positive) or sell (negative) to reach the target allocation.
     */
    public BigDecimal sharesToTrade(BigDecimal portfolioValue) {
        return toShares(Percentages.of(portfolioValue, variance()));
    }

    Position withTargetAllocation(BigDecimal newTarget) {
        return new Position(asset, newTarget, currentAllocation);
    }

    Position rebalanced() {
        return new Position(asset, targetAllocation, targetAllocation);
    }

    private BigDecimal toShares(BigDecimal amount) {
        return amount.divide(unitPrice(), 2, RoundingMode.HALF_UP);
    }
}
