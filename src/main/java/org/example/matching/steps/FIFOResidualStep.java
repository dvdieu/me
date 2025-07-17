package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

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
