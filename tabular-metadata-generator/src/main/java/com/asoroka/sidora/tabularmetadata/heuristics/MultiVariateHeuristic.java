
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Sets.powerSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.Predicate;

public class MultiVariateHeuristic extends ChoiceCountAggregatingHeuristic<MultiVariateHeuristic> {

    private Map<DataType, Boolean> candidacies;

    public Map<DataType, Float> typeLikelihoods;

    private final float minimum;

    private static final Logger log = getLogger(MultiVariateHeuristic.class);

    /**
     * @param minimum
     */
    public MultiVariateHeuristic(final float minimum) {
        super();
        this.minimum = minimum;

        final Set<Set<DataType>> rawPowerSet = powerSet(DataType.valuesSet());
        final int numChoices = rawPowerSet.size();
        final Float likelihoodPerAppearance = 1 / (float) numChoices;
        log.trace("likelihoodPerAppearance: {}", likelihoodPerAppearance);
        typeLikelihoods = new EnumMap<>(DataType.class);
        final Map<DataType, Float> zeroes = toMap(DataType.valuesSet(), constant(0F));
        typeLikelihoods.putAll(zeroes);

        for (final DataType type : DataType.valuesSet()) {
            for (final Set<DataType> choice : rawPowerSet) {
                if (choice.contains(type)) {
                    final Float currentLikelihood = typeLikelihoods.containsKey(type) ? typeLikelihoods.get(type) : 0;
                    log.trace("Current likelihood of {} is {}.", type, currentLikelihood);
                    typeLikelihoods.put(type, currentLikelihood + likelihoodPerAppearance);
                }
            }
        }
        // assume all types are out of the running until proven otherwise
        candidacies = new EnumMap<>(DataType.class);
        final Map<DataType, Boolean> falses = toMap(DataType.valuesSet(), constant(false));
        candidacies.putAll(falses);

    }

    @Override
    protected boolean candidacy(final DataType type) {

        // determine which types are acceptable
        final Set<Entry<DataType, Boolean>> entrySet = candidacies.entrySet();
        final Collection<Entry<DataType, Boolean>> candidates = filter(entrySet, valueTrue);
        return candidates.contains(type);
    }

    @Override
    public MultiVariateHeuristic clone() {
        return this;
    }

    private static final Predicate<Entry<DataType, Boolean>> valueTrue =
            new Predicate<Entry<DataType, Boolean>>() {

                @Override
                public boolean apply(final Entry<DataType, Boolean> e) {
                    return e.getValue();
                }
            };

}
