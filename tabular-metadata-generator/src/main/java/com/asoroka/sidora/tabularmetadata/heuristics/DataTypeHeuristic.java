/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics;

import java.util.Map;
import java.util.SortedSet;

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
     * @return Types for the proffered values in order of their likelihood according to this heuristic.
     */
    public SortedSet<DataType> typesAsLikely();

    /**
     * @return The single most likely type for the proffered values according to this heuristic. Under any normal
     *         regime, this should be equal to {@code typesAsLikely().first()}.
     */
    public DataType mostLikelyType();

    /**
     * Provide a value to this heuristic for consideration.
     * 
     * @param value the value to consider
     */
    public void addValue(final String value);

    /**
     * @return The range taken on by proffered values in the Java value space associated to the most likely type.
     *         Under any normal regime, this should be equal to {@code getRanges.get(mostLikelyType())}.
     */
    public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange();

    /**
     * @return The ranges taken on by all values in the Java value space associated to each type.
     */
    public Map<DataType, Range<?>> getRanges();

    /**
     * We override {@link Object#clone()} in order to narrow its return type for type-safety in the use of this
     * method.
     * 
     * @see java.lang.Object#clone()
     * @return A clone of this heuristic.
     */
    public abstract T clone();

}
