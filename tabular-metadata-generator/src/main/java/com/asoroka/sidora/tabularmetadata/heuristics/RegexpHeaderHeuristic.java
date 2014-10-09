
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.google.common.base.Predicates.contains;
import static java.util.regex.Pattern.compile;

import java.util.regex.Pattern;

import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic.TreatsEachFieldAlikeHeaderHeuristic;
import com.google.common.base.Predicate;

/**
 * A {@link HeaderHeuristic} that uses a regular expression applied to each field in a row to determine whether the
 * row is a header row.
 * 
 * @author ajs6f
 */
public class RegexpHeaderHeuristic extends TreatsEachFieldAlikeHeaderHeuristic<RegexpHeaderHeuristic> {

    private final Pattern regexp;

    /**
     * @param regexp The regular expression against which to match.
     */
    public RegexpHeaderHeuristic(final String regexp) {
        this.regexp = compile(regexp);
    }

    @Override
    protected Predicate<CharSequence> fieldTest() {
        return contains(regexp);
    }

    @Override
    public RegexpHeaderHeuristic get() {
        return this;
    }
}
