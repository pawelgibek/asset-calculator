package com.example.assetcalculator.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Percentage rules and arithmetic shared by the portfolio model.
 */
final class Percentages {

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Percentages() {
    }

    /**
     * Returns {@code percentage}% of {@code amount}, rounded half-up to 2 decimal places.
     */
    static BigDecimal of(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * @throws NullPointerException          if {@code value} is null
     * @throws AllocationOutOfRangeException if {@code value} is outside 0–100%
     */
    static BigDecimal requireAllocation(String name, BigDecimal value) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw new AllocationOutOfRangeException(name, value);
        }
        return value;
    }

    static boolean exceedsHundred(BigDecimal value) {
        return value.compareTo(HUNDRED) > 0;
    }
}
