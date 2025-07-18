package org.example.matching;

import org.example.matching.context.MatchingConfig;
import org.example.matching.steps.*;

import java.util.Map;

/**
 * Factory class for creating and providing {@link MatchingHandler} instances
 * based on predefined {@link MatchingStrategy} configurations.
 *
 * <p>Each strategy maps to a distinct chain of {@link MatchingStep}s and a shared
 * {@link MatchingConfig}, enabling flexible and pluggable matching logic.</p>
 */
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
