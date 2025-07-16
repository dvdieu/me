package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingContext;
import org.example.v4.orderbook.DirectOrder;

public class ProRataStep extends BaseMatchingStep {

    @Override
    public void performAllocation(MatchingContext context) {
        long bucketVolume = context.bucketRemaining;
        long remainingSize = context.incoming.remainingQuantity;

        DirectOrder directOrder = context.priceLevel.head;
        while (directOrder != null && context.incoming.remainingQuantity > 0) {
            if (checkSelfMatching(context, directOrder)) {
                long proRataPass = directOrder.displayedQuantity * remainingSize / bucketVolume;
                long tradeSize = Math.min(proRataPass, context.incoming.remainingQuantity);

                if(tradeSize >= 1) {
                    context.performMatch(directOrder, tradeSize);
                }
            }

            directOrder = directOrder.prev;
        }
    }

}
