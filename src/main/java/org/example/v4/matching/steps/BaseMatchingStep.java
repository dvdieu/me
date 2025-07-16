package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingContext;
import org.example.v4.orderbook.DirectOrder;

public abstract class BaseMatchingStep implements MatchingStep {

    private MatchingStep next;


    @Override
    public MatchingStep setNext(MatchingStep next) {
        this.next = next;
        return next;
    }

    @Override
    public void runStep(MatchingContext context) {
        if (canAllocate(context)) {
            performAllocation(context);
        }

        if (next != null && shouldContinue(context)) {
            this.next.runStep(context);
        }
    }


    protected abstract void performAllocation(MatchingContext context);


    protected boolean canAllocate(MatchingContext context) {
        return !context.priceLevel.isEmpty() && context.incoming.remainingQuantity > 0;
    }

    protected boolean shouldContinue(MatchingContext context) {
        return !context.priceLevel.isEmpty() && context.incoming.remainingQuantity > 0;
    }

    protected boolean checkSelfMatching(MatchingContext context, DirectOrder resting) {
        if(context.incoming.isSelfMatch(resting)) {
            resting.remove();
            context.selfMatchOrders.add(resting);
            return false;
        }

        return true;
    }
}
