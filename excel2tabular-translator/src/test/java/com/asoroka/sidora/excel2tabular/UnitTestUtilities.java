
package com.asoroka.sidora.excel2tabular;

import static java.util.Arrays.asList;

import java.util.Iterator;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class UnitTestUtilities {

    @SafeVarargs
    public static <E> Answer<Iterator<E>> iterateOver(final E... es) {
        return new Answer<Iterator<E>>() {

            private List<E> elements = asList(es);

            @Override
            public Iterator<E> answer(final InvocationOnMock invocation) {
                return elements.iterator();
            }
        };
    }
}
