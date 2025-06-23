package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingConfig;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;

import java.util.Iterator;

public class LMMStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {

        long remainingSize = context.incoming.remainingQuantity;
        long remainingLmmQuota = config.totalLmmPerThousand * remainingSize / 1000;

        Iterator<Order> iterator = context.priceLevel.orders.iterator();

        while (iterator.hasNext() && context.incoming.remainingQuantity > 0 && remainingLmmQuota > 0) {
            Order resting = iterator.next();
            if (checkSelfMatching(context, resting, iterator)) {
                continue;
            }

            int lmmPercentage = config.lmmPerThousands.getOrDefault(resting.userId, 0);
            if(lmmPercentage != 0) {
                long lmmAllocate = lmmPercentage * remainingSize / 1000;
                long minTradeSize = Math.max(lmmAllocate, config.lmmMinFill);
                long tradeSize = Math.min(Math.min(minTradeSize, remainingLmmQuota), context.incoming.remainingQuantity);

                remainingLmmQuota -= tradeSize;
                context.performMatch(iterator, resting, tradeSize);
            }
        }
    }

}
