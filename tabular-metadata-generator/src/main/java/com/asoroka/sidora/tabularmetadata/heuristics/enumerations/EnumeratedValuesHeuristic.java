
package com.asoroka.sidora.tabularmetadata.heuristics.enumerations;

import java.util.Map;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.heuristics.Heuristic;

/**
 * A heuristic to enumerate lexes as recognized by type.<br/>
 * Determines a {@link Map} from {@link DataType} to a {@link Set} of lexes chosen from the lexes supplied to this
 * heuristic. A lex will be included in the set for a given datatype if and only if it was found to be parseable into
 * that type. For example, all lexes will be included in the set for {@link DataType#String}, since any lex can be
 * parsed as a String, but if the lexes presented are {"1","Foo","34,56"}, then the set for {@link DataType#Integer}
 * will include only "1".
 * 
 * @author ajs6f
 */
public interface EnumeratedValuesHeuristic<SelfType extends EnumeratedValuesHeuristic<SelfType>> extends
        Heuristic<SelfType, Map<DataType, Set<String>>> {

}
