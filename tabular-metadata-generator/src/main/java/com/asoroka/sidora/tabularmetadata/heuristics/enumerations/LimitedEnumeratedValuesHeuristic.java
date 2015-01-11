
package com.asoroka.sidora.tabularmetadata.heuristics.enumerations;

import java.util.Map;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.ValueCountingHeuristic;

/**
 * A {@link Heuristic} that accepts only a limited number of values before ceasing operation.
 * 
 * @author A. Soroka
 */
public class LimitedEnumeratedValuesHeuristic extends
        ValueCountingHeuristic<LimitedEnumeratedValuesHeuristic, Map<DataType, Set<String>>>
        implements EnumeratedValuesHeuristic<LimitedEnumeratedValuesHeuristic> {

    /**
     * Default to recording 10 values.
     */
    public static final int DEFAULT_LIMIT = 10;

    private int limit = DEFAULT_LIMIT;

    private final EnumeratedValuesHeuristic<?> wrappedStrategy;

    public LimitedEnumeratedValuesHeuristic() {
        super();
        this.wrappedStrategy = new InMemoryEnumeratedValuesHeuristic();
    }

    /**
     * @param limit
     * @param wrappedStrategy
     */
    public LimitedEnumeratedValuesHeuristic(final int limit, final EnumeratedValuesHeuristic<?> wrappedStrategy) {
        super();
        this.limit = limit;
        this.wrappedStrategy = wrappedStrategy;
    }

    @Override
    public boolean addValue(final String lex) {
        return super.addValue(lex) && valuesSeen() <= limit ? wrappedStrategy.addValue(lex) : false;
    }

    @Override
    public Map<DataType, Set<String>> results() {
        return wrappedStrategy.results();
    }

    @Override
    public LimitedEnumeratedValuesHeuristic get() {
        return new LimitedEnumeratedValuesHeuristic(limit, wrappedStrategy.get());
    }
}
