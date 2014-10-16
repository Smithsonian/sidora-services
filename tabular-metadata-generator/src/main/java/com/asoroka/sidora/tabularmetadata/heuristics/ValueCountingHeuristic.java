
package com.asoroka.sidora.tabularmetadata.heuristics;

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * A {@link ValueHeuristic} that counts the number of values received.
 * 
 * @author ajs6f
 */
public abstract class ValueCountingHeuristic<T extends ValueCountingHeuristic<T>> extends
        AbstractDataTypeHeuristic<T> {

    private int totalNumValues;

    /**
     * Set the counter of values seen to zero.
     * 
     * @author ajs6f
     */
    @Override
    public void reset() {
        this.totalNumValues = 0;
    }

    private static final Logger log = getLogger(ValueCountingHeuristic.class);

    @Override
    public void addValue(final String value) {
        log.trace("Received new value: {}", value);
        totalNumValues++;
    }

    protected int totalNumValues() {
        return totalNumValues;
    }

    @Override
    public T get() {
        @SuppressWarnings("unchecked")
        final T me = (T) this;
        return me;
    }

    @Override
    public int hashCode() {
        return hash(totalNumValues);
    }
}
