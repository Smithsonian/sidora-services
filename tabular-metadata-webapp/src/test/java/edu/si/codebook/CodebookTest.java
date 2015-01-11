
package edu.si.codebook;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.google.common.collect.Sets.newHashSet;
import static edu.si.codebook.Codebook.codebook;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.numbers.Ratio;

import edu.si.codebook.Codebook.VariableType;
import edu.si.codebook.Codebook.VariableType.RangeType;

@RunWith(MockitoJUnitRunner.class)
public class CodebookTest {

    @Mock
    private TabularMetadata emptyMetadata, metadata;

    private static final Ratio testRatio = new Ratio(ONE, TEN);

    private static final DataType TEST_DATATYPE = PositiveInteger;

    private Map<DataType, Range<?>> minmaxes = new HashMap<>();

    private Map<DataType, Set<String>> enumerations = new HashMap<>();

    @Test
    public void testEmptyMetadata() {
        assertTrue(codebook(emptyMetadata).getVariables().isEmpty());
    }

    @Before
    public void setUp() {
        when(metadata.headerNames()).thenReturn(asList("HEADER"));
        when(metadata.unparseablesOverTotals()).thenReturn(asList(testRatio));
        when(metadata.fieldTypes()).thenReturn(asList(TEST_DATATYPE));
        when(metadata.minMaxes()).thenReturn(asList(minmaxes));
        when(metadata.enumeratedValues()).thenReturn(asList(enumerations));
        enumerations.put(TEST_DATATYPE, newHashSet(""));
    }

    @Test
    public void testOneVariableMetadata() {
        minmaxes.put(PositiveInteger, Range.all());
        final Sequence<VariableType> variables = codebook(metadata).getVariables();
        assertEquals("HEADER", variables.first().name);
        assertEquals(newHashSet(""), variables.first().enumeration);
        assertEquals(PositiveInteger.uri, variables.first().type);
        assertTrue(variables.first().getRange().max == null);
        assertTrue(variables.first().getRange().min == null);
        assertEquals(1, variables.size());
    }

    @Test
    public void testOneVariableMetadataWithRange() {
        minmaxes.put(TEST_DATATYPE, Range.closed(1, 10));
        final VariableType variable = codebook(metadata).getVariables().first();
        final RangeType range = variable.getRange();
        assertEquals("1", range.min);
        assertEquals("10", range.max);
    }

    @Test
    public void testOneVariableMetadataWithNullRange() {
        minmaxes.put(Geographic, null);
        final Sequence<VariableType> variables = codebook(metadata).getVariables();
        assertTrue(variables.first().getRange() == null);
    }
}
