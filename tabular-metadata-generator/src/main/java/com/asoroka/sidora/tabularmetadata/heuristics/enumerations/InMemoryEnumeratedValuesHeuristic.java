
package com.asoroka.sidora.tabularmetadata.heuristics.enumerations;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Maps.asMap;
import static com.google.common.collect.MultimapBuilder.enumKeys;
import static com.google.common.collect.Multimaps.forMap;

import java.util.Map;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.AbstractHeuristic;
import com.google.common.collect.Multimap;

/**
 * A {@link EnumeratedValuesHeuristic} that maintains an in-memory map of enumerated lexes.
 * 
 * @author ajs6f
 */
public class InMemoryEnumeratedValuesHeuristic extends
        AbstractHeuristic<InMemoryEnumeratedValuesHeuristic, Map<DataType, Set<String>>> implements
        EnumeratedValuesHeuristic<InMemoryEnumeratedValuesHeuristic> {

    private Multimap<DataType, String> valuesSeen;

    @Override
    public boolean addValue(final String lex) {
        valuesSeen.putAll(forMap(asMap(parseableAs(lex), constant(lex))));
        return true;
    }

    @Override
    public void reset() {
        valuesSeen = enumKeys(DataType.class).hashSetValues().build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<DataType, Set<String>> results() {
        // See: https://code.google.com/p/google-collections/issues/detail?id=118#c2
        return (Map<DataType, Set<String>>) (Map<?, ?>) valuesSeen.asMap();
    }

    @Override
    public InMemoryEnumeratedValuesHeuristic get() {
        return new InMemoryEnumeratedValuesHeuristic();
    }
}
