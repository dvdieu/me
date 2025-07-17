package org.example.matching.steps;


import org.example.matching.context.MatchingConfig;
import org.example.matching.context.MatchingContext;

public interface MatchingStep {

    MatchingStep setNext(MatchingStep next);

    void runStep(MatchingContext context, MatchingConfig config);


    static MatchingStep buildChain(MatchingStep ...steps) {
        for (int i = 1; i < steps.length; i++) {
            steps[i - 1].setNext(steps[i]);
        }
        return steps[0];
    }
}
