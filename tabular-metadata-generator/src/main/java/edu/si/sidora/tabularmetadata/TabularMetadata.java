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

package edu.si.sidora.tabularmetadata;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Range;

import edu.si.sidora.tabularmetadata.datatype.DataType;

/**
 * A container for the results of metadata extraction on a single data file, with machinery to present those results in
 * appropriate orders.
 * 
 * @author A. Soroka
 */
public class TabularMetadata {

	private final List<String> headerNames;

	private final List<Ratio> unparseablesOverTotals;

	private final List<DataType> fieldTypes;

	private final List<Map<DataType, Range<?>>> minMaxes;

	private final List<Map<DataType, Set<String>>> enumeratedValues;

	/**
	 * @param headerNames
	 * @param fieldTypes
	 * @param minMaxes
	 * @param enumeratedValues
	 */
	public TabularMetadata(final List<String> headerNames, final List<Ratio> unparseablesOverTotals,
			final List<DataType> fieldTypes, final List<Map<DataType, Range<?>>> minMaxes,
			final List<Map<DataType, Set<String>>> enumeratedValues) {
		this.headerNames = headerNames;
		this.unparseablesOverTotals = unparseablesOverTotals;
		this.fieldTypes = fieldTypes;
		this.minMaxes = minMaxes;
		this.enumeratedValues = enumeratedValues;
	}

	/**
	 * @return A list of prospective header names.
	 */
	public List<String> headerNames() {
		return headerNames;
	}

	/**
	 * @return A list of the number of values that failed to parse as the likely type over the total number of values
	 *         seen for each field.
	 */
	public List<Ratio> unparseablesOverTotals() {
		return unparseablesOverTotals;
	}

	/**
	 * @return A list of candidates for field datatypes, one candidate set per field. Each candidate set is sorted
	 *         according to a notion of decreasing plausibility that is specific to the type-determination strategy
	 *         used.
	 */
	public List<DataType> fieldTypes() {
		return fieldTypes;
	}

	/**
	 * @return A list of maps from {@link DataType}s to {@link Range}s, with each data type mapped to the minimum and
	 *         maximum for each field, <i>if</i> that field is treated as of that type. The values of the endpoints of
	 *         the ranges, if they exist, are in the Java type value-space associated to the datatype keying that range.
	 *         The idea here is that when a given field is finally determined to be of a given type (by user action
	 *         after automatic action), the appropriate range can be looked up at that time. This is to ensure that in a
	 *         situation where the type determination strategy employed gave a wrong answer, the correct answer for
	 *         range can still be found after the type determination has been corrected.
	 * @see com.google.common.collect.Range
	 */
	public List<Map<DataType, Range<?>>> minMaxes() {
		return minMaxes;
	}

	/**
	 * @return A list (one element for each field in the input file) of maps from each possible datatype to an set of
	 *         the lexes found in that field that were parseable into that data type.
	 */
	public List<Map<DataType, Set<String>>> enumeratedValues() {
		return enumeratedValues;
	}

	@Override
	public String toString() {
		return toStringHelper(this).add("headerNames", headerNames()).add("fieldTypes", fieldTypes())
				.add("enumeratedValues", enumeratedValues()).add("minMaxes", minMaxes()).toString();
	}

	public static class Ratio {

		public final int numerator, denominator;

		public Ratio(Integer n, Integer d) {
			this.numerator = n;
			this.denominator = d;
		}
	}
}
