package com.example.assetcalculator.model;

import java.math.BigDecimal;

/**
 * Outcome of {@link Portfolio#rebalance()}.
 *
 * @param totalTargetAllocation sum of all target allocations (%)
 * @param unallocatedAllocation part of the portfolio left unallocated (%)
 * @param unallocatedFunds      value of the unallocated part
 */
public record RebalanceSummary(BigDecimal totalTargetAllocation,
                               BigDecimal unallocatedAllocation,
                               BigDecimal unallocatedFunds) {
}
