/**
 * TEMPORARY LICENSE HEADER STANDIN
 * REPLACE WITH APPROPRIATE SIDORA LICENSE
 */

package com.asoroka.sidora.tabularmetadata.datatype;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.parseableAs;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.sortByHierarchy;
import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.Sets.newHashSet;
import static com.googlecode.totallylazy.Predicates.contains;
import static com.googlecode.totallylazy.Sequences.sequence;
import static java.util.Arrays.asList;
import static java.util.EnumSet.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.asoroka.sidora.tabularmetadata.testframework.RowsOfRandomValuesForAllTypes;
import com.asoroka.sidora.tabularmetadata.testframework.TestUtilities.RandomValuesForAType;
import com.google.common.collect.ImmutableMap;
import com.googlecode.totallylazy.Function1;

@RunWith(Theories.class)
public class DataTypeTest {

    // private static final Logger log = getLogger(DataTypeTest.class);

    private static final Map<DataType, Set<DataType>> expectedParseableTypes;

    private static final Map<DataType, Set<DataType>> expectedSuperTypes;

    private static final Map<DataType, Set<String>> sampleValues;

    private static final Map<String, DataType> dataTypeNames;

    static {
        final ImmutableMap.Builder<DataType, Set<DataType>> b = builder();

        b.put(DataType.PositiveInteger, of(DataType.String, DataType.Decimal, DataType.Integer, DataType.NonNegativeInteger, DataType.PositiveInteger, DataType.DateTime));
        b.put(DataType.NonNegativeInteger, of(DataType.String, DataType.Decimal, DataType.Integer, DataType.NonNegativeInteger, DataType.DateTime));
        b.put(DataType.Integer, of(DataType.String, DataType.Decimal, DataType.Integer, DataType.DateTime));
        b.put(DataType.Decimal, of(DataType.String, DataType.Decimal));
        b.put(DataType.Geographic, of(DataType.Geographic, DataType.String));
        b.put(DataType.Boolean, of(DataType.Boolean, DataType.String));
        b.put(DataType.DateTime, of(DataType.DateTime, DataType.String));
        b.put(DataType.String, of(DataType.String));
        b.put(DataType.URI, of(DataType.URI, DataType.String));

        expectedParseableTypes = b.build();

        final ImmutableMap.Builder<DataType, Set<DataType>> b2 = builder();

        b2.put(DataType.PositiveInteger, of(DataType.String, DataType.Decimal, DataType.Integer, DataType.NonNegativeInteger, DataType.PositiveInteger));
        b2.put(DataType.NonNegativeInteger, of(DataType.String, DataType.Decimal, DataType.Integer, DataType.NonNegativeInteger));
        b2.put(DataType.Integer, of(DataType.String, DataType.Decimal, DataType.Integer));
        b2.put(DataType.Decimal, of(DataType.String, DataType.Decimal));
        b2.put(DataType.Geographic, of(DataType.String, DataType.Geographic));
        b2.put(DataType.Boolean, of(DataType.String, DataType.Boolean));
        b2.put(DataType.String, of(DataType.String));
        b2.put(DataType.DateTime, of(DataType.String, DataType.DateTime));
        b2.put(DataType.URI, of(DataType.String, DataType.URI));

        expectedSuperTypes = b2.build();

        final ImmutableMap.Builder<DataType, Set<String>> b3 = builder();

        b3.put(DataType.PositiveInteger, newHashSet("123", "9000"));
        b3.put(DataType.NonNegativeInteger, newHashSet("0"));
        b3.put(DataType.Integer, newHashSet("-1", "-9999"));
        b3.put(DataType.Decimal, newHashSet("-5344543.4563453", "6734.999"));
        b3.put(DataType.Geographic, newHashSet("38.03,-78.478889", "38.03,-78.478889, 0"));
        b3.put(DataType.Boolean, newHashSet("truE", "falsE", "t", "F"));
        b3.put(DataType.String, newHashSet(":::oobleck"));
        b3.put(DataType.URI, newHashSet("http://example.com"));
        b3.put(DataType.DateTime, newHashSet("1990-3-4"));

        sampleValues = b3.build();

        final ImmutableMap.Builder<String, DataType> b4 = builder();

        b4.putAll(ImmutableMap
                .of("PositiveInteger", DataType.PositiveInteger, "NonNegativeInteger", DataType.NonNegativeInteger));
        b4.putAll(ImmutableMap
                .of("Decimal", DataType.Decimal, "Geographic", DataType.Geographic, "Boolean", DataType.Boolean));
        b4.putAll(ImmutableMap
                .of("DateTime", DataType.DateTime, "String", DataType.String, "URI", DataType.URI, "Integer", DataType.Integer));

        dataTypeNames = b4.build();
    }

