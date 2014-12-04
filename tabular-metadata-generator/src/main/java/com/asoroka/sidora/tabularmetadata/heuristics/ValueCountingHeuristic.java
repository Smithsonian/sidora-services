
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * A {@link Heuristic} that counts the number of values received.
 * 
 * @author ajs6f
 */
public abstract class ValueCountingHeuristic<SelfType extends ValueCountingHeuristic<SelfType, ResultType>, ResultType>
        extends AbstractHeuristic<SelfType, ResultType> {

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
    public boolean addValue(final String value) {
        log.trace("Received new value: {}", value);
        totalNumValues++;
        return true;
    }

    public int totalNumValues() {
        return totalNumValues;
    }
}
