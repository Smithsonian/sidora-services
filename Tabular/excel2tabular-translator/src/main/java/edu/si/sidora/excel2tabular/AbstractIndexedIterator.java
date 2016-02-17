
package edu.si.sidora.excel2tabular;

import com.google.common.collect.AbstractIterator;

/**
 * An {@link Iterator} over an integer-indexed data structure.
 * 
 * @author ajs6f
 * @param <E>
 */
public abstract class AbstractIndexedIterator<E> extends AbstractIterator<E> {

    private int index, size;

    /**
     * @param position
     * @return an element of the indexed data structure
     */
    protected abstract E get(int position);

    protected AbstractIndexedIterator(final int size) {
        this(0, size);
    }

    protected AbstractIndexedIterator(final int start, final int size) {
        this.size = size;
        this.index = start;
    }

    @Override
    protected E computeNext() {
        if (index >= size) {
            return endOfData();
        }
        return get(index++);
    }
}
