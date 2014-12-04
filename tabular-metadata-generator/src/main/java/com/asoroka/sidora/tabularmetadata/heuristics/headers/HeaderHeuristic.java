
package com.asoroka.sidora.tabularmetadata.heuristics.headers;

import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;

/**
 * Tests a row of fields to see if they represent a header. <br/>
 * TODO allow this type to access more information than a single row of fields with which to make its determination
 * 
 * @author ajs6f
 */
public interface HeaderHeuristic<SelfType extends HeaderHeuristic<SelfType>> extends Heuristic<SelfType, Boolean> {

}
