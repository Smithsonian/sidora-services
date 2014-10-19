
package com.asoroka.sidora.tabularmetadata.heuristics.ranges;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Range.closed;
import static java.util.Arrays.asList;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.datatype.GeographicValue;
import com.asoroka.sidora.tabularmetadata.heuristics.types.HeuristicTestFrame;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;

/**
 * Because of the enormous mix of data types in these tests, and because this is in fact test code, and most of all,
 * because of Java's really pitiful lack of type variance, it is simpler here to dispense with some type safety, hence
 * the annotation to this class.
 * 
 * @author ajs6f
 */
@SuppressWarnings("rawtypes")
public class RunningMinMaxHeuristicTest extends HeuristicTestFrame<RunningMinMaxHeuristic> {

    private static Map<DataType, Map<Range, List>> sampleData;

    static {
        sampleData =
                ImmutableMap.<DataType, Map<Range, List>> builder()
                        .put(
                                DataType.Integer,
                                of((Range) closed(-10, 10),
                                        (List) newArrayList(-10, -5, 0, 5, 10)))
                        .put(
                                DataType.Decimal,
                                of((Range) closed(-10F, 10F),
                                        (List) newArrayList(-10F, -5F, 0F, 5F, 10F)))
                        .put(
                                DataType.String,
                                of((Range) closed("BAR", "QUUX"),
                                        (List) newArrayList("FOO", "BAR", "QUUX")))
                        .put(
                                DataType.Geographic,
                                of((Range) closed(new GeographicValue(asList(0F, 0F)), new GeographicValue(asList(1F,
                                        1F))),
                                        (List) newArrayList("0,0", "0,1", "0,2", "1,0", "1,1")))
                        .put(
                                DataType.PositiveInteger,
                                of((Range) closed(1, 10),
                                        (List) newArrayList("1", "5", "10")))
                        .put(
                                DataType.NonNegativeInteger,
                                of((Range) closed(0, 10),
                                        (List) newArrayList("0", "1", "5", "10")))
                        .put(
                                DataType.URI,
                                of((Range) closed(URI.create("http://a"), URI.create("http://z")),
                                        (List) newArrayList("http://a", "http://k", "http://r", "http://z")))
                        .put(
                                DataType.Boolean,
                                of(
                                        (Range) closed(true, true), (List) newArrayList("true", "true", "true"),
                                        (Range) closed(false, true), (List) newArrayList("true", "false", "true")))
                        .put(
                                DataType.DateTime,
                                of((Range) closed(date("0"), date("100")),
                                        (List) newArrayList("0", "25", "50", "100")))
                        .build();

    }

    protected static DateTime date(final String v) {
        return dateTimeParser().parseDateTime(v);
    }

    @Override
    protected RunningMinMaxHeuristic newTestHeuristic() {
        return new RunningMinMaxHeuristic();
    }

    private static final Logger log = getLogger(RunningMinMaxHeuristicTest.class);

    @Test
    public void testMinMaxsPerDataType() {
        log.trace("testMinMaxsPerDataType()...");
        for (final DataType testType : sampleData.keySet()) {
            log.trace("Testing min/max for type: {}", testType);
            final Map<Range, List> sampleRangeAndData = sampleData.get(testType);
            final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
            for (final Range expectedRange : sampleRangeAndData.keySet()) {
                log.trace("Found expected range: {}", expectedRange);
                final List sampleDataForRange = sampleRangeAndData.get(expectedRange);
                for (final Object lex : sampleDataForRange) {
                    log.trace("Adding lex: {}", lex);
                    testStrategy.addValue(lex.toString());
                }
                final Map<DataType, Range<?>> minMaxes = testStrategy.getRanges();
                final Range minMax = minMaxes.get(testType);
                assertEquals("Got wrong min/max!", expectedRange, minMax);
            }
        }
    }

    @Test
    public void testMissingLimits() {
        final RunningMinMaxHeuristic testStrategy = newTestHeuristic();
        for (int i = -10; i <= -1; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        // now we ask for a range for a data type for which it was not set
        final Range<GeographicValue> geographicRange =
                (Range<GeographicValue>) testStrategy.getRanges().get(DataType.Geographic);
        assertEquals("Found a defined range where we should not have!", Range.all(), geographicRange);
    }
}
