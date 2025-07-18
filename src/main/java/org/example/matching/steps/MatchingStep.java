package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;

/**
 * Represents a single step in the matching pipeline.
 *
 * <p>Implementations of this interface define custom matching logic,
 * which can be chained together to form a configurable matching strategy.</p>
 *
 * <p>This interface follows the Chain of Responsibility pattern,
 * allowing each step to pass control to the next via {@code setNext(...)}.</p>
 */
public interface MatchingStep {

    /**
     * Sets the next step in the matching chain.
     *
     * @param next the next {@link MatchingStep}.
     * @return the next step, allowing for fluent chaining.
     */
    MatchingStep setNext(MatchingStep next);

    /**
     * Executes this step's logic on the given matching context.
     *
     * @param context the matching context containing current state of the match.
     * @param config  the configuration used to guide matching logic.
     */
    void runStep(MatchingContext context, MatchingConfig config);

    /**
     * Utility method to build a chain of {@link MatchingStep} instances.
     *
     * @param steps the steps to link together in order.
     * @return the head of the constructed chain.
     */
    static MatchingStep buildChain(MatchingStep ...steps) {
        for (int i = 1; i < steps.length; i++) {
            steps[i - 1].setNext(steps[i]);
        }
        return steps[0];
    }
}
