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

    abstract Iterator<E> reversed();

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
