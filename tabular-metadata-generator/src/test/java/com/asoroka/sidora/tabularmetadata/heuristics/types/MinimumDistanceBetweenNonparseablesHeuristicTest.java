
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.SortedSet;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.HeuristicTestFrame;

public class MinimumDistanceBetweenNonparseablesHeuristicTest extends
        HeuristicTestFrame<MinimumDistanceBetweenNonparseablesHeuristic, SortedSet<DataType>> {

    private static final int MINIMUM_DISTANCE = 2;

    final List<String> passingData = asList("1", "FOO", "3", "BAR", "5");

    final List<String> nonPassingData = asList("1", "FOO", "BAR", "4", "5");

    @Override
    protected MinimumDistanceBetweenNonparseablesHeuristic newTestHeuristic() {
        return new MinimumDistanceBetweenNonparseablesHeuristic(MINIMUM_DISTANCE);
    }

    @Test
    public void testPassingAndNonpassingData() {
        final MinimumDistanceBetweenNonparseablesHeuristic testStrategy = newTestHeuristic();
        for (final String i : passingData) {
            testStrategy.addValue(i);
        }
        assertEquals(
                "Failed to accept type with nonparseable values less than or equal to the minimum distance apart!",
                PositiveInteger, testStrategy.results().first());
        for (final String i : nonPassingData) {
            testStrategy.addValue(i);
        }
        assertEquals("Failed to reject types with nonparseable values greater than the minimum distance apart!",
                DataType.String, testStrategy.results().first());
    }
}
