/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.statistics.heuristics;

import static com.asoroka.sidora.datatype.DataType.String;
import static com.asoroka.sidora.datatype.DataType.firstMostRestrictiveType;
import static com.google.common.collect.Sets.filter;

import java.util.EnumMap;
import java.util.Set;

import com.asoroka.sidora.datatype.DataType;
import com.google.common.base.Predicate;

public class StrictlySatisfactoryType extends RunningMinMaxHeuristic {

    final EnumMap<DataType, Float> typeCounts = new EnumMap<>(DataType.class);

    @Override
    public DataType mostLikelyType() {
        final Float totalNumValues = typeCounts.get(String);
        // we select for only those types which were selected as possible for all values
        final Set<DataType> possibleTypes = filter(typeCounts.keySet(), new Predicate<DataType>() {

            @Override
            public boolean apply(final DataType type) {
                return typeCounts.get(type) == totalNumValues;
            }
        });
        return firstMostRestrictiveType(possibleTypes);

    }

    @Override
    public void addValue(final String value) {
        super.addValue(value);
        for (final DataType type : DataType.parseableAs(value)) {
            typeCounts.put(type, typeCounts.get(type) + 1);
        }
    }
}
