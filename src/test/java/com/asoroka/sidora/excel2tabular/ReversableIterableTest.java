
package com.asoroka.sidora.excel2tabular;

import static com.asoroka.sidora.excel2tabular.ReversableIterable.reversed;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReversableIterableTest {

    @Mock
    Iterator<Object> mockForwardIterator;

    @Mock
    Iterator<Object> mockBackwardIterator;

    @Test
    public void testReversed() {
        final ReversableIterable<?> testReversable = new ReversableIterable<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return mockForwardIterator;
            }

            @Override
            Iterator<Object> reversed() {
                return mockBackwardIterator;
            }
        };
        final ReversableIterable<?> testReversableReversed = reversed(testReversable);
        assertEquals(mockForwardIterator, testReversableReversed.reversed());
        assertEquals(mockBackwardIterator, testReversableReversed.iterator());
    }
}
