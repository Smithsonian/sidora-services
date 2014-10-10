/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics;

import javax.inject.Provider;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * Determines into which {@link DataType} a series of values most likely falls, and determines a range for the
 * supplied values. Generally, implementations of this type should be used <i>only</i> on a single series of values
 * drawn from a single column of a single tabular data set: it is expected that implementations will maintain state
 * that cannot be reset for reuse.
 * 
 * @author ajs6f
 * @param <T>
 */
public interface DataTypeHeuristic<T extends DataTypeHeuristic<T>> extends Cloneable, Provider<DataTypeHeuristic<T>> {

    /**
     * @return The most likely type for the proffered values.
     */
    public DataType mostLikelyType();

    /**
     * Provide a value to this heuristic for consideration.
     * 
     * @param value the value to consider
     */
    public void addValue(final String value);

    /**
     * @return The range taken on by proffered values in the value space of the most likely type.
     */
    public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange();

    /**
     * We override {@link Object#clone()} in order to narrow its return type for type-safety.
     * 
     * @see java.lang.Object#clone()
     * @return A clone of this heuristic.
     */
    public abstract T clone();

}
