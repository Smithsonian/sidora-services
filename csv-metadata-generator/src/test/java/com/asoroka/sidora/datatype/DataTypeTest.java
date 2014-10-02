/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.datatype;

import static com.asoroka.sidora.datatype.DataType.Boolean;
import static com.asoroka.sidora.datatype.DataType.DateTime;
import static com.asoroka.sidora.datatype.DataType.Decimal;
import static com.asoroka.sidora.datatype.DataType.Geographic;
import static com.asoroka.sidora.datatype.DataType.Integer;
import static com.asoroka.sidora.datatype.DataType.NonNegativeInteger;
import static com.asoroka.sidora.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.datatype.DataType.String;
import static com.asoroka.sidora.datatype.DataType.orderingByHierarchy;
import static com.asoroka.sidora.datatype.DataType.parseableAs;
import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.sort;
import static java.util.EnumSet.of;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

public class DataTypeTest {

    private static final Logger log = getLogger(DataTypeTest.class);

    private static Map<DataType, Set<DataType>> expectedParseableTypes;

    private static Map<DataType, Set<DataType>> expectedSuperTypes;

    private static Map<DataType, Set<String>> sampleValues;

    {
        {
            final ImmutableMap.Builder<DataType, Set<DataType>> b = builder();

            b.put(PositiveInteger, of(String, Decimal, Integer, NonNegativeInteger, PositiveInteger, DateTime));
            b.put(NonNegativeInteger, of(String, Decimal, Integer, NonNegativeInteger, DateTime));
            b.put(Integer, of(String, Decimal, Integer, DateTime));
            b.put(Decimal, of(String, Decimal));
            b.put(Geographic, of(Geographic, String));
            b.put(Boolean, of(Boolean, String));
            b.put(DateTime, of(DateTime, String));
            b.put(String, of(String));

            expectedParseableTypes = b.build();

            final ImmutableMap.Builder<DataType, Set<DataType>> b2 = builder();

            b2.put(PositiveInteger, of(String, Decimal, Integer, NonNegativeInteger, PositiveInteger));
            b2.put(NonNegativeInteger, of(String, Decimal, Integer, NonNegativeInteger));
            b2.put(Integer, of(String, Decimal, Integer));
            b2.put(Decimal, of(String, Decimal));
            b2.put(Geographic, of(String, Geographic));
            b2.put(Boolean, of(String, Boolean));
            b2.put(String, of(String));
            b2.put(DateTime, of(String, DateTime));

            expectedSuperTypes = b2.build();

            final ImmutableMap.Builder<DataType, Set<String>> b3 = builder();

            b3.put(PositiveInteger, newHashSet("123", "9000"));
            b3.put(NonNegativeInteger, newHashSet("0"));
            b3.put(Integer, newHashSet("-1", "-9999"));
            b3.put(Decimal, newHashSet("-5344543.4563453", "6734.999"));
            b3.put(Geographic, newHashSet("38.03,-78.478889"));
            b3.put(Boolean, newHashSet("truE", "falsE"));
            b3.put(String, newHashSet("oobleck"));
            b3.put(DateTime, newHashSet("1990-3-4"));

            sampleValues = b3.build();
        }
    }

    @Test
    public void testCorrectParsingOfValues() {
        for (final DataType testType : DataType.values()) {
            for (final String testValue : sampleValues.get(testType)) {
                final Set<DataType> result = parseableAs(testValue);
                assertEquals("Did not find the appropriate datatypes suggested as parseable for a " + testType + "!",
                        expectedParseableTypes
                                .get(testType), result);
            }
        }
    }

    @Test
    public void testSupertypes() {
        for (final DataType testDatatype : DataType.values()) {
            final Set<DataType> result = testDatatype.supertypes();
            assertEquals("Did not find the appropriate supertypes for" + testDatatype + "!", expectedSuperTypes
                    .get(testDatatype), result);
        }
    }

    @Test
    public void testOrderingByHierarchy() {
        List<DataType> listToBeSorted = _(Decimal, PositiveInteger, NonNegativeInteger, Integer, String);
        List<DataType> listInCorrectSorting =
                _(String, Decimal, Integer, NonNegativeInteger, PositiveInteger);
        sort(listToBeSorted, orderingByHierarchy);
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, listToBeSorted);

        listToBeSorted = _(Geographic, String);
        listInCorrectSorting = _(String, Geographic);
        sort(listToBeSorted, orderingByHierarchy);
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, listToBeSorted);

        listToBeSorted = _(DateTime, String);
        listInCorrectSorting = _(String, DateTime);
        sort(listToBeSorted, orderingByHierarchy);
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, listToBeSorted);

        listToBeSorted = _(Boolean, String);
        listInCorrectSorting = _(String, Boolean);
        sort(listToBeSorted, orderingByHierarchy);
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, listToBeSorted);
    }

    @SafeVarargs
    private static <E> ArrayList<E> _(final E... es) {
        return newArrayList(es);
    }
}
