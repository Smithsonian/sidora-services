
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static com.google.common.collect.Iterables.all;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.heuristics.AbstractHeuristic;
import com.google.common.base.Predicate;

/**
 * As the name implies, a {@link HeaderHeuristic} that treats each field in the row the same and applies a single test
 * to each. Only if every field value passes the field-test does the row pass this test. Subclasses define the test by
 * implementing {@link #fieldTest()}.
 * 
 * @author ajs6f
 * @param <SelfType>
 */
public abstract class TreatsEachFieldAlikeHeaderHeuristic<SelfType extends TreatsEachFieldAlikeHeaderHeuristic<SelfType>>
        extends AbstractHeuristic<SelfType> implements HeaderHeuristic<SelfType> {

    private static final Logger log = getLogger(TreatsEachFieldAlikeHeaderHeuristic.class);

    private List<String> inputRow;

    @Override
    public boolean isHeader() {
        log.trace("Checking input row: {}", inputRow);
        return all(inputRow, fieldTest());
    }

    protected abstract Predicate<? super String> fieldTest();

    @Override
    public boolean addValue(final String value) {
        return inputRow.add(value);
    }

    @Override
    public void reset() {
        inputRow = new ArrayList<>();
    }

    @Override
    public abstract SelfType clone();
}