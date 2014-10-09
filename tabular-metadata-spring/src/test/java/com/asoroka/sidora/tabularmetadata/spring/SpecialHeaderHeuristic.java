
package com.asoroka.sidora.tabularmetadata.spring;

import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic;

/**
 * A trivial implementation of {@link HeaderHeuristic} that refuses to recognize anything as a header row.
 * 
 * @author ajs6f
 */
public class SpecialHeaderHeuristic implements HeaderHeuristic {

    @Override
    public boolean apply(final Iterable<String> input) {
        return false;
    }

    @Override
    public HeaderHeuristic get() {
        return this;
    }

}
