package com.example.assetcalculator.config;

import com.example.assetcalculator.catalog.Asset;
import com.example.assetcalculator.model.Portfolio;
import com.example.assetcalculator.model.Position;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Provides the initial portfolio as a Spring bean.
 */
@Configuration
public class PortfolioConfiguration {

    private static final BigDecimal TOTAL_PORTFOLIO_VALUE = new BigDecimal("100000");

    @Bean
    public Portfolio portfolio() {
        return new Portfolio(TOTAL_PORTFOLIO_VALUE, List.of(
                position(Asset.IBM, "20", "10"),
                position(Asset.MSFT, "20", "20"),
                position(Asset.ORCL, "20", "30"),
                position(Asset.AAPL, "20", "20"),
                position(Asset.HD, "20", "20")
        ));
    }

    private static Position position(Asset asset, String targetAllocation, String currentAllocation) {
        return new Position(asset, new BigDecimal(targetAllocation), new BigDecimal(currentAllocation));
    }
}
