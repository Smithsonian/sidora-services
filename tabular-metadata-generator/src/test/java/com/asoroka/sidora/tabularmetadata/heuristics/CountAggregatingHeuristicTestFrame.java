
package com.asoroka.sidora.tabularmetadata.heuristics;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Boolean;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.DateTime;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Integer;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.NonNegativeInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.google.common.collect.ImmutableList.of;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;

public abstract class CountAggregatingHeuristicTestFrame<T extends CountAggregatingHeuristic<T>> extends
        RunningMinMaxHeuristicTestFrame<CountAggregatingHeuristic<T>> {

    protected static Map<DataType, List<String>> parseableValues;

    protected static Map<DataType, List<String>> oneNonparseableValue;

    @Before
    public void setUp() {
        parseableValues = new EnumMap<>(DataType.class);
        oneNonparseableValue = new EnumMap<>(DataType.class);
        // these are all parseable values for the datatype to which they are assigned
        parseableValues.put(String, of("Jane", "John", "Sarah", "Simon"));
        parseableValues.put(Decimal, of("0.0", "2220.19999", "-13452", "34.345"));
        parseableValues.put(Integer, of("0", "1", "-1 ", "34"));
        parseableValues.put(NonNegativeInteger, of("0", "1", " 11", "34"));
        parseableValues.put(PositiveInteger, of("354455", "13452432", "112345235 ", "34529534"));
        parseableValues.put(Boolean, of("True", "F", "TruE", "FaLse"));
        parseableValues.put(Geographic, of("38.03,-78.478889", " -78.478889,38.03", "1,0,77", "0,1"));
        parseableValues.put(DateTime, of("1990-3-4", "2014-273", "2014-W40-2", "2014-W40",
                "2014-09-30T18:58:45Z"));

        // here we add one nonparseable value to each list of parseable values
        oneNonparseableValue.put(Decimal, of("0.0", "2220.19999", "-13452", "34.345", "0sd"));
        oneNonparseableValue.put(Integer, of("0", "1", "-1 ", "34", "-1.3"));
        oneNonparseableValue.put(NonNegativeInteger, of("0", "1", " 11", "34", "-11"));
        oneNonparseableValue.put(PositiveInteger, of("354455", "13452432", "112345235 ", "34529534", "Q"));
        oneNonparseableValue.put(Boolean, of("True", "F", "TruE", "FaLse", "Q"));
        oneNonparseableValue.put(Geographic, of("38.03,-78.478889", " -78.478889,38.03", "1,0,77", "0,1", "38.03"));
        oneNonparseableValue.put(DateTime, of("1990-3-4", "2014-273", "2014-W40-2", "2014-W40",
                "2014-09-30T18:58:45Z", "2014/24"));
        // nothing cannot parse as a String
        oneNonparseableValue.remove(String);
    }
}
