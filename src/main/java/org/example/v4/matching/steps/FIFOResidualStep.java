package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingConfig;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;
import org.example.v4.orderbook.DirectOrder;

import java.util.Iterator;

public class FIFOResidualStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context, MatchingConfig config) {

        DirectOrder directOrder = context.priceLevel.head;
        while (directOrder != null && context.incoming.remainingQuantity > 0) {
            if(checkSelfMatching(context, directOrder)) {
                long tradeSize = Math.min(directOrder.order.displayedQuantity, context.incoming.remainingQuantity);
                context.performMatch(directOrder, tradeSize);
            }

            directOrder = directOrder.prev;
        }
    }

}
