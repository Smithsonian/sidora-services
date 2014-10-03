/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import com.asoroka.sidora.datatype.DataType;

public interface TypeDeterminationHeuristic<T extends TypeDeterminationHeuristic<T>> extends Cloneable {

    public DataType mostLikelyType();

    public void addValue(final String value);

    public abstract float getMaximum();

    public abstract float getMinimum();

    public abstract T clone();

}
