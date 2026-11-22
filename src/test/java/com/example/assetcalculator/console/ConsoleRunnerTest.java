package com.example.assetcalculator.console;

import com.example.assetcalculator.catalog.Asset;
import com.example.assetcalculator.config.PortfolioConfiguration;
import com.example.assetcalculator.model.Portfolio;
import com.example.assetcalculator.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ConsoleRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PortfolioConfiguration.class, ConsoleRunner.class);

    @Test
    void consoleIsEnabledByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(ConsoleRunner.class));
    }

    @Test
    void consoleIsDisabledWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("asset-calculator.console.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ConsoleRunner.class));
    }

    @Test
    void runStartsConsoleWithGivenPortfolio() {
        Portfolio portfolio = new Portfolio(new BigDecimal("5000"),
                List.of(new Position(Asset.HD, new BigDecimal("100"), new BigDecimal("100"))));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ConsoleRunner(portfolio, new ByteArrayInputStream("EXIT\n".getBytes(StandardCharsets.UTF_8)), out).run();

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("Total Portfolio Value: 5000 USD")
                .contains("Available assets: HD")
                .contains("Exiting Asset Calculator...");
    }
}
