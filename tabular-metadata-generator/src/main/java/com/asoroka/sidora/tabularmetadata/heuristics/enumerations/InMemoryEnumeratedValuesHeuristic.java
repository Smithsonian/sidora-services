
package com.asoroka.sidora.tabularmetadata.heuristics.enumerations;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * A {@link EnumeratedValuesHeuristic} that maintains an in-memory map of enumerated lexes.
 * 
 * @author ajs6f
 */
public class InMemoryEnumeratedValuesHeuristic implements EnumeratedValuesHeuristic<InMemoryEnumeratedValuesHeuristic> {

    EnumMap<DataType, Set<String>> valuesTakenOn = new EnumMap<>(DataType.class);

    public InMemoryEnumeratedValuesHeuristic() {
        super();
        for (final DataType t : DataType.values()) {
            valuesTakenOn.put(t, new HashSet<String>());
        }
    }

    @Override
    public void addValue(final String lex) {
        for (final DataType t : parseableAs(lex)) {
            valuesTakenOn.get(t).add(lex);
        }
    }

    @Override
    public void reset() {
        valuesTakenOn = new EnumMap<>(DataType.class);
    }

    @Override
    public Map<DataType, Set<String>> getEnumeratedValues() {
        return valuesTakenOn;
    }

    @Override
    public InMemoryEnumeratedValuesHeuristic clone() {
        return new InMemoryEnumeratedValuesHeuristic();
    }

    @Override
    public InMemoryEnumeratedValuesHeuristic get() {
        return this;
    }
}
