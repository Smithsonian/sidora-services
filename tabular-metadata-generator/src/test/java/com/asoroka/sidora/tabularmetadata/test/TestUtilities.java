
package com.asoroka.sidora.tabularmetadata.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.internal.stubbing.answers.Returns;

import com.asoroka.sidora.tabularmetadata.heuristics.ValueHeuristic;

/**
 * Utilities for testing.
 * 
 * @author ajs6f
 */
public abstract class TestUtilities {

    /**
     * The following peculiar locution arises from the need to provide "cloneability" while avoiding a recursive mock
     * 
     * @return a cloneable mock {@link ValueHeuristic}
     */
    public static <T extends ValueHeuristic<T>> ValueHeuristic<T> cloneableMockStrategy(
            final ValueHeuristic<T> strategy) {
        final ValueHeuristic<T> mocked = mock(ValueHeuristic.class);
        final Returns type = new Returns(strategy.typesAsLikely());
        when(mocked.typesAsLikely()).thenAnswer(type);
        final Returns range = new Returns(strategy.getRange());
        when(mocked.getRange()).thenAnswer(range);
        final Returns cloner = new Returns(strategy);
        when(mocked.clone()).thenAnswer(cloner);
        return mocked;
    }

}
