
package com.asoroka.sidora.tabularmetadata.heuristics;

/**
 * @author ajs6f
 */
public abstract class AbstractHeuristic<SelfType extends AbstractHeuristic<SelfType, ResultType>, ResultType>
        implements Heuristic<SelfType, ResultType> {

    public AbstractHeuristic() {
        reset();
    }

    @Override
    public abstract SelfType newInstance();

}
