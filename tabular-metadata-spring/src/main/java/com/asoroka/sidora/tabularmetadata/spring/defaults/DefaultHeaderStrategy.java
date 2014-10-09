
package com.asoroka.sidora.tabularmetadata.spring.defaults;

import org.springframework.stereotype.Component;

import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic;

/**
 * Supplies {@link HeaderHeuristic.Default} as the default header determination strategy in Spring integrations.
 * 
 * @author ajs6f
 */
@Component
public class DefaultHeaderStrategy extends HeaderHeuristic.Default {

}
