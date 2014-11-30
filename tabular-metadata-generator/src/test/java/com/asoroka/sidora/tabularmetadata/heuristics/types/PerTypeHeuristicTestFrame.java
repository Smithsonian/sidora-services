
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

public abstract class PerTypeHeuristicTestFrame<TestHeuristic extends PerTypeHeuristic<TestHeuristic>> extends
        CountAggregatingHeuristicTestFrame<TestHeuristic> {

    private static final Logger log = getLogger(PerTypeHeuristicTestFrame.class);
}
