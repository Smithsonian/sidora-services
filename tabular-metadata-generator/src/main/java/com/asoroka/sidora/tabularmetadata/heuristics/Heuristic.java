
package com.asoroka.sidora.tabularmetadata.heuristics;

public interface Heuristic {

    /**
     * Provide a value to this heuristic for consideration.
     * 
     * @param value the value to consider
     */
    public void addValue(final String value);

}
