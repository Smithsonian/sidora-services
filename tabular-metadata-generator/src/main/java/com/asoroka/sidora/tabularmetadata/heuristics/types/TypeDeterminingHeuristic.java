
package com.asoroka.sidora.tabularmetadata.heuristics.types;

import java.util.SortedSet;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;

/**
 * Determines into which {@link DataType}s a series of values most likely falls.
 * 
 * @author ajs6f
 */
public interface TypeDeterminingHeuristic<SelfType extends TypeDeterminingHeuristic<SelfType>> extends
        Heuristic<SelfType, SortedSet<DataType>> {

}
