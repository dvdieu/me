package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingContext;
import org.example.v4.orderbook.DirectOrder;

import java.util.List;

public class LMMStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context) {

        long remainingSize = context.incoming.remainingQuantity;

        DirectOrder directOrder = context.priceLevel.head;
        while (directOrder != null && context.incoming.remainingQuantity > 0) {
            if (checkSelfMatching(context, directOrder)) {
                if(List.of(412L, 413L).contains(directOrder.userId)) { // TODO: define LMM user
                    long lmmAllocate = 5 * remainingSize / 100; // TODO: adjust percentage
                    long minTradeSize = Math.max(lmmAllocate, 1);
                    long tradeSize = Math.min(Math.min(minTradeSize, context.incoming.remainingQuantity), directOrder.displayedQuantity);

                    context.performMatch(directOrder, tradeSize);
                }
            }

            directOrder = directOrder.prev;
        }
    }

}
