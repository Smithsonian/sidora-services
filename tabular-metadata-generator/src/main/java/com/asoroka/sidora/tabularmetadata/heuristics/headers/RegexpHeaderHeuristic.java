
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import static com.google.common.base.Predicates.contains;
import static java.util.regex.Pattern.compile;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.common.base.Predicate;

/**
 * A {@link HeaderHeuristic} that uses a regular expression applied to each field in a row to determine whether the
 * row is a header row.
 * 
 * @author A. Soroka
 */
public class RegexpHeaderHeuristic extends TreatsEachFieldAlikeHeaderHeuristic<RegexpHeaderHeuristic> {

    private final Pattern regexp;

    private static final Logger log = getLogger(RegexpHeaderHeuristic.class);

    /**
     * @param regexp The regular expression against which to match.
     */
    public RegexpHeaderHeuristic(final String regexp) {
        log.debug("Using {} for header determination with pattern {}.", this.getClass(), regexp);
        this.regexp = compile(regexp);
    }

    @Override
    protected Predicate<CharSequence> fieldTest() {
        return contains(regexp);
    }

    @Override
    public RegexpHeaderHeuristic get() {
        return new RegexpHeaderHeuristic(regexp.pattern());
    }
}
