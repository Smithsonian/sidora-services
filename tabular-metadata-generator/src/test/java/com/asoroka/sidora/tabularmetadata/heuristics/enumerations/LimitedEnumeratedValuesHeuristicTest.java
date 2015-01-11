
package com.asoroka.sidora.tabularmetadata.heuristics.enumerations;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class LimitedEnumeratedValuesHeuristicTest extends
        EnumeratedValuesHeuristicTestFrame<LimitedEnumeratedValuesHeuristic> {

    @Override
    protected LimitedEnumeratedValuesHeuristic newTestHeuristic() {
        return new LimitedEnumeratedValuesHeuristic();
    }

    @Test
    public void testLimitedScan() {
        final LimitedEnumeratedValuesHeuristic testStrategy =
                new LimitedEnumeratedValuesHeuristic(1, new InMemoryEnumeratedValuesHeuristic());
        for (int i = 0; i < 5; i++) {
            testStrategy.addValue(Integer.toString(i));
        }
        final Map<DataType, Set<String>> results = testStrategy.results();
        final Set<String> enumeration = results.get(DataType.String);
        assertEquals(1, enumeration.size());
    }
}
