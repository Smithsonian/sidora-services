
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Integer;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

public class RunningMinMaxHeuristicTest {

    // private static final Logger log = getLogger(RunningMinMaxHeuristicTest.class);

    private static TestRunningMinMaxHeuristic createTestStrategy(final DataType type) {
        return new TestRunningMinMaxHeuristic(type);
    }

    @Test
    public void testMinMax() {
        TestRunningMinMaxHeuristic testStrategy = createTestStrategy(Integer);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        final Range<Integer> intMinMax = testStrategy.getRange();
        final Integer intMin = intMinMax.lowerEndpoint();
        final Integer intMax = intMinMax.upperEndpoint();
        assertEquals("Got wrong minimum!", 0, intMin, 0);
        assertEquals("Got wrong maximum!", 9, intMax, 0);
        testStrategy = createTestStrategy(Decimal);
        for (byte i = 0; i < 10; i++) {
            testStrategy.addValue(String.valueOf(i));
        }
        final Range<Float> floatMinMax = testStrategy.getRange();
        final Float floatMin = floatMinMax.lowerEndpoint();
        final Float floatMax = floatMinMax.upperEndpoint();
        assertEquals("Got wrong minimum!", 0, floatMin, 0);
        assertEquals("Got wrong maximum!", 9, floatMax, 0);
    }

    private static class TestRunningMinMaxHeuristic extends RunningMinMaxHeuristic<TestRunningMinMaxHeuristic> {

        public TestRunningMinMaxHeuristic(final DataType type) {
            this.type = type;
        }

        final DataType type;

        @Override
        protected boolean candidacy(final DataType candidateType) {
            return type.equals(candidateType);
        }

        @Override
        public TestRunningMinMaxHeuristic clone() {
            return new TestRunningMinMaxHeuristic(type);
        }

        @Override
        public TestRunningMinMaxHeuristic get() {
            return this;
        }
    }
}
