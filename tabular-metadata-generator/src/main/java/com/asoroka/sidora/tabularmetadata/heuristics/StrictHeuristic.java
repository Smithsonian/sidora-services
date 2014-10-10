
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * A very strict heuristic for determining types for CSV fields. This heuristic accepts types only if <i>every</i>
 * value in the field can be parsed as that type. This heuristic is weak to typos and to special values.
 * 
 * @author ajs6f
 */
public class StrictHeuristic extends CountAggregatingHeuristic<StrictHeuristic> {

    private static final Logger log = getLogger(StrictHeuristic.class);

    @Override
    protected boolean candidacy(final DataType type) {
        log.debug("For type {} found {} occurences out of {} total values", type, typeCounts.get(type),
                totalNumValues());
        return typeCounts.get(type).equals(totalNumValues());
    }

    @Override
    public StrictHeuristic clone() {
        return new StrictHeuristic();
    }
}
