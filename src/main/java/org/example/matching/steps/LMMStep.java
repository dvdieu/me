package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

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
