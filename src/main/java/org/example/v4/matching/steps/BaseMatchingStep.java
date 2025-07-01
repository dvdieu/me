package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingConfig;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;
import org.example.v4.orderbook.DirectOrder;

import java.util.Iterator;

public abstract class BaseMatchingStep implements MatchingStep {

    private MatchingStep next;


    @Override
    public MatchingStep setNext(MatchingStep next) {
        this.next = next;
        return next;
    }

    @Override
    public void runStep(MatchingContext context, MatchingConfig config) {
        if (canAllocate(context, config)) {
            performAllocation(context, config);
        }

        if (next != null && shouldContinue(context, config)) {
            this.next.runStep(context, config);
        }
    }


    protected abstract void performAllocation(MatchingContext context, MatchingConfig config);


    protected boolean canAllocate(MatchingContext context, MatchingConfig config) {
        return !context.priceLevel.isEmpty() && context.incoming.remainingQuantity > 0;
    }

    protected boolean shouldContinue(MatchingContext context, MatchingConfig config) {
        return !context.priceLevel.isEmpty() && context.incoming.remainingQuantity > 0;
    }

    protected boolean checkSelfMatching(MatchingContext context, DirectOrder resting) {
        if(context.incoming.isSelfMatch(resting.order)) {
            resting.remove();
            context.selfMatchOrders.add(resting.order);
            return false;
        }

        return true;
    }
}
