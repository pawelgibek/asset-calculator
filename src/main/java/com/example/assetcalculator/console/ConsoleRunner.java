package com.example.assetcalculator.console;

import com.example.assetcalculator.model.Portfolio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Starts the interactive console. Can be switched off with
 * {@code asset-calculator.console.enabled=false} (e.g. in tests or CI).
 */
@Component
@ConditionalOnProperty(name = "asset-calculator.console.enabled", havingValue = "true", matchIfMissing = true)
public class ConsoleRunner implements CommandLineRunner {

    private final Portfolio portfolio;
    private final InputStream in;
    private final PrintStream out;

    @Autowired
    public ConsoleRunner(Portfolio portfolio) {
        this(portfolio, System.in, System.out);
    }

    ConsoleRunner(Portfolio portfolio, InputStream in, PrintStream out) {
        this.portfolio = portfolio;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run(String... args) {
        new PortfolioConsole(portfolio, in, out).start();
    }
}
