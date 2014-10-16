
package com.asoroka.sidora.tabularmetadata.heuristics;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * @author ajs6f
 */
public abstract class AbstractDataTypeHeuristic<T extends AbstractDataTypeHeuristic<T>> implements
        ValueHeuristic<T> {

    @Override
    public DataType mostLikelyType() {
        return typesAsLikely().first();
    }

    @Override
    public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange() {
        final Range<MinMax> minMax = (Range<MinMax>) getRanges().get(mostLikelyType());
        return minMax;
    }

    /*
     * (non-Javadoc)
     * @see com.asoroka.sidora.tabularmetadata.heuristics.ValueHeuristic<T>#clone()
     */
    @Override
    public abstract T clone();

    @Override
    public boolean equals(final Object o) {
        if (this.getClass().isInstance(o)) {
            @SuppressWarnings("unchecked")
            final T t = (T) o;
            return this.hashCode() == t.hashCode();
        }
        return false;
    }

}
