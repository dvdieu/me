package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingConfig;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;

import java.util.Iterator;

public class ProRataStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {
        long bucketVolume = context.bucketRemaining;
        long remainingSize = context.incoming.remainingQuantity;
        Iterator<Order> iterator = context.priceLevel.orders.iterator();

        while (iterator.hasNext() && context.incoming.remainingQuantity > 0) {
            Order resting = iterator.next();
            if (checkSelfMatching(context, resting, iterator)) {
                continue;
            }

            long proRataPass = resting.displayedQuantity * remainingSize / bucketVolume;
            long tradeSize = Math.min(proRataPass, context.incoming.remainingQuantity);

            if(tradeSize >= config.proRataMin) {
                context.performMatch(iterator, resting, tradeSize);
            }
        }
    }

}
