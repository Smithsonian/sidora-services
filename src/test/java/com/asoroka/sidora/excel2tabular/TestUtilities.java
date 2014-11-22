
package com.asoroka.sidora.excel2tabular;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestUtilities {

    public static <E> Answer<Iterator<E>> iterate(final E... es) {
        return new Answer<Iterator<E>>() {

            private List<E> elements = Arrays.asList(es);

            @Override
            public Iterator<E> answer(final InvocationOnMock invocation) throws Throwable {
                return elements.iterator();
            }
        };
    }
}
