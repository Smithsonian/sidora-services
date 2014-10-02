/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.String;
import static com.asoroka.sidora.datatype.DataType.firstMostRestrictiveType;
import static com.google.common.collect.Sets.filter;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Set;

import org.slf4j.Logger;

import com.asoroka.sidora.datatype.DataType;
import com.google.common.base.Predicate;

public class StrictlySatisfactoryType extends RunningMinMaxHeuristic<StrictlySatisfactoryType> {

    final EnumMap<DataType, Float> typeCounts = new EnumMap<>(DataType.class);

    private static final Logger log = getLogger(StrictlySatisfactoryType.class);

    /**
     * Initialize counts for each datatype.
     */
    public StrictlySatisfactoryType() {
        for (final DataType type : DataType.values()) {
            typeCounts.put(type, 0F);
        }
    }

    @Override
    public DataType mostLikelyType() {
        final Float totalNumValues = typeCounts.get(String);
        log.trace("Working with {} total values.", totalNumValues);
        // we filter to only those types which were selected as parseable for all values
        final Set<DataType> possibleTypes = filter(typeCounts.keySet(), new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType type) {
                return typeCounts.get(type).equals(totalNumValues);
            }
        });
        log.trace("Found candidate types: {}", possibleTypes);
        return firstMostRestrictiveType(possibleTypes);

    }

    @Override
    public void addValue(final String value) {
        super.addValue(value);
        for (final DataType type : DataType.parseableAs(value)) {
            typeCounts.put(type, typeCounts.get(type) + 1);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public StrictlySatisfactoryType clone() {
        return new StrictlySatisfactoryType();
    }
}
