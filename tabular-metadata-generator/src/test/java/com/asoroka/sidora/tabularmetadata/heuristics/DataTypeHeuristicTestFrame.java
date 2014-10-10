
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

public abstract class DataTypeHeuristicTestFrame<T extends DataTypeHeuristic<T>> {

    protected abstract T newTestHeuristic();

    @Test
    public void testClone() {
        final T testHeuristic = newTestHeuristic();
        assertTrue(testHeuristic.clone().equals(testHeuristic));
    }

    @Test
    public void testEquals() {
        final T testHeuristic = newTestHeuristic();
        assertTrue(testHeuristic.equals(newTestHeuristic()));
        assertFalse(testHeuristic.equals(new Object()));

        final NonEqualHeuristic nonEqualObject = new NonEqualHeuristic();
        assertFalse(testHeuristic.equals(nonEqualObject));
    }

    protected static class NonEqualHeuristic implements DataTypeHeuristic<NonEqualHeuristic> {

        @Override
        public NonEqualHeuristic clone() {
            return new NonEqualHeuristic();
        }

        @Override
        public NonEqualHeuristic get() {
            return this;
        }

        @Override
        public DataType mostLikelyType() {
            // NO OP
            return null;
        }

        @Override
        public void addValue(final String value) {
            // NO OP
        }

        @Override
        public <MinMax extends Comparable<MinMax>> Range<MinMax> getRange() {
            // NO OP
            return null;
        }
    }
}
