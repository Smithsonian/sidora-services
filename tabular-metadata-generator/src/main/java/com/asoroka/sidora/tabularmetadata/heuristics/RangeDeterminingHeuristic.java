
package com.asoroka.sidora.tabularmetadata.heuristics;

import java.util.Map;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * Determines ranges in different types for the supplied values.
 * 
 * @author ajs6f
 */
public interface RangeDeterminingHeuristic extends Heuristic {

    /**
     * @return The range taken on by proffered values in the Java value space associated to the most likely type.
     *         Under any normal regime, this should be equal to {@code getRanges.get(mostLikelyType())}.
     */
    public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange();

    /**
     * @return The ranges taken on by all values in the Java value space associated to each type.
     */
    public Map<DataType, Range<?>> getRanges();
}
