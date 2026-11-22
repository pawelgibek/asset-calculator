package com.example.assetcalculator.model;

import com.example.assetcalculator.catalog.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionTest {

    private static final BigDecimal TOTAL = new BigDecimal("100000");

    private static Position position(Asset asset, String target, String current) {
        return new Position(asset, new BigDecimal(target), new BigDecimal(current));
    }

    @Test
    void rejectsNullArguments() {
        BigDecimal ten = BigDecimal.TEN;

        assertThatNullPointerException().isThrownBy(() -> new Position(null, ten, ten));
        assertThatNullPointerException().isThrownBy(() -> new Position(Asset.IBM, null, ten));
        assertThatNullPointerException().isThrownBy(() -> new Position(Asset.IBM, ten, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100"})
    void acceptsAllocationsOnRangeBoundaries(String value) {
        Position position = position(Asset.IBM, value, value);

        assertThat(position.targetAllocation()).isEqualByComparingTo(value);
        assertThat(position.currentAllocation()).isEqualByComparingTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01"})
    void rejectsAllocationsJustOutsideRange(String value) {
        assertThatThrownBy(() -> position(Asset.IBM, value, "0")).isInstanceOf(AllocationOutOfRangeException.class);
        assertThatThrownBy(() -> position(Asset.IBM, "0", value)).isInstanceOf(AllocationOutOfRangeException.class);
    }

    @Test
    void investedAmountIsCurrentAllocationShareOfTotalWithTwoDecimals() {
        assertThat(position(Asset.IBM, "20", "10").investedAmount(TOTAL))
                .isEqualTo(new BigDecimal("10000.00"));
    }

    @Test
    void investedAmountRoundsHalfUp() {
        // 1 * 0.5 / 100 = 0.005 -> 0.01
        assertThat(position(Asset.IBM, "0", "0.5").investedAmount(BigDecimal.ONE))
                .isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void sharesAreInvestedAmountDividedByPriceWithTwoDecimals() {
        // 10000.00 / 150 = 66.666... -> 66.67
        assertThat(position(Asset.IBM, "20", "10").shares(TOTAL))
                .isEqualTo(new BigDecimal("66.67"));
    }

    @Test
    void sharesRoundHalfUp() {
        // invested = 35 * 1% = 0.35; 0.35 / 70 = 0.005 -> 0.01
        assertThat(position(Asset.HD, "0", "1").shares(new BigDecimal("35")))
                .isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void varianceIsTargetMinusCurrent() {
        assertThat(position(Asset.IBM, "20", "10").variance()).isEqualByComparingTo("10");
        assertThat(position(Asset.ORCL, "20", "30").variance()).isEqualByComparingTo("-10");
    }

    /**
     * The negative case also catches sign bugs (abs(), max(0, ...)) that a positive case would miss.
     */
    @Test
    void sharesToTradeIsNegativeWhenOverweight() {
        // -10% of 100000 = -10000.00; / 220 = -45.4545... -> -45.45
        assertThat(position(Asset.ORCL, "20", "30").sharesToTrade(TOTAL))
                .isEqualTo(new BigDecimal("-45.45"));
    }
}
