
package com.asoroka.sidora.tabularmetadata.heuristics;

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * A {@link DataTypeHeuristic} that counts the number of values received.
 * 
 * @author ajs6f
 */
public abstract class ValueCountingHeuristic<T extends ValueCountingHeuristic<T>> implements DataTypeHeuristic<T> {

    private int totalNumValues = 0;

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

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public abstract T clone();

    @Override
    public boolean equals(final Object o) {
        if (this.getClass().isInstance(o)) {
            @SuppressWarnings("unchecked")
            final T t = (T) o;
            return this.hashCode() == t.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash(totalNumValues);
    }
}
