package com.example.assetcalculator.console;

import com.example.assetcalculator.model.AllocationOutOfRangeException;
import com.example.assetcalculator.model.Portfolio;
import com.example.assetcalculator.model.Position;
import com.example.assetcalculator.model.TotalAllocationExceededException;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Interactive session: reads commands, parses input and delegates to {@link Portfolio}.
 * Business rules live in {@link Portfolio}; all output goes through {@link ConsoleView}.
 * Streams are injected so the session can be driven from tests.
 */
public class PortfolioConsole {

    /**
     * Plain decimal number with at most 2 decimal places. A leading minus is
     * accepted so negative values get the "must be between 0 and 100%" message.
     * Rejects exponents (1e1), a plus sign and decimal commas.
     */
    private static final Pattern PERCENTAGE = Pattern.compile("-?\\d+(\\.\\d{1,2})?");

    private final Portfolio portfolio;
    private final Scanner scanner;
    private final ConsoleView view;

    public PortfolioConsole(Portfolio portfolio, InputStream in, PrintStream out) {
        this.portfolio = Objects.requireNonNull(portfolio, "portfolio must not be null");
        this.scanner = new Scanner(Objects.requireNonNull(in, "in must not be null"), StandardCharsets.UTF_8);
        this.view = new ConsoleView(out);
    }

    /**
     * Runs until the user types EXIT or the input is closed.
     */
    public void start() {
        view.showPortfolio(portfolio);

        boolean running = true;
        while (running) {
            view.showPrompt(portfolio);
            running = readLine().map(this::handleInput).orElse(false);
        }
    }

    /**
     * @return whether the session should continue
     */
    private boolean handleInput(String input) {
        return Command.parse(input)
                .map(this::execute)
                .orElseGet(() -> selectAsset(input));
    }

    private boolean execute(Command command) {
        return switch (command) {
            case EXIT -> {
                view.showExit();
                yield false;
            }
            case REBALANCE -> {
                view.showRebalanced(portfolio, portfolio.rebalance());
                yield true;
            }
        };
    }

    private boolean selectAsset(String symbol) {
        Optional<Position> position = portfolio.findPosition(symbol);
        if (position.isEmpty()) {
            view.showUnknownAsset();
            view.showPortfolio(portfolio);
            return true;
        }
        return changeTargetAllocation(position.get());
    }

    /**
     * @return whether the session should continue (false when input closed mid-way)
     */
    private boolean changeTargetAllocation(Position position) {
        view.askForTargetAllocation(position);

        Optional<String> input = readLine();
        input.ifPresent(value -> {
            applyTargetAllocation(position, value);
            view.showPortfolio(portfolio);
        });
        return input.isPresent();
    }

    private void applyTargetAllocation(Position position, String input) {
        if (!PERCENTAGE.matcher(input).matches()) {
            view.showInvalidNumber();
            return;
        }
        try {
            portfolio.updateTargetAllocation(position.asset(), new BigDecimal(input));
            view.showTargetUpdated();
        } catch (AllocationOutOfRangeException e) {
            view.showAllocationOutOfRange();
        } catch (TotalAllocationExceededException e) {
            view.showTotalExceeded(e.getCurrentTotal(), e.getNewTotal());
        }
    }

    /**
     * @return the next trimmed line, or empty (after telling the user) when the input is closed
     */
    private Optional<String> readLine() {
        if (!scanner.hasNextLine()) {
            view.showInputClosed();
            return Optional.empty();
        }
        return Optional.of(scanner.nextLine().trim());
    }
}
