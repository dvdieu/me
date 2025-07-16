package org.example.v4.matching;


import org.example.v4.matching.context.MatchingContext;
import org.example.v4.matching.steps.MatchingStep;

public class MatchingHandler {

    private final MatchingStep matchingStep;

    public MatchingHandler(MatchingStep matchingStep) {
        this.matchingStep = matchingStep;
    }


    public void tryMatchInstantly(MatchingContext context) {
        matchingStep.runStep(context);
    }

}
