
package com.asoroka.sidora.tabularmetadata.heuristics;

/**
 * Convenience class for arranging {@link Heuristic} initialization. Subclasses should put all initialization code in
 * {@link #reset()} and assume that it will be called on construction.
 * 
 * @author ajs6f
 */
public abstract class AbstractHeuristic<SelfType extends AbstractHeuristic<SelfType, ResultType>, ResultType>
        implements Heuristic<SelfType, ResultType> {

    public AbstractHeuristic() {
        reset();
    }
}
