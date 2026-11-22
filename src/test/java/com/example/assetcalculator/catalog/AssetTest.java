package com.example.assetcalculator.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTest {

    /**
     * Unit prices are used as divisors when calculating shares,
     * so a zero or negative price would break every calculation.
     */
    @Test
    void allUnitPricesArePositive() {
        for (Asset asset : Asset.values()) {
            assertThat(asset.getUnitPrice())
                    .as("unit price of %s", asset)
                    .isPositive();
        }
    }
}
