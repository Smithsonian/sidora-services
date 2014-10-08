/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.csvmetadata.heuristics;

import javax.inject.Provider;

import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * Determines into which {@link DataType} a series of values most likely falls. Also determines a minimum and maximum
 * of the supplied values.
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
    public Range<?> getRange();

    /**
     * We override {@link Object#clone()} in order to narrow its return type for type-safety.
     * 
     * @see java.lang.Object#clone()
     * @return A clone of this heuristic.
     */
    public abstract T clone();

}
