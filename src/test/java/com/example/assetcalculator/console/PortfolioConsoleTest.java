package com.example.assetcalculator.console;

import com.example.assetcalculator.catalog.Asset;
import com.example.assetcalculator.model.Portfolio;
import com.example.assetcalculator.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Drives {@link PortfolioConsole} through in-memory input/output streams.
 * Business rules (ranges, totals, rebalancing) are covered by PortfolioTest;
 * here we only check input parsing and what the user sees.
 * The timeout runs in a separate thread so even a busy infinite loop fails the test.
 */
@Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class PortfolioConsoleTest {

    private Portfolio portfolio;

    /**
     * Own fixture, independent of PortfolioConfiguration, so changing the initial
     * portfolio does not break console tests.
     */
    @BeforeEach
    void setUp() {
        portfolio = portfolio(
                position(Asset.IBM, "20", "10"),
                position(Asset.MSFT, "20", "20"),
                position(Asset.ORCL, "20", "30"),
                position(Asset.AAPL, "20", "20"),
                position(Asset.HD, "20", "20"));
    }

    @Test
    void constructorRejectsNullArguments() {
        InputStream in = InputStream.nullInputStream();
        PrintStream out = new PrintStream(OutputStream.nullOutputStream());

        assertThatNullPointerException().isThrownBy(() -> new PortfolioConsole(null, in, out));
        assertThatNullPointerException().isThrownBy(() -> new PortfolioConsole(portfolio, null, out));
        assertThatNullPointerException().isThrownBy(() -> new PortfolioConsole(portfolio, in, null));
    }

    @Nested
    class Commands {

        @Test
        void exitTerminatesConsoleIgnoringCaseAndWhitespace() {
            String output = run("  Exit  \n");

            assertThat(output)
                    .contains("Exiting Asset Calculator...")
                    .doesNotContain("Input closed");
        }

        @Test
        void promptListsAssetsFromPortfolio() {
            portfolio = portfolio(position(Asset.IBM, "50", "50"), position(Asset.HD, "50", "50"));

            assertThat(run("EXIT\n")).contains("Available assets: IBM, HD");
        }

        @Test
        void unknownAssetIsReported() {
            assertThat(run("XYZ\nEXIT\n")).contains("Unknown asset. Please try again.");
        }

        @Test
        void rebalancePrintsSummary() {
            String output = run("IBM\n10\nrebalance\nEXIT\n");

            assertThat(output)
                    .contains("Portfolio rebalanced successfully!")
                    .contains("Total Target Allocation: 90%")
                    .contains("Unallocated Allocation: 10%")
                    .contains("Unallocated Funds: 10000.00 USD");
        }
    }

    @Nested
    class EndOfInput {

        @Test
        void closedInputAtMainPromptTerminatesGracefully() {
            assertThat(run("")).contains("Input closed. Exiting Asset Calculator...");
        }

        @Test
        void closedInputWhileAskingForTargetTerminatesWithoutChanges() {
            String output = run("IBM\n");

            assertThat(output)
                    .contains("Enter new Target % for IBM:")
                    .contains("Input closed. Exiting Asset Calculator...");
            assertThat(target(Asset.IBM)).isEqualByComparingTo("20");
        }
    }

    @Nested
    class TargetAllocationInput {

        @ParameterizedTest
        @ValueSource(strings = {"10", "12.25"})
        void acceptsPlainNumbersWithUpToTwoDecimalPlaces(String value) {
            String output = run("IBM\n" + value + "\nEXIT\n");

            assertThat(output).contains("Target Allocation updated successfully!");
            assertThat(target(Asset.IBM)).isEqualByComparingTo(value);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1,5", "1e1", "+5", "12.345"})
        void rejectsMalformedNumbers(String value) {
            String output = run("IBM\n" + value + "\nEXIT\n");

            assertThat(output).contains("Invalid number. Please enter a percentage with up to 2 decimal places");
            assertThat(target(Asset.IBM)).isEqualByComparingTo("20");
        }

        /**
         * A minus sign passes parsing, so the user gets the range message instead of "invalid number".
         */
        @Test
        void reportsValueOutsideRange() {
            String output = run("IBM\n-0.01\nEXIT\n");

            assertThat(output)
                    .contains("UPDATE REJECTED!")
                    .contains("Target Allocation must be between 0 and 100%.");
        }

        @Test
        void reportsTotalAbove100WithBothTotals() {
            String output = run("IBM\n25\nEXIT\n");

            assertThat(output)
                    .contains("UPDATE REJECTED!")
                    .contains("Total Target Allocation cannot exceed 100%.")
                    .contains("Current total: 100%")
                    .contains("After this change: 105%");
        }
    }

    @Nested
    class PortfolioTable {

        private static final int TARGET = 1;
        private static final int VARIANCE = 3;
        private static final int TRADE = 7;

        @Test
        void showsAllColumnsAndBuyForUnderweightAsset() {
            // IBM: target 20, current 10 -> +10% of 100000 / 150 = 66.67
            // (the calculations are verified in PositionTest; this checks the row layout)
            assertThat(columns(run("EXIT\n"), "IBM"))
                    .containsExactly("IBM", "20%", "10%", "+10%", "150", "66.67", "10000.00", "BUY 66.67 shares");
        }

        @Test
        void showsSellForOverweightAsset() {
            List<String> orcl = columns(run("EXIT\n"), "ORCL");

            assertThat(orcl.get(VARIANCE)).isEqualTo("-10%");
            assertThat(orcl.get(TRADE)).isEqualTo("SELL 45.45 shares");
        }

        @Test
        void showsNoTradeForAssetOnTarget() {
            List<String> msft = columns(run("EXIT\n"), "MSFT");

            assertThat(msft.get(VARIANCE)).isEqualTo("0%");
            assertThat(msft.get(TRADE)).isEqualTo("0.00 shares");
        }

        /**
         * "100%" in other tests guards against scientific notation (100 stripped is 1E+2).
         */
        @Test
        void showsPercentagesWithoutTrailingZeros() {
            List<String> ibm = columns(run("IBM\n12.50\nEXIT\n"), "IBM", 1);

            assertThat(ibm.get(TARGET)).isEqualTo("12.5%");
            assertThat(ibm.get(VARIANCE)).isEqualTo("+2.5%");
        }

        /**
         * Splits a table row into its cells. Columns are padded with at least two
         * spaces for these values, while a cell itself contains at most single spaces.
         */
        private static List<String> columns(String output, String security) {
            return columns(output, security, 0);
        }

        /**
         * @param table which printed table to read (0 = shown on start, 1 = after the first action, ...)
         */
        private static List<String> columns(String output, String security, int table) {
            String row = output.lines()
                    .filter(line -> line.startsWith(security + " "))
                    .skip(table)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No table row for " + security));
            return List.of(row.strip().split("\\s{2,}"));
        }
    }

    private static Position position(Asset asset, String target, String current) {
        return new Position(asset, new BigDecimal(target), new BigDecimal(current));
    }

    private static Portfolio portfolio(Position... positions) {
        return new Portfolio(new BigDecimal("100000"), List.of(positions));
    }

    private String run(String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        new PortfolioConsole(portfolio, in, out).start();

        return buffer.toString(StandardCharsets.UTF_8);
    }

    private BigDecimal target(Asset asset) {
        return portfolio.findPosition(asset.getSymbol()).orElseThrow().targetAllocation();
    }
}
