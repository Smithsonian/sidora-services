
package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import static com.google.common.collect.Ordering.natural;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.types.HeuristicTestFrame;
import com.asoroka.sidora.tabularmetadata.test.RandomValues;
import com.google.common.collect.Range;

/**
 * Because of the enormous mix of data types in these tests, and because this is in fact test code, and most of all,
 * because of Java's really pitiful lack of type variance, it is simpler here to dispense with some type safety, hence
 * the annotation to this class.
 * 
 * @author ajs6f
 */
@SuppressWarnings("rawtypes")
@RunWith(Theories.class)
public class RunningMinMaxHeuristicTest extends HeuristicTestFrame<RunningMinMaxHeuristic> {

    @DataPoints
    public static DataType[] datatypes = DataType.values();

    protected static DateTime date(final String v) {
        return dateTimeParser().parseDateTime(v);
    }

    @Override
    protected RunningMinMaxHeuristic newTestHeuristic() {
        return new RunningMinMaxHeuristic();
    }

    private static final Logger log = getLogger(RunningMinMaxHeuristicTest.class);

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
     * 
     * @param values a {@link Map} from DataTypes to a random selection of values appropriate to that type.
     */
    @Theory
    public void testMinsAndMaxesShouldAlwaysBeFoundPerType(
            @RandomValues(numRuns = 5, valuesPerType = 5) final Map<DataType, List<Comparable>> values) {
        for (final DataType type : values.keySet()) {
            final List<Comparable> typeValues = values.get(type);
            final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
            final Comparable lowest = natural().min(typeValues);
            final Comparable highest = natural().max(typeValues);
            for (final Comparable value : typeValues) {
                testStrategy.addValue(value.toString());
            }
            final Range<?> range = testStrategy.getRanges().get(type);
            final Comparable calculatedMaxForType = range.upperEndpoint();
            final Comparable calculatedMinForType = range.lowerEndpoint();
            assertEquals("Calculated maximum should be the highest value submitted!", highest,
                    calculatedMaxForType);
            assertEquals("Calculated minimum should be the lowest value submitted!", lowest,
                    calculatedMinForType);
        }
    }
}
