
package com.asoroka.sidora.tabularmetadata.heuristics;

public class ScanLimitedHeuristic<InnerHeuristicType extends Heuristic<InnerHeuristicType>>
        extends ValueCountingHeuristic<ScanLimitedHeuristic<InnerHeuristicType>> {

    private final InnerHeuristicType innerHeuristic;

    private final int scanLimit;

    /**
     * @param innerHeuristic
     * @param scanLimit
     */
    public ScanLimitedHeuristic(final InnerHeuristicType innerHeuristic, final int scanLimit) {
        super();
        this.innerHeuristic = innerHeuristic;
        this.scanLimit = scanLimit;
    }

    /**
     * We test to see if we have passed the scan limit and only continue processing this lex if we have not.
     * 
     * @see com.asoroka.sidora.tabularmetadata.heuristics.ValueCountingHeuristic#addValue(java.lang.String)
     */
    @Override
    public boolean addValue(final String lex) {
        if (super.addValue(lex)) {
            return totalNumValues() <= scanLimit;
        }
        return false;
    }

    @Override
    public void reset() {
        innerHeuristic.reset();
    }

    @Override
    public ScanLimitedHeuristic<InnerHeuristicType> get() {
        return this;
    }

    @Override
    public ScanLimitedHeuristic<InnerHeuristicType> clone() {
        return new ScanLimitedHeuristic<>(innerHeuristic.clone(), scanLimit);
    }

}
