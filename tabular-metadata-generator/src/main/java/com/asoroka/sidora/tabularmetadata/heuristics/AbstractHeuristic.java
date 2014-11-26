
package com.asoroka.sidora.tabularmetadata.heuristics;

/**
 * @author ajs6f
 */
public abstract class AbstractHeuristic<SelfType extends AbstractHeuristic<SelfType>> implements Heuristic<SelfType> {

    public AbstractHeuristic() {
        reset();
    }

    @Override
    public abstract SelfType newInstance();

}
