
package com.asoroka.sidora.tabularmetadata.heuristics;

import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.collect.Range;

public abstract class RunningMinMaxHeuristicTestFrame<T extends RunningMinMaxHeuristic<T>> extends
        PerTypeHeuristicTestFrame<T> {

    private static final Logger log = getLogger(RunningMinMaxHeuristicTestFrame.class);

    @Test
    public void testMinMax() {
        log.trace("testMinMax()...");
        final T intTestStrategy = newTestHeuristic();
        for (byte i = -10; i < 10; i++) {
            intTestStrategy.addValue(String.valueOf(i));
        }
        final Range<Integer> intMinMax = intTestStrategy.getRange();
        final int intMin = intMinMax.lowerEndpoint();
        final int intMax = intMinMax.upperEndpoint();
        assertEquals("Got wrong minimum!", -10, intMin, 0);
        assertEquals("Got wrong maximum!", 9, intMax, 0);
        final T floatTestStrategy = newTestHeuristic();
        for (float i = -10F; i < 10F; i++) {
            floatTestStrategy.addValue(String.valueOf(i));
        }
        final Range<Float> floatMinMax = floatTestStrategy.getRange();
        final float floatMin = floatMinMax.lowerEndpoint();
        final float floatMax = floatMinMax.upperEndpoint();
        assertEquals("Got wrong minimum!", -10, floatMin, 0);
        assertEquals("Got wrong maximum!", 9, floatMax, 0);
    }
}