    @Test
    public void testCorrectParsingOfValues() {
        for (final DataType testType : DataType.values()) {
            for (final String testValue : sampleValues.get(testType)) {
                final Set<DataType> result = parseableAs(testValue);
                assertEquals("Did not find the appropriate datatypes suggested as parseable for a " + testType + "!", expectedParseableTypes
                        .get(testType), result);
            }
        }
    }

    @Theory
    public void testSampleValues(
            @RowsOfRandomValuesForAllTypes(numRowsPerType = 10, valuesPerType = 10) final RandomValuesForAType values) {
        assertTrue(sequence(values).map(parseableAs).forAll(contains(values.type)));
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
        List<DataType> listToBeSorted =
                asList(DataType.Decimal, DataType.PositiveInteger, DataType.Integer, DataType.NonNegativeInteger, DataType.String);
        List<DataType> listInCorrectSorting =
                asList(DataType.PositiveInteger, DataType.NonNegativeInteger, DataType.Integer, DataType.Decimal, DataType.String);
        List<DataType> sorted = new ArrayList<>(sortByHierarchy(listToBeSorted));
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, sorted);

        listToBeSorted = asList(DataType.String, DataType.Geographic);
        listInCorrectSorting = asList(DataType.Geographic, DataType.String);
        sorted = new ArrayList<>(sortByHierarchy(listToBeSorted));
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, sorted);

        listToBeSorted = asList(DataType.String, DataType.DateTime);
        listInCorrectSorting = asList(DataType.DateTime, DataType.String);
        sorted = new ArrayList<>(sortByHierarchy(listToBeSorted));
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, sorted);

        listToBeSorted = asList(DataType.String, DataType.Boolean);
        listInCorrectSorting = asList(DataType.Boolean, DataType.String);
        sorted = new ArrayList<>(sortByHierarchy(listToBeSorted));
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, sorted);

        listToBeSorted = asList(DataType.String, DataType.URI);
        listInCorrectSorting = asList(DataType.URI, DataType.String);
        sorted = new ArrayList<>(sortByHierarchy(listToBeSorted));
        assertEquals("Got wrong ordering by hierarchy of datatypes!", listInCorrectSorting, sorted);
    }

    @Test
    public void testBadGeographies() {
        String testValue = "23, 23, 23, 23";
        assertFalse("Accepted a four-valued tuple as geographic coordinates!", parseableAs(testValue)
                .contains(DataType.Geographic));
        testValue = "23";
        assertFalse("Accepted a one-valued tuple as geographic coordinates!", parseableAs(testValue)
                .contains(DataType.Geographic));
    }

    @Test
    public void testNoDecimalPointDecimal() {
        final String testValue = "7087";
        assertTrue("Failed to accept a no-decimal-point number as a legitimate Decimal!", parseableAs(testValue)
                .contains(DataType.Decimal));
    }

    @Test
    public void testBadIntegerPartDecimal() {
        final String testValue = "fhglf.7087";
        assertFalse("Accepted a \"number\" with non-integral integer part as a legitimate Decimal!", parseableAs(testValue)
                .contains(DataType.Decimal));
    }

    @Test
    public void testBadDecimalPartDecimal() {
        final String testValue = "34235.dfgsdfg";
        assertFalse("Accepted a \"number\" with non-integral decimal part as a legitimate Decimal!", parseableAs(testValue)
                .contains(DataType.Decimal));
    }

    @Test
    public void testBadBothPartsDecimal() {
        final String testValue = "sgsg.dfgsdfg";
        assertFalse("Accepted a \"number\" with non-integral decimal part as a legitimate Decimal!", parseableAs(testValue)
                .contains(DataType.Decimal));
    }

    @Test
    public void testCompletelyBadDecimal() {
        final String testValue = "s24fgsdfg";
        assertFalse("Accepted a \"number\" with non-integral decimal part as a legitimate Decimal!", parseableAs(testValue)
                .contains(DataType.Decimal));
    }

    @Test
    public void testBadURI() {
        final String testValue = "38.03,-78.478889";
        assertFalse("Accepted a string that cannot be parsed as an URI as a legitimate URI!", parseableAs(testValue)
                .contains(DataType.URI));
    }

    @Test
    public void testNames() {
        assertEquals("Our test datatype names are fewer or greater in number than the number of actual DataTypes!",
                DataType.values().length, dataTypeNames.size());
        for (final String name : dataTypeNames.keySet()) {
            assertEquals(dataTypeNames.get(name), DataType.valueOf(name));
        }
    }

    private static final Function1<Comparable<?>, Set<DataType>> parseableAs =
            new Function1<Comparable<?>, Set<DataType>>() {

                @Override
                public Set<DataType> call(final Comparable<?> value) {
                    return parseableAs(value.toString());
                }
            };
}
