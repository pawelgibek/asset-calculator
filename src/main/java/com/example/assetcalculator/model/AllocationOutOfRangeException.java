package com.example.assetcalculator.model;

import java.math.BigDecimal;

/**
 * Thrown when a single allocation is outside the 0–100% range.
 */
public class AllocationOutOfRangeException extends IllegalArgumentException {

    public AllocationOutOfRangeException(String name, BigDecimal value) {
        super(name + " must be between 0 and 100% but was " + value + "%");
    }
}
