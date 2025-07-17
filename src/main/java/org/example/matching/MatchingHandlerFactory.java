package org.example.matching;

import org.example.matching.context.MatchingConfig;
import org.example.matching.steps.*;

import java.util.Map;

public class MatchingHandlerFactory {

    private static final Map<MatchingStrategy, MatchingHandler> MATCHING_PROCESSOR_MAP = Map.of(
            MatchingStrategy.FIFO, new MatchingHandler(
                    MatchingStep.buildChain(new FIFOResidualStep()),
                    matchingConfig()
            ),
            MatchingStrategy.PRO_RATA, new MatchingHandler(
                    MatchingStep.buildChain(
                        new FIFOExceptionStep(),
                        new ProRataStep(),
                        new FIFOResidualStep()
                    ),
                    matchingConfig()
            ),
            MatchingStrategy.LMM, new MatchingHandler(
                    MatchingStep.buildChain(
                            new FIFOExceptionStep(),
                            new LMMStep(),
                            new FIFOResidualStep()
                    ),
                    matchingConfig()
            )
    );


    public static MatchingHandler getMatchingProcessor(MatchingStrategy strategy) {
        return MATCHING_PROCESSOR_MAP.get(strategy);
    }

    private static MatchingConfig matchingConfig() {
        MatchingConfig config = new MatchingConfig();
        config.lmmPerThousands = Map.ofEntries(
                Map.entry(412L, 50),
                Map.entry(413L, 60)
        );

        return config;
    }
}
