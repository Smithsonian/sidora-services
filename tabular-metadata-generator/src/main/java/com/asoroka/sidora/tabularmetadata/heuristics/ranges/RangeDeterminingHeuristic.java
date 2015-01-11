
package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import java.util.Map;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;
import com.google.common.collect.Range;

/**
 * Determines ranges in different {@link DataType}s for the supplied values.
 * 
 * @author A. Soroka
 */
public interface RangeDeterminingHeuristic<SelfType extends RangeDeterminingHeuristic<SelfType>> extends
        Heuristic<SelfType, Map<DataType, Range<?>>> {
    // EMPTY
}
