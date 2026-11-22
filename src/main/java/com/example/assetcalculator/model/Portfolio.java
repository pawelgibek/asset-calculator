package com.example.assetcalculator.model;

import com.example.assetcalculator.catalog.Asset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Aggregate that owns all portfolio rules:
 * <ul>
 *     <li>each allocation is within 0–100% (enforced by {@link Position}),</li>
 *     <li>target and current allocations each sum to at most 100%,</li>
 *     <li>every asset appears at most once.</li>
 * </ul>
 * Positions are immutable and only this class replaces them, so the rules always hold.
 * Not thread-safe; intended for a single interactive session.
 */
public class Portfolio {

    private final BigDecimal totalValue;
    private final List<Position> positions;

    public Portfolio(BigDecimal totalValue, List<Position> positions) {
        Objects.requireNonNull(totalValue, "totalValue must not be null");
        Objects.requireNonNull(positions, "positions must not be null");
        requirePositive(totalValue);
        requireNotEmpty(positions);
        requireNoDuplicates(positions);

        this.totalValue = totalValue;
        this.positions = new ArrayList<>(positions);

        requireTotalWithinLimit("target", sum(Position::targetAllocation));
        requireTotalWithinLimit("current", sum(Position::currentAllocation));
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    /**
     * @return snapshot of the positions; later changes to the portfolio are not reflected
     */
    public List<Position> getPositions() {
        return List.copyOf(positions);
    }

    /**
     * Case-insensitive lookup by ticker symbol among the positions in this portfolio.
     */
    public Optional<Position> findPosition(String symbol) {
        return positions.stream()
                .filter(p -> p.symbol().equalsIgnoreCase(symbol))
                .findFirst();
    }

    public BigDecimal getTotalTargetAllocation() {
        return sum(Position::targetAllocation);
    }

    /**
     * @throws AllocationOutOfRangeException    if the new target is outside 0–100%
     * @throws TotalAllocationExceededException if the total target would exceed 100%
     * @throws IllegalArgumentException         if the asset is not part of this portfolio
     */
    public void updateTargetAllocation(Asset asset, BigDecimal newTarget) {
        int index = indexOf(asset);
        Position current = positions.get(index);
        // built first, so an out-of-range value is reported before a total overflow
        Position updated = current.withTargetAllocation(newTarget);

        BigDecimal currentTotal = getTotalTargetAllocation();
        BigDecimal newTotal = currentTotal
                .subtract(current.targetAllocation())
                .add(updated.targetAllocation());

        if (Percentages.exceedsHundred(newTotal)) {
            throw new TotalAllocationExceededException(currentTotal, newTotal);
        }

        positions.set(index, updated);
    }

    /**
     * Moves every current allocation to its target. Any part of the portfolio not
     * covered by targets is reported as unallocated.
     */
    public RebalanceSummary rebalance() {
        positions.replaceAll(Position::rebalanced);

        BigDecimal totalTarget = getTotalTargetAllocation();
        BigDecimal unallocated = Percentages.HUNDRED.subtract(totalTarget);

        return new RebalanceSummary(totalTarget, unallocated, Percentages.of(totalValue, unallocated));
    }

    private int indexOf(Asset asset) {
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i).asset() == asset) {
                return i;
            }
        }
        throw new IllegalArgumentException(asset + " is not part of the portfolio");
    }

    private BigDecimal sum(Function<Position, BigDecimal> allocation) {
        return positions.stream()
                .map(allocation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void requirePositive(BigDecimal totalValue) {
        if (totalValue.signum() <= 0) {
            throw new IllegalArgumentException("totalValue must be positive but was " + totalValue);
        }
    }

    private static void requireNotEmpty(List<Position> positions) {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("portfolio must contain at least one position");
        }
    }

    private static void requireNoDuplicates(List<Position> positions) {
        Set<Asset> seen = new HashSet<>();
        for (Position position : positions) {
            if (!seen.add(position.asset())) {
                throw new IllegalArgumentException("duplicate asset: " + position.symbol());
            }
        }
    }

    private static void requireTotalWithinLimit(String kind, BigDecimal total) {
        if (Percentages.exceedsHundred(total)) {
            throw new IllegalArgumentException(
                    "total " + kind + " allocation must not exceed 100% but was " + total + "%");
        }
    }
}
