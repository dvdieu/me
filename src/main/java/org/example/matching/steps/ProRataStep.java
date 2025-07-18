package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

/**
 * A matching step that performs Pro-Rata allocation across all resting orders
 * at the current price level.
 *
 * <p>Each order receives a proportion of the incoming quantity based on its displayed size
 * relative to the total available liquidity (bucket volume).</p>
 *
 * <p>Only non-self-matching orders are eligible, and a minimum trade size threshold
 * ({@code config.proRataMin}) is enforced to avoid dust trades.</p>
 */
public class ProRataStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {
        long bucketVolume = context.bucketRemaining;
        long remainingSize = context.incoming.remainingQuantity;

        DirectOrder directOrder = context.priceLevel.head;
        while (directOrder != null && context.incoming.remainingQuantity > 0) {
            if (checkSelfMatching(context, directOrder)) {
                long proRataPass = directOrder.displayedQuantity * remainingSize / bucketVolume;
                long tradeSize = Math.min(proRataPass, context.incoming.remainingQuantity);

                if(tradeSize >= config.proRataMin) {
                    context.performMatch(directOrder, tradeSize);
                }
            }

            directOrder = directOrder.prev;
        }
    }

}
