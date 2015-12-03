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


package edu.si.sidora.tabularmetadata.heuristics.enumerations;

import java.util.Map;
import java.util.Set;

import edu.si.sidora.tabularmetadata.datatype.DataType;
import edu.si.sidora.tabularmetadata.heuristics.Heuristic;
import edu.si.sidora.tabularmetadata.heuristics.ValueCountingHeuristic;

/**
 * A {@link Heuristic} that accepts only a limited number of values before ceasing operation.
 * 
 * @author A. Soroka
 */
public class LimitedEnumeratedValuesHeuristic extends
        ValueCountingHeuristic<LimitedEnumeratedValuesHeuristic, Map<DataType, Set<String>>>
        implements EnumeratedValuesHeuristic<LimitedEnumeratedValuesHeuristic> {

    /**
     * Default to recording 10 values.
     */
    public static final int DEFAULT_LIMIT = 10;

    private int limit = DEFAULT_LIMIT;

    private final EnumeratedValuesHeuristic<?> wrappedStrategy;

    public LimitedEnumeratedValuesHeuristic() {
        super();
        this.wrappedStrategy = new InMemoryEnumeratedValuesHeuristic();
    }

    /**
     * @param limit
     * @param wrappedStrategy
     */
    public LimitedEnumeratedValuesHeuristic(final int limit, final EnumeratedValuesHeuristic<?> wrappedStrategy) {
        super();
        this.limit = limit;
        this.wrappedStrategy = wrappedStrategy;
    }

    @Override
    public boolean accept(final String lex) {
        return super.accept(lex) && valuesSeen() <= limit ? wrappedStrategy.accept(lex) : false;
    }

    @Override
    public Map<DataType, Set<String>> results() {
        return wrappedStrategy.results();
    }

    @Override
    public LimitedEnumeratedValuesHeuristic get() {
        return new LimitedEnumeratedValuesHeuristic(limit, wrappedStrategy.get());
    }
}
