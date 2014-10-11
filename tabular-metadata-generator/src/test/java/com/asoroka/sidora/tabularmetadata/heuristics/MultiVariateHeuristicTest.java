
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

@Ignore
public class MultiVariateHeuristicTest extends RunningMinMaxHeuristicTestFrame<MultiVariateHeuristic> {

    private static final Logger log = getLogger(MultiVariateHeuristicTest.class);

    @Override
    protected MultiVariateHeuristic newTestHeuristic() {
        return new MultiVariateHeuristic(0.1F);
    }

    @Test
    public void showTypeLikelihoods() {
        final MultiVariateHeuristic testHeuristic = newTestHeuristic();
        log.trace("Naive type likelihoods: \n{}", testHeuristic.typeLikelihoods);
    }
}
