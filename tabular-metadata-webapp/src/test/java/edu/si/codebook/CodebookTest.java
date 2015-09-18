/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */
package edu.si.codebook;

import static edu.si.codebook.Codebook.codebook;
import static edu.si.sidora.tabularmetadata.datatype.DataType.Geographic;
import static edu.si.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Range;

import edu.si.codebook.Codebook.VariableType;
import edu.si.codebook.Codebook.VariableType.RangeType;
import edu.si.sidora.tabularmetadata.TabularMetadata;
import edu.si.sidora.tabularmetadata.TabularMetadata.Ratio;
import edu.si.sidora.tabularmetadata.datatype.DataType;

@RunWith(MockitoJUnitRunner.class)
public class CodebookTest {

	@Mock private TabularMetadata emptyMetadata, metadata;

	private static final Ratio testRatio = new Ratio(1, 10);

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
		enumerations.put(TEST_DATATYPE, singleton(""));
	}

	@Test
	public void testOneVariableMetadata() {
		minmaxes.put(PositiveInteger, Range.all());
		final List<VariableType> variables = codebook(metadata).getVariables();
		assertEquals("HEADER", variables.get(0).name);
		assertEquals(singleton(""), variables.get(0).enumeration);
		assertEquals(PositiveInteger.uri, variables.get(0).type);
		assertNull(variables.get(0).getRange().max);
		assertNull(variables.get(0).getRange().min);
		assertEquals(1, variables.size());
	}

	@Test
	public void testOneVariableMetadataWithRange() {
		minmaxes.put(TEST_DATATYPE, Range.closed(1, 10));
		final VariableType variable = codebook(metadata).getVariables().get(0);
		final RangeType range = variable.getRange();
		assertEquals("1", range.min);
		assertEquals("10", range.max);
	}

	@Test
	public void testOneVariableMetadataWithNullRange() {
		minmaxes.remove(Geographic);
		final List<VariableType> variables = codebook(metadata).getVariables();
		assertNull(variables.get(0).getRange());
	}
}
