
package com.asoroka.sidora.tabularmetadata;

import static com.google.common.collect.Iterables.getFirst;
import static com.googlecode.totallylazy.Sequences.sequence;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Group;

/**
 * A container for the results of metadata extraction on a single data file.
 * 
 * @author ajs6f
 */
public class TabularMetadata {

    static final <T> Map<DataType, T> EMPTY_DATATYPE_MAP() {
        return Collections.<DataType, T> emptyMap();
    }

    /**
     * A list of header names.
     */
    public final List<String> headerNames;

    /**
     * A list of candidates for field datatypes, one candidate set per field. Each candidate set is sorted according
     * to a notion of decreasing plausibility that is specific to the type-determination strategy used.
     */
    public final List<SortedSet<DataType>> fieldTypes;

    /**
     * A list of maps from {@link DataType}s to {@link Range}s, with each data type mapped to the minimum and maximum
     * for each field, <i>if</i> that field is treated as correctly of that type. The values of the endpoints of the
     * ranges, if they exist, are in the Java type value-space associated to the datatype considered most likely for
     * that field. The idea here is that when a given field is finally determined to be of a given type (by user
     * action after automatic action), the appropriate range can be looked up at that time. This is to ensure that in
     * a situation where the type determination strategy employed gave a wrong answer, the correct answer for range
     * can still be found after the type determination has been corrected.
     * 
     * @see com.google.common.collect.Range<?>
     */
    public final List<SortedMap<DataType, Range<?>>> minMaxes;

    /**
     * A list (one element for each field in the input file) of maps from each possible datatype to an set of the
     * lexes found in that field that were parseable into that data type.
     */
    public final List<SortedMap<DataType, Set<String>>> enumeratedValues;

    /**
     * @param headerNames
     * @param fieldTypes
     * @param minMaxes
     * @param enumeratedValues
     */
    public TabularMetadata(final List<String> headerNames, final List<SortedSet<DataType>> fieldTypes,
            final List<Map<DataType, Range<?>>> minMaxes, final List<Map<DataType, Set<String>>> enumeratedValues) {
        this.headerNames = headerNames;
        this.fieldTypes = fieldTypes;
        // sort range and enumerated values reporting by most likely type
        this.minMaxes =
                sequence(minMaxes).groupBy(indexByPosition()).map(this.<Range<?>> sortByLikelihood()).toList();
        this.enumeratedValues =
                sequence(enumeratedValues).groupBy(indexByPosition()).map(this.<Set<String>> sortByLikelihood())
                        .toList();
    }

    static <T> Callable1<T, Integer> indexByPosition() {
        return new Callable1<T, Integer>() {

            private int i = 0;

            @Override
            public Integer call(final T element) {
                return i++;
            }
        };
    }

    /**
     * WARNING: Must not be called before setting {@link #fieldTypes}!
     */
    private <T> Callable1<Group<Integer, Map<DataType, T>>, SortedMap<DataType, T>> sortByLikelihood() {
        return new Callable1<Group<Integer, Map<DataType, T>>, SortedMap<DataType, T>>() {

            @Override
            public SortedMap<DataType, T> call(
                    final Group<Integer, Map<DataType, T>> minMaxMapWithIndex) {
                final TreeMap<DataType, T> sortedByLikelihood =
                        new TreeMap<>(fieldTypes.get(minMaxMapWithIndex.key()).comparator());
                final Map<DataType, T> unsortedMinMaxMap =
                        getFirst(minMaxMapWithIndex, TabularMetadata.<T> EMPTY_DATATYPE_MAP());
                sortedByLikelihood.putAll(unsortedMinMaxMap);
                return sortedByLikelihood;
            }
        };
    }

    private static final ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(TabularMetadata.class);
    }

    @Override
    public String toString() {
        return toStringHelper().add("headerNames", headerNames).add("fieldTypes", fieldTypes).add("enumeratedValues",
                enumeratedValues).add("minMaxes", minMaxes).toString();
    }

    private static final Logger log = getLogger(TabularMetadata.class);

}
