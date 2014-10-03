
package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.Decimal;
import static com.asoroka.sidora.datatype.DataType.Geographic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.asoroka.sidora.datatype.DataType;

public class RunningMinMaxHeuristicTest {

    @Test
    public void testMinMax() {
        final RunningMinMaxHeuristic<?> testStrategy = createTestStrategy(Decimal);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        assertEquals("Got wrong minimum!", 0, testStrategy.getMinimum(), 0);
        assertEquals("Got wrong maximum!", 9, testStrategy.getMaximum(), 0);
    }

    @Test
    public void testNonNumericDataType() {
        final RunningMinMaxHeuristic<?> testStrategy = createTestStrategy(Geographic);
        try {
            testStrategy.getMinimum();
            fail();
        } catch (final NotAComparableFieldException e) {
            // expected
        }
        try {
            testStrategy.getMaximum();
            fail();
        } catch (final NotAComparableFieldException e) {
            // expected
        }
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
