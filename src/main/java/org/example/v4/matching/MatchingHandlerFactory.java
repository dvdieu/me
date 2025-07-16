package org.example.v4.matching;

import org.example.v4.matching.steps.*;

import java.util.Map;

public class MatchingHandlerFactory {

    private static final Map<MatchingStrategy, MatchingHandler> MATCHING_PROCESSOR_MAP = Map.of(
            MatchingStrategy.FIFO, new MatchingHandler(
                    MatchingStep.buildChain(new FIFOResidualStep())
            ),
            MatchingStrategy.PRO_RATA, new MatchingHandler(
                    MatchingStep.buildChain(
                        new FIFOExceptionStep(),
                        new ProRataStep(),
                        new FIFOResidualStep()
                    )
            ),
            MatchingStrategy.LMM, new MatchingHandler(
                    MatchingStep.buildChain(
                            new FIFOExceptionStep(),
                            new LMMStep(),
                            new FIFOResidualStep()
                    )
            )
    );


    public static MatchingHandler getMatchingProcessor(MatchingStrategy strategy) {
        return MATCHING_PROCESSOR_MAP.get(strategy);
    }

}
