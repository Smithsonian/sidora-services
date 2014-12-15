
package edu.si.codebook;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Geographic;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static edu.si.codebook.Codebook.codebook;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;
import com.googlecode.totallylazy.Sequence;

import edu.si.codebook.Codebook.VariableType;

@RunWith(MockitoJUnitRunner.class)
public class CodebookTest {

    @Mock
    private TabularMetadata emptyMetadata, metadata;

    private SortedSet<DataType> datatypes = newTreeSet(asList(PositiveInteger));

    private NavigableMap<DataType, Range<?>> minmaxes = new TreeMap<>();

    private NavigableMap<DataType, Set<String>> enumerations = new TreeMap<>();

    @Test
    public void testEmptyMetadata() {
        assertTrue(codebook(emptyMetadata).getVariables().isEmpty());
    }

    @Before
    public void setUp() {
        when(metadata.headerNames()).thenReturn(asList("HEADER"));
        when(metadata.fieldTypes()).thenReturn(asList(datatypes));
        when(metadata.minMaxes()).thenReturn(asList(minmaxes));
        when(metadata.enumeratedValues()).thenReturn(asList(enumerations));
        enumerations.put(PositiveInteger, newHashSet(""));
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
        minmaxes.put(PositiveInteger, Range.closed(1, 10));
        final Sequence<VariableType> variables = codebook(metadata).getVariables();
        assertEquals("1", variables.first().getRange().min);
        assertEquals("10", variables.first().getRange().max);
    }

    @Test
    public void testOneVariableMetadataWithNullRange() {
        minmaxes.put(Geographic, null);
        final Sequence<VariableType> variables = codebook(metadata).getVariables();
        assertTrue(variables.first().getRange() == null);
    }
}
