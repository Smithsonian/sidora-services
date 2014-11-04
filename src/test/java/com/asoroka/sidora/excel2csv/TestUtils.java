
package com.asoroka.sidora.excel2csv;

import static java.lang.Math.min;

import java.util.LinkedList;
import java.util.List;

public class TestUtils {

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

    protected static <A, B> List<Pair<A, B>> zip(final List<A> listA, final List<B> listB) {

        final List<Pair<A, B>> pairList = new LinkedList<>();
        final int smallerListSize = min(listA.size(), listB.size());
        for (int index = 0; index < smallerListSize; index++) {
            pairList.add(Pair.of(listA.get(index), listB.get(index)));
        }
        return pairList;
    }

}
