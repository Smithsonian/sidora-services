/*
 * Copyright 2015-2016 Smithsonian Institution.
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

import static java.lang.Float.isNaN;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import edu.si.sidora.tabularmetadata.datatype.DataType;

/**
 * A heuristic that permits a certain specified fraction of values to fail at parsing to the most likely type.
 * 
 * @author A. Soroka
 */
public class FractionHeuristic extends PerTypeHeuristic<FractionHeuristic> {

    private final float fractionOfAllowedNonparseables;

    private static final Logger log = getLogger(FractionHeuristic.class);

    /**
     * @param nonParsingFraction The fraction of nonparsing values to permit
     */
    public FractionHeuristic(final float nonParsingFraction) {
        super();
        this.fractionOfAllowedNonparseables = nonParsingFraction;
    }

    @Override
    protected boolean candidacy(final DataType type) {
        final float nonParseableOccurrences = valuesSeen() - typeCounts.getOrDefault(type, 0);
        log.trace("Found {} nonparseable occurrences out of {} total values for type {}.", nonParseableOccurrences,
                valuesSeen(), type);

        final float nonParseableFraction = nonParseableOccurrences / valuesSeen();
        if (isNaN(nonParseableFraction)) {
            // there were no lexes accepted
            return true;
        }
        log.trace("for a nonparseable fraction of {}.", nonParseableFraction);
        return nonParseableFraction <= fractionOfAllowedNonparseables;
    }

    @Override
    public FractionHeuristic get() {
        return new FractionHeuristic(fractionOfAllowedNonparseables);
    }
}
