package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingConfig;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;

import java.util.Iterator;

public class FIFOResidualStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {

        Iterator<Order> iterator = context.priceLevel.orders.iterator();
        while (iterator.hasNext() && context.incoming.remainingQuantity > 0) {
            Order resting = iterator.next();
            if(checkSelfMatching(context, resting, iterator)) {
                continue;
            }

            long tradeSize = Math.min(resting.displayedQuantity, context.incoming.remainingQuantity);
            context.performMatch(iterator, resting, tradeSize);
        }
    }

}
