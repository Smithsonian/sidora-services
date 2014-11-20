/**
 * 
 */

package com.asoroka.sidora.excel2tabular;

import java.util.Iterator;

/**
 * Offers the ability to iterate something in two "opposite" directions.
 * 
 * @author ajs6f
 */
public abstract class ReversableIterable<E> implements Iterable<E> {

    /**
     * @return an {@link Iterator} that iterates in the "opposite" order of {@link #iterator()}, for some
     *         sub-type-specific meaning of the term "opposite".
     */
    abstract Iterator<E> reversed();

    /**
     * Swaps the order of iteration for {@link #iterator()} and {@link #reversed()} of the input
     * {@link ReversableIterable}
     * 
     * @param reversable
     * @return a swapped form of reversable
     */
    public static <E> ReversableIterable<E> reversed(final ReversableIterable<E> reversable) {
        return new ReversableIterable<E>() {

            @Override
            public Iterator<E> iterator() {
                return reversable.reversed();
            }

            @Override
            Iterator<E> reversed() {
                return reversable.iterator();
            }
        };
    }
}
