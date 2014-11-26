
package com.asoroka.sidora.tabularmetadata.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.internal.stubbing.answers.Returns;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.EnumeratedValuesHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.ranges.RangeDeterminingHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.types.TypeDeterminingHeuristic;
import com.google.common.base.Function;

/**
 * Utilities for testing.
 * 
 * @author ajs6f
 */
public abstract class TestUtilities {

    /**
     * Extracts the most likely type selection from a {@link ValueHeuristic}
     */
    protected static final Function<TypeDeterminingHeuristic<?>, DataType> getMostLikelyType =
            new Function<TypeDeterminingHeuristic<?>, DataType>() {

                @Override
                public DataType apply(final TypeDeterminingHeuristic<?> heuristic) {
                    return heuristic.mostLikelyType();
                }
            };

    /**
     * The following peculiar locution arises from the need to provide "cloneability" while avoiding a recursive mock
     * 
     * @return a cloneable mock strategy
     */
    public static MockedHeuristic cloneableMockStrategy(final MockedHeuristic strategy) {
        final MockedHeuristic mocked = mock(MockedHeuristic.class);
        final Returns cloner = new Returns(strategy);
        when(mocked.newInstance()).thenAnswer(cloner);
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
