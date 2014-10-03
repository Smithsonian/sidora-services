/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.firstMostRestrictiveType;
import static java.lang.Float.parseFloat;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Map;

import com.asoroka.sidora.datatype.DataType;

/**
 * @author ajs6f
 */
public abstract class RunningMinMaxHeuristic<T extends RunningMinMaxHeuristic<T>> implements
        TypeDeterminationHeuristic<T> {

    protected Map<DataType, Object> minimums;

    protected Float numericMinimum = null;

    protected Float numericMaximum = null;

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#mostLikelyType()
     */
    @Override
    abstract public DataType mostLikelyType();

    protected static DataType mostLikelyType(final String value) {
        return firstMostRestrictiveType(DataType.parseableAs(value));
    }

    protected static boolean isLikelyComparable(final String value) {
        return mostLikelyType(value).isComparable();
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#addValue(java.lang.String)
     */
    @Override
    public void addValue(final String value) {
        if (isLikelyComparable(value)) {
            final float floatValue = parseFloat(value);
            numericMinimum = (numericMinimum == null) ? floatValue : min(numericMinimum, floatValue);
            numericMaximum = (numericMaximum == null) ? floatValue : max(numericMaximum, floatValue);
        }

    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#getNumericMaximum()
     */
    @Override
    public float getMaximum() {
        if (mostLikelyType().isComparable()) {
            return numericMaximum;
        }
        throw new NotAComparableFieldException();
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#getNumericMinimum()
     */
    @Override
    public float getMinimum() {
        if (mostLikelyType().isComparable()) {
            return numericMinimum;
        }
        throw new NotAComparableFieldException();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    abstract public T clone();

}
