
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Boolean;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.DateTime;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Integer;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.NonNegativeInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public abstract class CountAggregatingHeuristicTestFrame<T extends CountAggregatingHeuristic<T>> {

    protected CountAggregatingHeuristic<?> testHeuristic;

    protected abstract T newTestInstance();

    protected static Map<DataType, List<String>> parseableValues;

    protected static Map<DataType, List<String>> oneNonparseableValue;
    static {
        // these are all parseable values for the datatype to which they are assigned
        parseableValues = new EnumMap<>(DataType.class);
        parseableValues.put(String, of("Jane", "John", "Sarah", "Simon"));
        parseableValues.put(Decimal, of("0", "0.1", "-1", "34.345"));
        parseableValues.put(Integer, of("0", "1", "-1 ", "34"));
        parseableValues.put(NonNegativeInteger, of("0", "1", " 11", "34"));
        parseableValues.put(PositiveInteger, of("354455", "13452432", "112345235 ", "34529534"));
        parseableValues.put(Boolean, of("True", "F", "TruE", "FaLse"));
        parseableValues.put(Geographic, of("38.03,-78.478889", " -78.478889,38.03", "1,0,77", "0,1"));
        parseableValues.put(DateTime, of("1990-3-4", "2014-273", "2014-W40-2", "2014-W40",
                "2014-09-30T18:58:45Z"));

        // copy the parseable values for reuse
        oneNonparseableValue = new EnumMap<>(DataType.class);
        for (final DataType type : DataType.values()) {
            oneNonparseableValue.put(type, newArrayList(parseableValues.get(type)));

        }
        // here we add one nonparseable value to each list of parseable values
        oneNonparseableValue.get(Decimal).add("0sd");
        oneNonparseableValue.get(Integer).add("-1.3 ");
        oneNonparseableValue.get(NonNegativeInteger).add("-11");
        oneNonparseableValue.get(PositiveInteger).add("Q");
        oneNonparseableValue.get(Boolean).add("Q");
        oneNonparseableValue.get(Geographic).add("38.03");
        oneNonparseableValue.get(DateTime).add("2014/24");
        // nothing cannot parse as a String
        oneNonparseableValue.remove(String);
    }

    @Test
    public void testClone() {
        testHeuristic = newTestInstance();
        assertTrue(testHeuristic.clone().equals(testHeuristic));
    }

    @Test
    public void testEquals() {
        testHeuristic = newTestInstance();
        assertTrue(testHeuristic.equals(newTestInstance()));
        assertFalse(testHeuristic.equals(new Object()));

        final CountAggregatingHeuristic<?> nonEqualObject = new NonEqualHeuristic();
        nonEqualObject.addValue("test value");
        assertFalse(testHeuristic.equals(nonEqualObject));
    }

    protected static class NonEqualHeuristic extends CountAggregatingHeuristic<NonEqualHeuristic> {

        @Override
        protected boolean candidacy(final DataType d) {
            return false;
        }

        @Override
        public NonEqualHeuristic clone() {
            return new NonEqualHeuristic();
        }

        @Override
        public NonEqualHeuristic get() {
            return this;
        }
    }
}
