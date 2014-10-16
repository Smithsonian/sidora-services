
package com.asoroka.sidora.tabularmetadata.heuristics;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public class CachingEnumeratedValuesHeuristic implements EnumeratedValuesHeuristic {

    EnumMap<DataType, Set<Serializable>> valuesTakenOn = new EnumMap<>(DataType.class);

    @Override
    public void addValue(final String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reset() {
        valuesTakenOn = new EnumMap<>(DataType.class);
    }

}
