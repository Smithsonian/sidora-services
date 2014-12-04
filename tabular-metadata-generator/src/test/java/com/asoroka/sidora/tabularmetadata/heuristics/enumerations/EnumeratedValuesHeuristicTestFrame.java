
package com.asoroka.sidora.tabularmetadata.heuristics.enumerations;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.HeuristicTestFrame;
import com.google.common.collect.ImmutableMap;

public abstract class EnumeratedValuesHeuristicTestFrame<TestHeuristic extends EnumeratedValuesHeuristic<TestHeuristic>>
        extends HeuristicTestFrame<TestHeuristic, Map<DataType, Set<String>>> {

    // TODO clean up sample data and make it more understandable

    private static Map<DataType, List<String>> simpleData;

    /**
     * A map from types, to maps from correct answers, to simple test data.
     */
    private static Map<DataType, Map<Set<String>, List<String>>> simpleTestData = new HashMap<>();

    /**
     * A map from types, to maps from correct answers, to slightly more realistic test data.
     */
    private static Map<DataType, Map<Set<String>, List<String>>> realisticTestData = new HashMap<>();

    static {
        simpleData = ImmutableMap.<DataType, List<String>> builder()
                .put(DataType.String,
                        asList("FOO", "FOO", "BAR", "QUUX"))
                .put(DataType.Integer,
                        asList("1", "3", "3", "5"))
                .put(DataType.Geographic,
                        asList("1,2", "1,3", "1,2", "5,6"))
                .build();
        for (final DataType type : simpleData.keySet()) {
            final List<String> dataList = simpleData.get(type);
            simpleTestData.put(type, ImmutableMap.<Set<String>, List<String>> builder()
                    .put(new HashSet<>(dataList), dataList).build());
        }
    }
    static {
        realisticTestData =
                ImmutableMap.<DataType, Map<Set<String>, List<String>>> builder()
                        .put(DataType.String,
                                of((Set<String>) newHashSet("FOO", "BAR", "QUUX"), asList("FOO", "FOO",
                                        "BAR", "QUUX")))
                        .put(DataType.Integer,
                                of((Set<String>) newHashSet("1", "3", "5"),
                                        asList("1", "NOTANINTEGER", "3", "3", "6.7", "5")))
                        .put(DataType.Decimal,
                                of((Set<String>) newHashSet("1", "3", "5"),
                                        asList("1", "NOTADECIMAL", "3", "3", "5")))
                        .put(DataType.Geographic,
                                of((Set<String>) newHashSet("1,2", "1,3", "5,6"),
                                        asList("1,2", "1,3", "1,2", "5,6", "1", "NOTAGEOGRAPHICVALUE")))
                        .build();
    }

    @Test
    public void testWithSimpleData() {
        testWithSampleData(simpleTestData);
    }

    @Test
    public void testWithRealisticData() {
        testWithSampleData(realisticTestData);
    }

    public void testWithSampleData(final Map<DataType, Map<Set<String>, List<String>>> data) {
        for (final DataType type : data.keySet()) {
            final Map<Set<String>, List<String>> expectedDataToTestData = data.get(type);
            for (final Set<String> expectedResults : expectedDataToTestData.keySet()) {
                final TestHeuristic testStrategy = newTestHeuristic();
                for (final String lex : expectedDataToTestData.get(expectedResults)) {
                    testStrategy.addValue(lex);
                }
                final Set<String> results = testStrategy.results().get(type);
                assertEquals(expectedResults, results);
            }
        }
    }

}
