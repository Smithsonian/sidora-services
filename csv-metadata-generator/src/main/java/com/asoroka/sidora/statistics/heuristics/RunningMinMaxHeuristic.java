/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.firstMostRestrictiveType;
import static java.lang.Float.parseFloat;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.asoroka.sidora.datatype.DataType;

/**
 * @author ajs6f
 */
public abstract class RunningMinMaxHeuristic implements TypeDeterminationHeuristic {

    protected Float numericMinimum;

    protected Float numericMaximum;

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#mostLikelyType()
     */
    @Override
    abstract public DataType mostLikelyType();

    protected static DataType mostLikelyType(final String value) {
        return firstMostRestrictiveType(DataType.parseableAs(value));
    }

    protected static boolean isLikelyNumeric(final String value) {
        return mostLikelyType(value).isNumeric();
    }

    @Override
    public boolean isLikelyNumeric() {
        return mostLikelyType().isNumeric();
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#addValue(java.lang.String)
     */
    @Override
    public void addValue(final String value) {
        if (isLikelyNumeric(value)) {
            final float floatValue = parseFloat(value);
            numericMinimum = min(numericMinimum, floatValue);
            numericMaximum = max(numericMaximum, floatValue);
        }

    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#getNumericMaximum()
     */
    @Override
    public Float getNumericMaximum() {
        if (isLikelyNumeric()) {
            return numericMaximum;
        }
        throw new NotANumericFieldException();
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.statistics.heuristics.TypeDeterminationHeuristic#getNumericMinimum()
     */
    @Override
    public Float getNumericMinimum() {
        if (isLikelyNumeric()) {
            return numericMinimum;
        }
        throw new NotANumericFieldException();
    }

}
