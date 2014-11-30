
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * A very strict heuristic for determining types for CSV fields. This heuristic accepts a type for a field only if
 * <i>every</i> value in the field can be parsed as that type. This heuristic is weak to typos and to "special"
 * values.
 * 
 * @author ajs6f
 */
public class StrictHeuristic extends PerTypeHeuristic<StrictHeuristic> {

    private static final Logger log = getLogger(StrictHeuristic.class);

    @Override
    protected boolean candidacy(final DataType type) {
        final Integer typeCount = typeCounts.get(type);
        log.debug("For type {} found {} occurrences out of {} total values", type, typeCount, totalNumValues());
        return typeCount.equals(totalNumValues());
    }

    @Override
    public StrictHeuristic newInstance() {
        return new StrictHeuristic();
    }

    @Override
    public StrictHeuristic get() {
        return this;
    }
}
