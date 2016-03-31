/*
 * Copyright 2015-2016 Smithsonian Institution.
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

package edu.si.sidora.tabularmetadata.testframework;

import static edu.si.sidora.tabularmetadata.datatype.DataType.datatypes;
import static edu.si.sidora.tabularmetadata.testframework.TestUtilities.randomValues;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.iterate;

import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

public class RowsOfRandomValuesForAllTypesSupplier extends ParameterSupplier {

	@Override
	public List<PotentialAssignment> getValueSources(final ParameterSignature sig) {

		final RowsOfRandomValuesForAllTypes metadata = sig.getAnnotation(RowsOfRandomValuesForAllTypes.class);
		final short valuesPerType = metadata.valuesPerType();
		final short numRowsPerType = metadata.numRowsPerType();

		return datatypes().stream()
				.flatMap(type -> iterate(0, i -> i + 1).limit(numRowsPerType)
						.map(i -> randomValues(type, valuesPerType)))
				.map(from -> PotentialAssignment.forValue(null, from)).collect(toList());

	}
}
