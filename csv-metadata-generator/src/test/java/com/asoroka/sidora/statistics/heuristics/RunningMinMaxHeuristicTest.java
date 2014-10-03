
package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.Decimal;
import static com.asoroka.sidora.datatype.DataType.Integer;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.datatype.DataType;

public class RunningMinMaxHeuristicTest {

    private static final Logger log = getLogger(RunningMinMaxHeuristicTest.class);

    @Test
    public void testMinMax() {
        RunningMinMaxHeuristic<?> testStrategy = createTestStrategy(Integer);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        assertEquals("Got wrong minimum!", 0, (Integer) testStrategy.getMinimum(), 0);
        assertEquals("Got wrong maximum!", 9, (Integer) testStrategy.getMaximum(), 0);
        testStrategy = createTestStrategy(Decimal);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        assertEquals("Got wrong minimum!", 0, (Float) testStrategy.getMinimum(), 0);
        assertEquals("Got wrong maximum!", 9, (Float) testStrategy.getMaximum(), 0);
    }

    private static RunningMinMaxHeuristic<?> createTestStrategy(final DataType type) {
        @SuppressWarnings("rawtypes")
        final RunningMinMaxHeuristic<?> strategy = new RunningMinMaxHeuristic() {

            @Override
            public DataType mostLikelyType() {
                return type;
            }

            @Override
            public RunningMinMaxHeuristic clone() {
                // NO OP
                return null;
            }
        };
        return strategy;
    }
}
