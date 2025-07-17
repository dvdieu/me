package org.example.matching.steps;

import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;

public class FIFOExceptionStep extends FIFOResidualStep {


    @Override
    protected boolean canAllocate(MatchingContext context, MatchingConfig config) {
        return super.canAllocate(context, config) && context.incoming.remainingQuantity >= context.bucketRemaining;
    }

}
