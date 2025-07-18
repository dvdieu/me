package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

/**
 * A matching step that performs standard FIFO allocation for all available resting orders
 * at the current price level, consuming as much of the incoming order as possible.
 *
 * <p>This step iterates through the orders in FIFO order (from head to tail)
 * and matches the incoming order with each resting order until it is fully filled
 * or no more liquidity remains.</p>
 *
 * <p>Self-matching orders are detected and skipped via {@link #checkSelfMatching}.</p>
 */
public class FIFOResidualStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {

        DirectOrder directOrder = context.priceLevel.head;
        while (directOrder != null && context.incoming.remainingQuantity > 0) {
            if(checkSelfMatching(context, directOrder)) {
                long tradeSize = Math.min(directOrder.displayedQuantity, context.incoming.remainingQuantity);
                context.performMatch(directOrder, tradeSize);
            }

            directOrder = directOrder.prev;
        }
    }

}
