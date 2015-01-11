
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static java.util.Collections.singleton;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

/**
 * This is a very simple test of whether a line is a header line. Only any line in which each field parses only as a
 * {@link DataType#String} will be accepted.
 */
public class DefaultHeaderHeuristic extends TreatsEachFieldAlikeHeaderHeuristic<DefaultHeaderHeuristic> {

    @Override
    protected Predicate<? super String> fieldTest() {
        return isOnlyString;
    }

    private static final Predicate<String> isOnlyString = new Predicate<String>() {

        @Override
        public boolean apply(final String value) {
            return parseableAs(value).equals(singleton(String));
        }
    };

    @Override
    public DefaultHeaderHeuristic get() {
        return new DefaultHeaderHeuristic();
    }

}