
package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import java.util.Map;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;
import com.google.common.collect.Range;

/**
 * Determines ranges in different types for the supplied values.
 * 
 * @author ajs6f
 */
public interface RangeDeterminingHeuristic<SelfType extends RangeDeterminingHeuristic<SelfType>> extends
        Heuristic<SelfType> {

    /**
     * @return The ranges taken on by all values in the Java value space associated to each type.
     */
    Map<DataType, Range<?>> getRanges();
}
