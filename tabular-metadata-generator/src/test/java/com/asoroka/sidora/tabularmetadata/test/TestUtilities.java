
package com.asoroka.sidora.tabularmetadata.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.internal.stubbing.answers.Returns;

import com.asoroka.sidora.tabularmetadata.heuristics.DataTypeHeuristic;

/**
 * Utilities for testing.
 * 
 * @author ajs6f
 */
public abstract class TestUtilities {

    /**
     * The following peculiar locution arises from the need to provide "cloneability" while avoiding a recursive mock
     * 
     * @return a cloneable mock {@link DataTypeHeuristic}
     */
    public static <T extends DataTypeHeuristic<T>> DataTypeHeuristic<T> cloneableMockStrategy(
            final DataTypeHeuristic<T> strategy) {
        final DataTypeHeuristic<T> mocked = mock(DataTypeHeuristic.class);
        final Returns type = new Returns(strategy.mostLikelyType());
        when(mocked.mostLikelyType()).thenAnswer(type);
        final Returns range = new Returns(strategy.getRange());
        when(mocked.getRange()).thenAnswer(range);
        final Returns cloner = new Returns(strategy);
        when(mocked.clone()).thenAnswer(cloner);
        return mocked;
    }

}
