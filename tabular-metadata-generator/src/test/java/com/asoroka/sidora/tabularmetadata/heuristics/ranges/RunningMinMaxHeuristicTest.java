
package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import static com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.addValues;
import static com.google.common.collect.Ordering.natural;
import static org.junit.Assert.assertEquals;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.types.HeuristicTestFrame;
import com.asoroka.sidora.tabularmetadata.testframework.RowsOfRandomValuesForAllTypes;
import com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;
import com.google.common.collect.Range;

/**
 * @author ajs6f
 */
@RunWith(Theories.class)
public class RunningMinMaxHeuristicTest extends HeuristicTestFrame<RunningMinMaxHeuristic> {

    @DataPoints
    public static DataType[] datatypes = DataType.values();

    @Override
    protected RunningMinMaxHeuristic newTestHeuristic() {
        return new RunningMinMaxHeuristic();
    }

    /**
     * Asserts that the range of a {@link DataType} for which no values have been seen is {@link Range#all()}.
     */
    @Theory
    public void testMissingLimits(final DataType type) {
        final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
        final Range<?> range = testStrategy.getRanges().get(type);
        assertEquals("Found a defined range where we should not have!", Range.all(), range);
    }

    /**
     * Asserts that for each {@link DataType}, after supplying some comparable values of that type, the range recorded
     * should be the accurate range of the values supplied. This test does not examine interactions between types
     * (i.e. for values that could be parsed as several types).
     */
    @Theory
    public void testMinsAndMaxesShouldAlwaysBeFoundPerType(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 5, valuesPerType = 5) final RandomValuesForAType values) {

        final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
        final Comparable<?> lowest = natural().min(values);
        final Comparable<?> highest = natural().max(values);
        addValues(testStrategy, values);
        final Range<?> range = testStrategy.getRanges().get(values.type);
        final Comparable<?> calculatedMaxForType = range.upperEndpoint();
        final Comparable<?> calculatedMinForType = range.lowerEndpoint();
        assertEquals("Calculated maximum should be the highest value submitted!", highest,
                calculatedMaxForType);
        assertEquals("Calculated minimum should be the lowest value submitted!", lowest,
                calculatedMinForType);
    }
}
