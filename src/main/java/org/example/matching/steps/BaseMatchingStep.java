package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.orderbook.DirectOrder;

/**
 * Abstract base class for a step in the matching strategy chain.
 *
 * <p>This class implements the {@link MatchingStep} interface and provides
 * common logic for chaining and step execution control.</p>
 *
 * <p>Each step determines whether it can allocate liquidity via {@link #canAllocate},
 * performs allocation logic via {@link #performAllocation}, and decides whether
 * to continue to the next step via {@link #shouldContinue}.</p>
 */
public abstract class BaseMatchingStep implements MatchingStep {

    private MatchingStep next;

    /**
     * Sets the next step in the chain.
     *
     * @param next the next {@link MatchingStep}.
     * @return the same next step (for fluent chaining).
     */
    @Override
    public MatchingStep setNext(MatchingStep next) {
        this.next = next;
        return next;
    }

    /**
     * Executes this step’s logic. If the step is eligible for allocation
     * (via {@link #canAllocate}), it performs allocation and, if allowed
     * (via {@link #shouldContinue}), forwards execution to the next step.
     *
     * @param context the matching context.
     * @param config  the matching configuration.
     */
    @Override
    public void runStep(MatchingContext context, MatchingConfig config) {
        if (canAllocate(context, config)) {
            performAllocation(context, config);
        }

        if (next != null && shouldContinue(context, config)) {
            this.next.runStep(context, config);
        }
    }

    /**
     * Performs the core allocation logic for this step.
     *
     * @param context the matching context.
     * @param config  the matching configuration.
     */
    protected abstract void performAllocation(MatchingContext context, MatchingConfig config);


    /**
     * Checks whether this step should attempt allocation.
     * Default: price level is not empty and incoming order still has remaining quantity.
     *
     * @param context the matching context.
     * @param config  the matching configuration.
     * @return true if this step can perform allocation.
     */
    protected boolean canAllocate(MatchingContext context, MatchingConfig config) {
        return !context.priceLevel.isEmpty() && context.incoming.remainingQuantity > 0;
    }


    /**
     * Determines whether to continue to the next step.
     * Default: same condition as {@link #canAllocate}.
     *
     * @param context the matching context.
     * @param config  the matching configuration.
     * @return true if the chain should continue to the next step.
     */
    protected boolean shouldContinue(MatchingContext context, MatchingConfig config) {
        return !context.priceLevel.isEmpty() && context.incoming.remainingQuantity > 0;
    }

    /**
     * Utility method to check and handle self-matching scenarios.
     * If the incoming order would match with itself, the resting order is removed
     * and added to {@code context.selfMatchOrders} instead of being matched.
     *
     * @param context the matching context.
     * @param resting the candidate resting order.
     * @return false if it was a self-match; true otherwise.
     */
    protected boolean checkSelfMatching(MatchingContext context, DirectOrder resting) {
        if(context.incoming.isSelfMatch(resting)) {
            resting.remove();
            context.selfMatchOrders.add(resting);
            return false;
        }

        return true;
    }
}
