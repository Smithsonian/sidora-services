
package com.asoroka.sidora.tabularmetadata.heuristics;

import com.asoroka.sidora.tabularmetadata.SelfTypeInstanceGenerator;

/**
 * The simple idea of a heuristic that accepts lexes and does some kind of work with them. See its subtypes for
 * examples of use.
 * 
 * @author ajs6f
 */
public interface Heuristic<SelfType extends Heuristic<SelfType>> extends SelfTypeInstanceGenerator<SelfType> {

    /**
     * Provide a value to this heuristic for consideration.
     * 
     * @param lex the value to consider
     * @return whether or not to continue evaluating the lex (for use in polymorphic chains of evaluation)
     */
    public boolean addValue(final String lex);

    /**
     * Resets this heuristic to discard all gathered information.
     */
    void reset();

}
