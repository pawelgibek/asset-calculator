package com.example.assetcalculator;

import com.example.assetcalculator.model.Portfolio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The interactive console is disabled here; otherwise it would read from the
 * test JVM's stdin and the context would never finish starting.
 */
@SpringBootTest(properties = "asset-calculator.console.enabled=false")
class AssetCalculatorApplicationTests {

    @Autowired
    private Portfolio portfolio;

    /**
     * Context loads and component scanning registers the portfolio bean the console needs.
     */
    @Test
    void contextProvidesPortfolio() {
        assertThat(portfolio.getPositions()).isNotEmpty();
    }
}
