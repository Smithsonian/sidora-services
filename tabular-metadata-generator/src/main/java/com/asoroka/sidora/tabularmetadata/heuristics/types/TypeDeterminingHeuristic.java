
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;

/**
 * Determines into which {@link DataType} a series of values most likely falls.
 * 
 * @author ajs6f
 */
public interface TypeDeterminingHeuristic<SelfType extends TypeDeterminingHeuristic<SelfType>> extends
        Heuristic<SelfType, DataType> {

    /**
     * @return number of values since most-recent reset (or initialization)
     */
    int valuesSeen();

    /**
     * @return number of values since most-recent reset (or initialization) that could parse as the likely type
     */
    int parseableValuesSeen();

}
