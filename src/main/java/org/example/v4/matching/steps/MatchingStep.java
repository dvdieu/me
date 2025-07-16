package org.example.v4.matching.steps;


import org.example.v4.matching.context.MatchingContext;

public interface MatchingStep {

    MatchingStep setNext(MatchingStep next);

    void runStep(MatchingContext context);


    static MatchingStep buildChain(MatchingStep ...steps) {
        for (int i = 1; i < steps.length; i++) {
            steps[i - 1].setNext(steps[i]);
        }
        return steps[0];
    }
}
