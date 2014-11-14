
package com.asoroka.sidora.excel2csv;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.google.common.io.Resources;

/**
 * Some convenient utilities for tests.
 * 
 * @author ajs6f
 */
public class TestUtils {

    /**
     * A cheap and cheerful 2-tuple.
     * 
     * @author ajs6f
     * @param <A>
     * @param <B>
     */
    protected static class Pair<A, B> {

        public A a;

        public B b;

        private Pair(final A a2, final B b2) {
            this.a = a2;
            this.b = b2;
        }

        public static <A, B> Pair<A, B> of(final A a, final B b) {
            return new Pair<>(a, b);
        }
    }

    /**
     * Zips two lists together into a list of {@link Pair}s that is as long as the shorter input.
     * 
     * @param listA
     * @param listB
     * @return a zipped list
     */
    protected static <A, B> List<Pair<A, B>> zip(final List<A> listA, final List<B> listB) {

        final List<Pair<A, B>> zipped = new LinkedList<>();
        final int smallerListSize = min(listA.size(), listB.size());
        for (int i = 0; i < smallerListSize; i++) {
            zipped.add(Pair.of(listA.get(i), listB.get(i)));
        }
        return zipped;
    }

    protected static List<String> readLines(final URL result) throws IOException {
        return Resources.readLines(result, UTF_8);
    }

    protected static void compareLines(final List<String> resultLines, final List<String> checkLines) {
        for (final Pair<String, String> line : zip(checkLines, resultLines)) {
            assertEquals("Got bad line in results!", line.a, line.b);
        }
    }

}
