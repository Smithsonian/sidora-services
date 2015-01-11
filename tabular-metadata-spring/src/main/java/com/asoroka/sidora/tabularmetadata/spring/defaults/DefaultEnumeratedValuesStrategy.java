
package com.asoroka.sidora.tabularmetadata.spring.defaults;

import org.springframework.stereotype.Component;

import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.InMemoryEnumeratedValuesHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.LimitedEnumeratedValuesHeuristic;

/**
 * Supplies {@link InMemoryEnumeratedValuesHeuristic} as the default enumerated values determination strategy in
 * Spring integrations.
 * 
 * @author A. Soroka
 */
@Component
public class DefaultEnumeratedValuesStrategy extends LimitedEnumeratedValuesHeuristic {
    // NO CONTENT
}
