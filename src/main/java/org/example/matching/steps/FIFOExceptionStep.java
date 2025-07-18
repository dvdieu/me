package org.example.matching.steps;

import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;

/**
 * A matching step that performs FIFO allocation only when
 * the incoming order can fully consume the remaining liquidity
 * at the current price level.
 *
 * <p>This step is typically used as an "exception" or override
 * in priority-based matching strategies, ensuring that full fills
 * are required before allocation is allowed.</p>
 *
 * <p>It extends {@link FIFOResidualStep} but adds a stricter allocation condition:
 * {@code incoming.remainingQuantity >= bucketRemaining}.</p>
 */
public class FIFOExceptionStep extends FIFOResidualStep {


    @Override
    protected boolean canAllocate(MatchingContext context, MatchingConfig config) {
        return super.canAllocate(context, config) && context.incoming.remainingQuantity >= context.bucketRemaining;
    }

}
