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


package edu.si.sidora.tabularmetadata.heuristics.headers;

import static com.google.common.collect.Iterables.all;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Predicate;

import edu.si.sidora.tabularmetadata.heuristics.AbstractHeuristic;

/**
 * As the name implies, a {@link HeaderHeuristic} that treats each field in the row the same and applies a single test
 * to each. Only if every field value passes the field-test does the row pass this test. Subclasses define the test by
 * implementing {@link #fieldTest()}.
 * 
 * @author A. Soroka
 * @param <SelfType>
 */
public abstract class TreatsEachFieldAlikeHeaderHeuristic<SelfType extends TreatsEachFieldAlikeHeaderHeuristic<SelfType>>
        extends AbstractHeuristic<SelfType, Boolean> implements HeaderHeuristic<SelfType> {

    private static final Logger log = getLogger(TreatsEachFieldAlikeHeaderHeuristic.class);

    private List<String> inputRow;

    @Override
    public Boolean results() {
        log.trace("Checking input row: {}", inputRow);
        return all(inputRow, fieldTest());
    }

    protected abstract Predicate<? super String> fieldTest();

    @Override
    public boolean addValue(final String value) {
        return inputRow.add(value);
    }

    @Override
    public void reset() {
        inputRow = new ArrayList<>();
    }
}