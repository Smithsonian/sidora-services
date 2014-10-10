
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Sets.powerSet;
import static java.util.Objects.hash;

import java.util.Map;
import java.util.Set;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

/**
 * @author ajs6f
 */
public abstract class ChoiceAggregatingHeuristic extends PerTypeHeuristic<ChoiceAggregatingHeuristic> {

    /**
     * A {@link Map} from choices of {@link DataType}s to the number of times that choice occurred.
     */
    private Map<Set<DataType>, Integer> choiceOccurrences;

    protected ChoiceAggregatingHeuristic() {
        // record that we haven't yet seen any choices made
        final Set<Set<DataType>> choices = powerSet(DataType.valuesSet());
        choiceOccurrences.putAll(toMap(choices, constant(0)));
    }

    @Override
    protected boolean candidacy(final DataType type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ChoiceAggregatingHeuristic clone() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 2 * hash(choiceOccurrences);
    }
}
