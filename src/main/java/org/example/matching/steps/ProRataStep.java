package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

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
