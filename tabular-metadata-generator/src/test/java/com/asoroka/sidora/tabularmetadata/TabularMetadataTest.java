
package com.asoroka.sidora.tabularmetadata;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

@RunWith(MockitoJUnitRunner.class)
public class TabularMetadataTest {

    @Mock
    private List<String> mockHeaderNames;

    @Mock
    private List<SortedSet<DataType>> mockTypes;

    @Mock
    private List<Map<DataType, Range<?>>> mockRanges;

    @Test
    public void testContainerAction() {
        final TabularMetadata testMetadata = new TabularMetadata(mockHeaderNames, mockTypes, mockRanges);
        assertEquals(mockHeaderNames, testMetadata.headerNames());
        assertEquals(mockTypes, testMetadata.fieldTypes());
        assertEquals(mockRanges, testMetadata.minMaxes());
    }

}
