
package com.asoroka.sidora.csvmetadata.heuristics;

import static com.asoroka.sidora.csvmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.csvmetadata.datatype.DataType.Integer;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.asoroka.sidora.csvmetadata.datatype.DataType;

public class RunningMinMaxHeuristicTest {

    // private static final Logger log = getLogger(RunningMinMaxHeuristicTest.class);

    @Test
    public void testMinMax() {
        RunningMinMaxHeuristic<?> testStrategy = createTestStrategy(Integer);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        final Integer intMin = (Integer) testStrategy.getRange().lowerEndpoint();
        final Integer intMax = (Integer) testStrategy.getRange().upperEndpoint();
        assertEquals("Got wrong minimum!", 0, intMin, 0);
        assertEquals("Got wrong maximum!", 9, intMax, 0);
        testStrategy = createTestStrategy(Decimal);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        final Float floatMin = (Float) testStrategy.getRange().lowerEndpoint();
        final Float floatMax = (Float) testStrategy.getRange().upperEndpoint();
        assertEquals("Got wrong minimum!", 0, floatMin, 0);
        assertEquals("Got wrong maximum!", 9, floatMax, 0);
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
