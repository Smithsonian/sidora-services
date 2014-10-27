
package com.asoroka.sidora.tabularmetadata.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.internal.stubbing.answers.Returns;

import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.EnumeratedValuesHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.ranges.RangeDeterminingHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.types.TypeDeterminingHeuristic;

/**
 * Utilities for testing.
 * 
 * @author ajs6f
 */
public abstract class TestUtilities {

    /**
     * The following peculiar locution arises from the need to provide "cloneability" while avoiding a recursive mock
     * 
     * @return a cloneable mock strategy
     */
    public static MockedHeuristic cloneableMockStrategy(final MockedHeuristic strategy) {
        final MockedHeuristic mocked = mock(MockedHeuristic.class);
        final Returns cloner = new Returns(strategy);
        when(mocked.clone()).thenAnswer(cloner);
        return mocked;
    }

    /**
     * Exists purely to help simplify mocking for tests.
     * 
     * @author ajs6f
     */
    public static interface MockedHeuristic extends TypeDeterminingHeuristic<MockedHeuristic>,
            RangeDeterminingHeuristic<MockedHeuristic>,
            EnumeratedValuesHeuristic<MockedHeuristic> {
        // NO CONTENT
    }

}
