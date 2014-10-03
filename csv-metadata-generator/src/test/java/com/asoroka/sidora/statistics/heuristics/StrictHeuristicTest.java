
package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.Boolean;
import static com.asoroka.sidora.datatype.DataType.DateTime;
import static com.asoroka.sidora.datatype.DataType.Decimal;
import static com.asoroka.sidora.datatype.DataType.Geographic;
import static com.asoroka.sidora.datatype.DataType.Integer;
import static com.asoroka.sidora.datatype.DataType.NonNegativeInteger;
import static com.asoroka.sidora.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.datatype.DataType.String;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.asoroka.sidora.datatype.DataType;

public class StrictHeuristicTest {

    private StrictHeuristic testStrictHeuristic;

    @Before
    public void setUp() {
        testStrictHeuristic = new StrictHeuristic();
    }

    private static Map<DataType, List<String>> goodValues;

    private static Map<DataType, List<String>> oneBadValue;
    {
        {
            // these are all good (parseable) values for the datatype to which they are assigned
            goodValues = new HashMap<>();
            goodValues.put(String, newArrayList("Jane", "John", "Sarah", "Simon"));
            goodValues.put(Decimal, newArrayList("0", "0.1", "-1", "34.345"));
            goodValues.put(Integer, newArrayList("0", "1", "-1 ", "34"));
            goodValues.put(NonNegativeInteger, newArrayList("0", "1", " 11", "34"));
            goodValues.put(PositiveInteger, newArrayList("35", "1", "11 ", "34"));
            goodValues.put(Boolean, newArrayList("True", "False", "TruE", "FaLse"));
            goodValues.put(Geographic, newArrayList("38.03,-78.478889", " -78.478889,38.03", "1,0", "0,1"));
            goodValues.put(DateTime, newArrayList("1990-3-4", "2014-273", "2014-W40-2", "2014-W40",
                    "2014-09-30T18:58:45Z"));

            // here we add one bad value to each list of good values
            oneBadValue = new HashMap<>(goodValues);
            oneBadValue.get(Decimal).add("0sd");
            oneBadValue.get(Integer).add("-1.3 ");
            oneBadValue.get(NonNegativeInteger).add("-11");
            oneBadValue.get(PositiveInteger).add("0");
            oneBadValue.get(Boolean).add("Q");
            oneBadValue.get(Geographic).add("38.03");
            oneBadValue.get(DateTime).add("2014/24");
            // nothing cannot parse as a String
            oneBadValue.remove(String);
        }
    }

    public void testActionWithGoodValues() {
        for (final DataType testType : DataType.values()) {
            testStrictHeuristic = new StrictHeuristic();
            for (final String testValue : goodValues.get(testType)) {
                testStrictHeuristic.addValue(testValue);
            }
            assertEquals("Didn't get the correct type for datatype " + testType + "!", testType, testStrictHeuristic
                    .mostLikelyType());
        }
    }

    @Test
    public void testActionWithOneBadValue() {
        for (final DataType testType : oneBadValue.keySet()) {
            testStrictHeuristic = new StrictHeuristic();
            for (final String testValue : oneBadValue.get(testType)) {
                testStrictHeuristic.addValue(testValue);
            }
            assertFalse("Got the correct type for datatype " + testType + " but shoudn't have!",
                    testStrictHeuristic.mostLikelyType().equals(testType));
        }
    }

}
