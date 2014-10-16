/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics;

import javax.inject.Provider;

/**
 * Generally, implementations of this type should be used <i>only</i> on a single series of values drawn from a single
 * column of a single tabular data set: it is expected that implementations will maintain state that cannot be reset
 * for reuse.
 * 
 * @author ajs6f
 * @param <T>
 */
public interface ValueHeuristic<T extends ValueHeuristic<T>> extends TypeDeterminingHeuristic,
        RangeDeterminingHeuristic, Cloneable,
        Provider<ValueHeuristic<T>> {

    /**
     * We override {@link Object#clone()} in order to narrow its return type for type-safety in the use of this
     * method.
     * 
     * @see java.lang.Object#clone()
     * @return A clone of this heuristic.
     */
    public abstract T clone();

}
