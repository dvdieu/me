package org.example.matching;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;
import org.example.matching.steps.MatchingStep;

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
