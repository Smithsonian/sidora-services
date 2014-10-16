
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Iterables.all;
import static java.util.Collections.singleton;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * Tests a row of fields to see if they represent a header. <br/>
 * TODO allow this type to access more information than a single row of fields with which to make its determination
 * 
 * @author ajs6f
 */
public interface HeaderHeuristic<T extends HeaderHeuristic<T>> extends Heuristic,
        Provider<T> {

    boolean isHeader();

    /**
     * As the name implies, a {@link HeaderHeuristic} that treats each field in the row the same and applies a single
     * test to each. Only if every field value passes the field-test does the row pass this test. Subclasses define
     * the test by implementing {@link #fieldTest()}.
     * 
     * @author ajs6f
     * @param <T>
     */
    public static abstract class TreatsEachFieldAlikeHeaderHeuristic<T extends TreatsEachFieldAlikeHeaderHeuristic<T>>
            implements HeaderHeuristic<T> {

        private static final Logger log = getLogger(TreatsEachFieldAlikeHeaderHeuristic.class);

        private List<String> inputRow = new ArrayList<>();

        @Override
        public boolean isHeader() {
            log.trace("Checking input row: {}", inputRow);
            return all(inputRow, fieldTest());
        }

        protected abstract Predicate<? super String> fieldTest();

        @Override
        public void addValue(final String value) {
            inputRow.add(value);
        }

        @Override
        public void reset() {
            inputRow.clear();
        }
    }

    /**
     * This is a very simple test of whether a line is a header line. Only any line in which each field parses only as
     * a {@link DataType#String} will be accepted.
     */
    public static class Default extends TreatsEachFieldAlikeHeaderHeuristic<Default> {

        @Override
        protected Predicate<? super String> fieldTest() {
            return isOnlyString;
        }

        private static final Predicate<String> isOnlyString = new Predicate<String>() {

            @Override
            public boolean apply(final String value) {
                return parseableAs(value).equals(singleton(DataType.String));
            }
        };

        @Override
        public Default get() {
            return this;
        }
    }
}
