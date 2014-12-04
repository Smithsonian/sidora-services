
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import java.util.SortedSet;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public abstract class PerTypeHeuristicTestFrame<TestHeuristic extends PerTypeHeuristic<TestHeuristic>> extends
        CountAggregatingHeuristicTestFrame<TestHeuristic, SortedSet<DataType>> {
    // TODO add appropriate tests to this level of test hierarchy
}
