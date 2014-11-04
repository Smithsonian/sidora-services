
package com.asoroka.sidora.excel2csv;

import static java.lang.Math.min;

import java.util.LinkedList;
import java.util.List;

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

}
