package com.example.assetcalculator.config;

import com.example.assetcalculator.catalog.Asset;
import com.example.assetcalculator.model.Portfolio;
import com.example.assetcalculator.model.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Range, duplicate and "at most 100%" rules are enforced by Portfolio itself;
 * here we only check what is specific to the initial data.
 */
class PortfolioConfigurationTest {

    private final Portfolio portfolio = new PortfolioConfiguration().portfolio();

    @Test
    void portfolioContainsEveryCatalogAsset() {
        assertThat(portfolio.getPositions())
                .extracting(Position::asset)
                .containsExactlyInAnyOrder(Asset.values());
    }

    @Test
    void portfolioIsFullyAllocated() {
        BigDecimal currentTotal = portfolio.getPositions().stream()
                .map(Position::currentAllocation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(portfolio.getTotalTargetAllocation()).isEqualByComparingTo("100");
        assertThat(currentTotal).isEqualByComparingTo("100");
    }
}
