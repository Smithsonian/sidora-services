/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.sidora.tabular.metadata.generator;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterators.peekingIterator;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.iterate;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVParser.parse;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.inject.Inject;

import edu.si.sidora.tabular.metadata.generator.datatype.DataType;
import edu.si.sidora.tabular.metadata.generator.formats.TabularFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Range;

import edu.si.sidora.tabular.metadata.generator.TabularMetadata.Ratio;
import edu.si.sidora.tabular.metadata.generator.heuristics.Heuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.enumerations.EnumeratedValuesHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.enumerations.LimitedEnumeratedValuesHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.headers.DefaultHeaderHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.headers.HeaderHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.ranges.RangeDeterminingHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.ranges.RunningMinMaxHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.types.StrictHeuristic;
import edu.si.sidora.tabular.metadata.generator.heuristics.types.TypeDeterminingHeuristic;

/**
 * Master entry point for this API. This is the class from which all parsing should initiate, via
 * {@link #getMetadata(URL)} or {@link #getMetadata(URL, Boolean)}.
 * 
 * @author A. Soroka
 */
public class TabularMetadataGenerator {

	private static final Charset CHARACTER_ENCODING = UTF_8;

	private CSVFormat format = DEFAULT;

	/**
	 * Default value of {@code 0} indicates no limit. See {@link TabularScanner.scan(final int limit)}.
	 */
	private Integer scanLimit = 0;

	private RangeDeterminingHeuristic<?> rangeStrategy = new RunningMinMaxHeuristic();

	private TypeDeterminingHeuristic<?> typeStrategy = new StrictHeuristic();

	private EnumeratedValuesHeuristic<?> enumStrategy = new LimitedEnumeratedValuesHeuristic();

	private HeaderHeuristic<?> headerStrategy = new DefaultHeaderHeuristic();

	private static final Logger log = getLogger(TabularMetadataGenerator.class);

	/**
	 * The main entry point to application workflow.
	 * 
	 * @param dataUrl Where to find some tabular data.
	 * @param withHeaders whether this tabular data has a header row
	 * @return The results of metadata extraction.
	 * @throws IOException
	 */
	public TabularMetadata getMetadata(final URL dataUrl, final Boolean withHeaders) throws IOException {
		try (final CSVParser csvParser = parse(dataUrl, CHARACTER_ENCODING, format)) {
			final PeekingIterator<CSVRecord> parser = peekingIterator(csvParser.iterator());
			// TODO allow a HeaderHeuristic to use more information than the
			// first line of data
			final CSVRecord firstLine = parser.peek();
			final boolean hasHeaders;
			if (withHeaders == null) {
				log.debug("Checking for the existence of headers.");
				for (final String field : firstLine) {
					headerStrategy.accept(field);
				}
				hasHeaders = headerStrategy.results();
				headerStrategy.reset();
			} else {
				hasHeaders = withHeaders;
				log.debug("Accepted information that headers is {}.", hasHeaders);
			}
			final List<String> headerNames;
			if (hasHeaders) {
				headerNames = newArrayList(firstLine);
				log.debug("Found headers: {}", headerNames);
				if (parser.hasNext()) parser.next();
			} else {
				headerNames = emptyHeaders(firstLine.size());
				log.debug("Found no headers.");
			}
			// scan values up to the limit
			final TabularScanner scanner = new TabularScanner(parser, typeStrategy, rangeStrategy, enumStrategy);
			scanner.scan(scanLimit);

			final List<TypeDeterminingHeuristic<?>> typeStrategies = scanner.getTypeStrategies();
			final List<RangeDeterminingHeuristic<?>> rangeStrategies = scanner.getRangeStrategies();
			final List<EnumeratedValuesHeuristic<?>> enumStrategies = scanner.getEnumStrategies();

			// extract the results for each field
			final List<DataType> columnTypes = typeStrategies.stream().map(Heuristic::results).collect(toList());
			final List<Map<DataType, Range<?>>> minMaxes = rangeStrategies.stream().map(Heuristic::results)
					.collect(toList());
			final List<Map<DataType, Set<String>>> enumValues = enumStrategies.stream().map(Heuristic::results)
					.collect(toList());
			final List<Ratio> valuesSeen = typeStrategies.stream()
					.map(h -> new Ratio(h.valuesSeen() - h.parseableValuesSeen(), h.valuesSeen())).collect(toList());
			return new TabularMetadata(headerNames, valuesSeen, columnTypes, minMaxes, enumValues);
		} catch (final NoSuchElementException e) {
			throw new EmptyDataFileException(dataUrl + " has no data in it!");
		}
	}

	/**
	 * Another, simpler entry point to application workflow.
	 * 
	 * @param dataUrl Where to find some tabular data.
	 * @return The results of metadata extraction.
	 * @throws IOException
	 */
	public TabularMetadata getMetadata(final URL dataUrl) throws IOException {
		return getMetadata(dataUrl, null);
	}

	private static final List<String> emptyHeaders(final int numFields) {
		return iterate(1, i -> i + 1).limit(numFields).map(i -> "Variable " + i).collect(toList());
	}

	/**
	 * @param scanLimit A limit to the number of rows to scan, including any header row that may be present. {@code 0}
	 *        indicates no limit.
	 */
	public void setScanLimit(final Integer scanLimit) {
		this.scanLimit = scanLimit;
	}

	/**
	 * @param strategy The header recognition strategy to use.
	 */
	@Inject
	public <H extends HeaderHeuristic<?>> void setHeaderStrategy(final H strategy) {
		this.headerStrategy = strategy;
	}

	/**
	 * @param format The tabular data format to expect.
	 */
	@Inject
	public void setFormat(final TabularFormat format) {
		this.format = format.get();
	}

	/**
	 * @param strategy The type recognition strategy to use.
	 */
	@Inject
	public void setTypeStrategy(final TypeDeterminingHeuristic<?> strategy) {
		this.typeStrategy = strategy;
	}

	/**
	 * @param strategy The range recognition strategy to use.
	 */
	@Inject
	public void setRangeStrategy(final RangeDeterminingHeuristic<?> strategy) {
		this.rangeStrategy = strategy;
	}

	/**
	 * @param strategy The enumerated-values recognition strategy to use.
	 */
	@Inject
	public void setEnumStrategy(final EnumeratedValuesHeuristic<?> strategy) {
		this.enumStrategy = strategy;
	}
}
