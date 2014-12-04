
package com.asoroka.sidora.tabularmetadata.heuristics;

import com.asoroka.sidora.tabularmetadata.SelfTypeInstanceGenerator;
import com.googlecode.totallylazy.Function1;

/**
 * The simple idea of a heuristic that accepts lexes and does some kind of work with them. See its subtypes for
 * examples of use.
 * 
 * @author ajs6f
 */
public interface Heuristic<SelfType extends Heuristic<SelfType, ResultType>, ResultType> extends
        SelfTypeInstanceGenerator<SelfType> {

    /**
     * Provide a value to this heuristic for consideration.
     * 
     * @param lex the value to consider
     * @return whether or not to continue evaluating the lex (for use in polymorphic chains of evaluation)
     */
    boolean addValue(final String lex);

    /**
     * Resets this heuristic to discard all gathered information.
     */
    void reset();

    ResultType results();

    public static class Extract<ResultType> extends Function1<Heuristic<?, ResultType>, ResultType> {

        @Override
        public ResultType call(final Heuristic<?, ResultType> strategy) {
            return strategy.results();
        }
    }
}
