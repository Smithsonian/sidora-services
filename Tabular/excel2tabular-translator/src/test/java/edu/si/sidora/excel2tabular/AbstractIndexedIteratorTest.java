
package edu.si.sidora.excel2tabular;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Iterators.elementsEqual;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class AbstractIndexedIteratorTest {

    List<String> testData = asList("One", "Two", "Three");

    @Test
    public void testFullIteration() {
        final AbstractIndexedIterator<String> testIterator = new AbstractIndexedIterator<String>(testData.size()) {

            @Override
            protected String get(final int position) {
                return testData.get(position);
            }

        };
        assertTrue(elementsEqual(testData.iterator(), testIterator));
    }

    @Test
    public void testSlice() {
        final AbstractIndexedIterator<String> testIterator = new AbstractIndexedIterator<String>(1, testData.size()) {

            @Override
            protected String get(final int position) {
                return testData.get(position);
            }

        };
        assertTrue(elementsEqual(skip(testData, 1).iterator(), testIterator));
    }
}
