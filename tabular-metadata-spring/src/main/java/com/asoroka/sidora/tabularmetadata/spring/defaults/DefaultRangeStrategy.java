
package com.asoroka.sidora.tabularmetadata.spring.defaults;

import org.springframework.stereotype.Component;

import com.asoroka.sidora.tabularmetadata.heuristics.ranges.RunningMinMaxHeuristic;

/**
 * Supplies {@link RunningMinMaxHeuristic} as the default range determination strategy in Spring integrations.
 * 
 * @author ajs6f
 */
@Component
public class DefaultRangeStrategy extends RunningMinMaxHeuristic {
    // NO CONTENT
}
