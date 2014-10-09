
package com.asoroka.sidora.tabularmetadata.spring.defaults;

import org.springframework.stereotype.Component;

import com.asoroka.sidora.tabularmetadata.heuristics.StrictHeuristic;

/**
 * Supplies {@link StrictHeuristic} as the default type determination strategy in Spring integrations.
 * 
 * @author ajs6f
 */
@Component
public class DefaultDatatypeStrategy extends StrictHeuristic {

}
