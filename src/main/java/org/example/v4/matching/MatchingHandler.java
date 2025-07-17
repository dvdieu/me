package org.example.v4.matching;


import org.example.v4.matching.context.MatchingConfig;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.matching.steps.MatchingStep;

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
