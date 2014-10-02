
package com.asoroka.sidora.statistics.heuristics;

import com.asoroka.sidora.datatype.DataType;

/**
 * A very strict heuristic for determining types for CSV fields. This heuristic accepts types only if <i>every</i>
 * value in the field can be parsed as that type. This heuristic is weak to typos and to special values.
 * 
 * @author ajs6f
 */
public class StrictHeuristic extends CountAggregatingHeuristic<StrictHeuristic> {

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.CountAggregatingHeuristic#candidacy(java.lang.Float)
     */
    @Override
    protected boolean candidacy(final DataType type) {
        return typeCounts.get(type).equals(totalNumValues());
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.CountAggregatingHeuristic#clone()
     */
    @Override
    public StrictHeuristic clone() {
        return new StrictHeuristic();
    }
}
