package com.example.assetcalculator.console;

import com.example.assetcalculator.model.Portfolio;
import com.example.assetcalculator.model.Position;
import com.example.assetcalculator.model.RebalanceSummary;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Everything the user sees: messages, prompts and the portfolio table.
 */
class ConsoleView {

    private static final String CURRENCY = "USD";
    private static final String ROW_FORMAT = "%-10s %-12s %-12s %-12s %-12s %-12s %-18s %-25s%n";
    private static final String TABLE_SEPARATOR = "-".repeat(144);
    private static final String SUMMARY_SEPARATOR = "-".repeat(40);

    private final PrintStream out;

    ConsoleView(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    void showPrompt(Portfolio portfolio) {
        String symbols = portfolio.getPositions().stream()
                .map(Position::symbol)
                .collect(Collectors.joining(", "));

        out.println("\nAvailable assets: " + symbols);
        out.println("Commands: " + Arrays.stream(Command.values())
                .map(Command::name)
                .collect(Collectors.joining(" | ")));
        out.print("Select an asset or enter a command: ");
    }

    void askForTargetAllocation(Position position) {
        out.println("Enter new Target % for " + position.symbol() + ":");
    }

    void showExit() {
        out.println("Exiting Asset Calculator...");
    }

    void showInputClosed() {
        out.println("\nInput closed. Exiting Asset Calculator...");
    }

    void showUnknownAsset() {
        out.println("Unknown asset. Please try again.");
    }

    void showInvalidNumber() {
        out.println("\nInvalid number. Please enter a percentage with up to 2 decimal places, e.g. 12.5");
    }

    void showTargetUpdated() {
        out.println("\nTarget Allocation updated successfully!");
    }

    void showAllocationOutOfRange() {
        out.println("\nUPDATE REJECTED!");
        out.println("Target Allocation must be between 0 and 100%.");
    }

    void showTotalExceeded(BigDecimal currentTotal, BigDecimal newTotal) {
        out.println("\nUPDATE REJECTED!");
        out.println("Total Target Allocation cannot exceed 100%.");
        out.println("Current total: " + percent(currentTotal));
        out.println("After this change: " + percent(newTotal));
        out.println("Please reduce another asset's Target Allocation first.");
    }

    void showRebalanced(Portfolio portfolio, RebalanceSummary summary) {
        out.println("\nPortfolio rebalanced successfully!");
        showPortfolio(portfolio);

        out.println("\nPORTFOLIO SUMMARY");
        out.println(SUMMARY_SEPARATOR);
        out.println("Total Target Allocation: " + percent(summary.totalTargetAllocation()));
        out.println("Unallocated Allocation: " + percent(summary.unallocatedAllocation()));
        out.println("Unallocated Funds: " + summary.unallocatedFunds() + " " + CURRENCY);
    }

    void showPortfolio(Portfolio portfolio) {
        BigDecimal totalValue = portfolio.getTotalValue();

        out.println("\nPORTFOLIO");
        out.println("Total Portfolio Value: " + totalValue + " " + CURRENCY);
        out.printf(ROW_FORMAT, "Security", "Target %", "Current %", "Variance",
                "Unit Price", "Shares", "Invested Amount", "Shares to Buy / Sell");
        out.println(TABLE_SEPARATOR);

        for (Position position : portfolio.getPositions()) {
            out.printf(ROW_FORMAT,
                    position.symbol(),
                    percent(position.targetAllocation()),
                    percent(position.currentAllocation()),
                    signedPercent(position.variance()),
                    position.unitPrice(),
                    position.shares(totalValue),
                    position.investedAmount(totalValue),
                    tradeAction(position.sharesToTrade(totalValue)));
        }

        out.println(TABLE_SEPARATOR);
    }

    private static String tradeAction(BigDecimal sharesToTrade) {
        return switch (sharesToTrade.signum()) {
            case 1 -> "BUY " + sharesToTrade + " shares";
            case -1 -> "SELL " + sharesToTrade.abs() + " shares";
            default -> sharesToTrade + " shares";
        };
    }

    private static String signedPercent(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + percent(value);
    }

    /**
     * Formats a percentage without trailing zeros or scientific notation, e.g. 7.50 -> "7.5%", 0.0 -> "0%".
     */
    private static String percent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
