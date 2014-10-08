
package com.asoroka.sidora.csvmetadata.heuristics;

import static com.asoroka.sidora.csvmetadata.datatype.DataType.parseableAs;
import static com.google.common.collect.Iterables.all;
import static java.util.Collections.singleton;

import javax.inject.Provider;

import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * Test a row of fields to see if they represent a header.
 * 
 * @author ajs6f
 */
public interface HeaderHeuristic extends Predicate<Iterable<String>>, Provider<HeaderHeuristic> {

    /**
     * This is a very simple test of whether a line is a header line. Only any line for which each field parses only
     * as a {@link DataType.String} will be accepted.
     */
    public static class Default implements HeaderHeuristic {

        @Override
        public boolean apply(final Iterable<String> line) {
            return all(line, isOnlyString);
        }

        private static final Predicate<String> isOnlyString = new Predicate<String>() {

            @Override
            public boolean apply(final String value) {
                return parseableAs(value).equals(singleton(DataType.String));
            }
        };

        @Override
        public HeaderHeuristic get() {
            return this;
        }
    }
}
