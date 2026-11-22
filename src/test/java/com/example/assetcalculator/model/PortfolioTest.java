package com.example.assetcalculator.model;

import com.example.assetcalculator.catalog.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTest {

    private static final BigDecimal TOTAL = new BigDecimal("100000");

    private static Position position(Asset asset, String target, String current) {
        return new Position(asset, new BigDecimal(target), new BigDecimal(current));
    }

    private static Portfolio portfolio(Position... positions) {
        return new Portfolio(TOTAL, List.of(positions));
    }

    private static BigDecimal target(Portfolio portfolio, Asset asset) {
        return portfolio.findPosition(asset.getSymbol()).orElseThrow().targetAllocation();
    }

    @Nested
    class Construction {

        @Test
        void rejectsNullArguments() {
            List<Position> positions = List.of(position(Asset.IBM, "100", "100"));

            assertThatNullPointerException().isThrownBy(() -> new Portfolio(null, positions));
            assertThatNullPointerException().isThrownBy(() -> new Portfolio(TOTAL, null));
        }

        @Test
        void rejectsNonPositiveTotalValue() {
            List<Position> positions = List.of(position(Asset.IBM, "100", "100"));

            assertThatIllegalArgumentException().isThrownBy(() -> new Portfolio(BigDecimal.ZERO, positions));
        }

        @Test
        void rejectsEmptyPortfolio() {
            assertThatIllegalArgumentException().isThrownBy(() -> new Portfolio(TOTAL, List.of()));
        }

        @Test
        void rejectsDuplicateAssets() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> portfolio(position(Asset.IBM, "10", "10"), position(Asset.IBM, "10", "10")))
                    .withMessageContaining("IBM");
        }

        @Test
        void rejectsTargetAllocationsAbove100() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> portfolio(position(Asset.IBM, "60", "50"), position(Asset.HD, "40.01", "50")))
                    .withMessageContaining("target");
        }

        @Test
        void rejectsCurrentAllocationsAbove100() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> portfolio(position(Asset.IBM, "50", "60"), position(Asset.HD, "50", "40.01")))
                    .withMessageContaining("current");
        }

        @Test
        void acceptsAllocationsSummingToExactly100() {
            Portfolio portfolio = portfolio(position(Asset.IBM, "60", "50"), position(Asset.HD, "40", "50"));

            assertThat(portfolio.getTotalTargetAllocation()).isEqualByComparingTo("100");
        }

        /**
         * Rules are validated at construction, so later changes to the caller's list must not leak in.
         */
        @Test
        void isNotAffectedByChangesToTheSourceList() {
            List<Position> source = new ArrayList<>(List.of(position(Asset.IBM, "100", "100")));
            Portfolio portfolio = new Portfolio(TOTAL, source);

            source.add(position(Asset.HD, "100", "100"));

            assertThat(portfolio.getPositions()).hasSize(1);
        }

        /**
         * Regression: positions used to be mutable, so a change in one portfolio leaked
         * into another that shared the same object and broke its 100% limit.
         */
        @Test
        void portfoliosSharingAPositionStayIndependent() {
            Position ibm = position(Asset.IBM, "20", "20");
            Portfolio first = portfolio(ibm);
            Portfolio second = portfolio(ibm, position(Asset.HD, "80", "80"));

            first.updateTargetAllocation(Asset.IBM, new BigDecimal("100"));

            assertThat(second.getTotalTargetAllocation()).isEqualByComparingTo("100");
        }
    }

    @Nested
    class FindPosition {

        private final Portfolio portfolio = portfolio(position(Asset.IBM, "50", "50"), position(Asset.HD, "50", "50"));

        @Test
        void findsPositionIgnoringCase() {
            assertThat(portfolio.findPosition("ibm"))
                    .map(Position::asset)
                    .contains(Asset.IBM);
        }

        @Test
        void doesNotFindCatalogAssetThatIsNotInPortfolio() {
            assertThat(portfolio.findPosition("MSFT")).isEmpty();
        }
    }

    @Nested
    class UpdateTargetAllocation {

        private Portfolio portfolio;

        @BeforeEach
        void setUp() {
            portfolio = portfolio(position(Asset.IBM, "20", "10"), position(Asset.MSFT, "80", "90"));
        }

        @Test
        void updatesTargetsWhileTotalStaysAtOrBelow100() {
            portfolio.updateTargetAllocation(Asset.IBM, new BigDecimal("10"));   // total 90
            portfolio.updateTargetAllocation(Asset.MSFT, new BigDecimal("90"));  // total exactly 100

            assertThat(target(portfolio, Asset.IBM)).isEqualByComparingTo("10");
            assertThat(target(portfolio, Asset.MSFT)).isEqualByComparingTo("90");
        }

        /**
         * 100.01 would also exceed the total; the range rule must be reported first.
         * (Range boundaries themselves are covered in PositionTest.)
         */
        @Test
        void reportsOutOfRangeBeforeTotalOverflow() {
            assertThatThrownBy(() -> portfolio.updateTargetAllocation(Asset.IBM, new BigDecimal("100.01")))
                    .isInstanceOf(AllocationOutOfRangeException.class);
            assertThat(target(portfolio, Asset.IBM)).isEqualByComparingTo("20");
        }

        @Test
        void rejectsChangeThatWouldPushTotalAbove100() {
            assertThatThrownBy(() -> portfolio.updateTargetAllocation(Asset.IBM, new BigDecimal("25")))
                    .isInstanceOfSatisfying(TotalAllocationExceededException.class, e -> {
                        assertThat(e.getCurrentTotal()).isEqualByComparingTo("100");
                        assertThat(e.getNewTotal()).isEqualByComparingTo("105");
                    });
            assertThat(target(portfolio, Asset.IBM)).isEqualByComparingTo("20");
        }

        @Test
        void rejectsAssetThatIsNotInPortfolio() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> portfolio.updateTargetAllocation(Asset.HD, BigDecimal.ZERO));
        }
    }

    @Nested
    class Rebalance {

        @Test
        void setsCurrentAllocationToTargetForAllPositions() {
            Portfolio portfolio = portfolio(position(Asset.IBM, "20", "10"), position(Asset.MSFT, "70", "90"));

            portfolio.rebalance();

            assertThat(portfolio.getPositions()).allSatisfy(p ->
                    assertThat(p.currentAllocation()).isEqualByComparingTo(p.targetAllocation()));
        }

        @Test
        void summaryReportsUnallocatedPartOfPortfolio() {
            Portfolio portfolio = portfolio(position(Asset.IBM, "20", "10"), position(Asset.MSFT, "70", "90"));

            RebalanceSummary summary = portfolio.rebalance();

            assertThat(summary.totalTargetAllocation()).isEqualByComparingTo("90");
            assertThat(summary.unallocatedAllocation()).isEqualByComparingTo("10");
            assertThat(summary.unallocatedFunds()).isEqualTo(new BigDecimal("10000.00"));
        }
    }
}
