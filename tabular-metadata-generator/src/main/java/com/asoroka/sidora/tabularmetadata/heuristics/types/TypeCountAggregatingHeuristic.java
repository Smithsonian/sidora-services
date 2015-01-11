/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.heuristics.types;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Maps.toMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.ValueCountingHeuristic;
import com.google.common.collect.ImmutableMap;

/**
 * A {@link TypeDeterminingHeuristic} that aggregates candidate type appearance information for its field.
 * 
 * @author A. Soroka
 * @param <SelfType>
 */
public abstract class TypeCountAggregatingHeuristic<SelfType extends TypeCountAggregatingHeuristic<SelfType>>
        extends ValueCountingHeuristic<SelfType, DataType> implements TypeDeterminingHeuristic<SelfType> {

    private static final ImmutableMap<DataType, Integer> zeroes = toMap(DataType.valuesSet(), constant(0));

    private static final Logger log = getLogger(TypeCountAggregatingHeuristic.class);

    /**
     * In this {@link Map}, we aggregate counts of parseable values for each datatype.
     */
    protected EnumMap<DataType, Integer> typeCounts;

    /**
     * Initialize counts for each datatype.
     */
    @Override
    public void reset() {
        super.reset();
        this.typeCounts = new EnumMap<>(DataType.class);
        this.typeCounts.putAll(zeroes);
    }

    @Override
    public boolean addValue(final String lex) {
        log.trace("Accepting lex: {}", lex);
        if (super.addValue(lex)) {
            incrementCounts(parseableAs(lex));
            return true;
        }
        return false;
    }

    private void incrementCounts(final Collection<DataType> types) {
        for (final DataType type : types) {
            typeCounts.put(type, typeCounts.get(type) + 1);
        }
    }

    @Override
    public int parseableValuesSeen() {
        return typeCounts.get(results());
    }
}
