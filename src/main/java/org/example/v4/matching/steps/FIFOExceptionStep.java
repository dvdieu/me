package org.example.v4.matching.steps;

import org.example.v4.matching.context.MatchingContext;

public class FIFOExceptionStep extends FIFOResidualStep {


    @Override
    protected boolean canAllocate(MatchingContext context) {
        return super.canAllocate(context) && context.incoming.remainingQuantity >= context.bucketRemaining;
    }

}
