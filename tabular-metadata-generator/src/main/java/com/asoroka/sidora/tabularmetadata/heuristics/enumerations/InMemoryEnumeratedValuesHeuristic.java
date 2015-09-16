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
 * @author A. Soroka
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
