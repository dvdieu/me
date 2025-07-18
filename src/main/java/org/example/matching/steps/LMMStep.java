package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

/**
 * A matching step that performs allocation based on LMM (Liquidity Market Maker) quotas.
 *
 * <p>This step allocates liquidity to eligible LMM participants based on their configured
 * weight (per-thousand percentage) and a minimum fill threshold.</p>
 *
 * <p>Only orders from LMM participants (defined in {@code config.lmmPerThousands}) are eligible.
 * Allocation is capped by a total LMM quota calculated as:
 * {@code totalLmmPerThousand * incoming.remainingQuantity / 1000}.</p>
 *
 * <p>Self-matching orders are excluded.</p>
 */
public class LMMStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {

        long remainingSize = context.incoming.remainingQuantity;
        long remainingLmmQuota = config.totalLmmPerThousand * remainingSize / 1000;

        DirectOrder directOrder = context.priceLevel.head;
        while (directOrder != null && remainingLmmQuota > 0) {
            if (checkSelfMatching(context, directOrder)) {
                int lmmPercentage = config.lmmPerThousands.getOrDefault(directOrder.userId, 0);
                if(lmmPercentage != 0) {
                    long lmmAllocate = lmmPercentage * remainingSize / 1000;
                    long minTradeSize = Math.max(lmmAllocate, config.lmmMinFill);
                    long tradeSize = Math.min(Math.min(minTradeSize, remainingLmmQuota), directOrder.displayedQuantity);

                    remainingLmmQuota -= tradeSize;
                    context.performMatch(directOrder, tradeSize);
                }
            }

            directOrder = directOrder.prev;
        }
    }

}
