/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import com.asoroka.sidora.datatype.DataType;

public interface TypeDeterminationHeuristic {

    public DataType mostLikelyType();

    public boolean isLikelyNumeric();

    public void addValue(final String value);

    public abstract Float getNumericMaximum();

    public abstract Float getNumericMinimum();

}
