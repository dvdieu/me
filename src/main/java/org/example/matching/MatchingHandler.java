package org.example.matching;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.matching.steps.MatchingStep;

/**
 * Entry point for executing the matching strategy pipeline.
 *
 * <p>{@code MatchingHandler} coordinates the matching process using a configured
 * chain of {@link MatchingStep}s and a shared {@link MatchingConfig}.</p>
 *
 * <p>This class decouples the matching engine from specific allocation strategies,
 * allowing strategies to be configured and injected at runtime.</p>
 */
public class MatchingHandler {

    private final MatchingStep matchingStep;
    private final MatchingConfig config;

    public MatchingHandler(MatchingStep matchingStep, MatchingConfig config) {
        this.matchingStep = matchingStep;
        this.config = config;
    }


    public void tryMatchInstantly(MatchingContext context) {
        matchingStep.runStep(context, config);
    }

}
