/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */


package edu.si.sidora.tabularmetadata.heuristics.types;

import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Maps.toMap;
import static edu.si.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static java.lang.Float.NEGATIVE_INFINITY;
import static java.util.EnumSet.complementOf;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import edu.si.sidora.tabularmetadata.datatype.DataType;

public class MinimumDistanceBetweenNonparseablesHeuristic extends
        PerTypeHeuristic<MinimumDistanceBetweenNonparseablesHeuristic> {

    private final int minimumDistance;

    private Map<DataType, Boolean> candidateTypes;

    /**
     * We define locations as {@link Float}s in order to use the special value {@link Float#NEGATIVE_INFINITY}, which
     * does not exist for integer types in Java.
     */
    private Map<DataType, Float> locationsOfLastNonparseables;

    public MinimumDistanceBetweenNonparseablesHeuristic(final int minimumDistance) {
        super();
        this.minimumDistance = minimumDistance;
    }

    @Override
    public void reset() {
        super.reset();
        candidateTypes = new EnumMap<>(DataType.class);
        locationsOfLastNonparseables = new EnumMap<>(DataType.class);
        // assume that every type is a candidate
        final Map<DataType, Boolean> allTrue = toMap(DataType.datatypes(), constant(true));
        candidateTypes.putAll(allTrue);

        // record that we haven't yet seen any nonparseables
        final Map<DataType, Float> originalLocations = toMap(DataType.datatypes(), constant(NEGATIVE_INFINITY));
        locationsOfLastNonparseables.putAll(originalLocations);
    }

    @Override
    protected boolean candidacy(final DataType type) {
        return candidateTypes.get(type);
    }

    @Override
    public boolean addValue(final String value) {
        if (super.addValue(value)) {
            for (final DataType type : notParseableAs(value)) {
                final float distanceToLastNonParseableOfThisType =
                        valuesSeen() - locationsOfLastNonparseables.get(type);
                if (distanceToLastNonParseableOfThisType < minimumDistance) {
                    // it's been too soon since the last nonparseable value of this type, knock it out of the running
                    candidateTypes.put(type, false);
                } else {
                    // mark that we saw a nonparseable value for this type
                    locationsOfLastNonparseables.put(type, (float) valuesSeen());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @param value
     * @return an {@link EnumSet} of those DataTypes into which s cannot be parsed
     */
    private static EnumSet<DataType> notParseableAs(final String value) {
        return complementOf(parseableAs(value));
    }

    @Override
    public MinimumDistanceBetweenNonparseablesHeuristic get() {
        return new MinimumDistanceBetweenNonparseablesHeuristic(minimumDistance);
    }

}
