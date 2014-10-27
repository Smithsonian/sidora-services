
package com.asoroka.sidora.tabularmetadata;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVParser.parse;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.formats.TabularFormat;
import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.EnumeratedValuesHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.enumerations.InMemoryEnumeratedValuesHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.headers.DefaultHeaderHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.headers.HeaderHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.ranges.RangeDeterminingHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.ranges.RunningMinMaxHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.types.StrictHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.types.TypeDeterminingHeuristic;
import com.google.common.base.Function;
import com.google.common.collect.Range;

/**
 * Master entry point for the workflow. This is the class from which all parsing should initiate, via
 * {@link #getMetadata(URL)}.
 * 
 * @author ajs6f
 */
public class TabularMetadataGenerator {

    private static final Charset CHARACTER_ENCODING = UTF_8;

    private CSVFormat format = DEFAULT;

    /**
     * DefaultHeaderHeuristic value of {@code 0} indicates no limit. See {@link TabularScanner.scan(final int limit)}.
     */
    private Integer scanLimit = 0;

    private RangeDeterminingHeuristic<?> rangeStrategy = new RunningMinMaxHeuristic();

    private TypeDeterminingHeuristic<?> typeStrategy = new StrictHeuristic();

    private EnumeratedValuesHeuristic<?> enumStrategy = new InMemoryEnumeratedValuesHeuristic();

    private HeaderHeuristic<?> headerStrategy = new DefaultHeaderHeuristic();

    /**
     * The main entry point to application workflow.
     * 
     * @param dataUrl Where to find some tabular data.
     * @return The results of metadata extraction.
     * @throws IOException
     */
    public TabularMetadata getMetadata(final URL dataUrl) throws IOException {
        // attempt to extract header names
        // TODO allow a HeaderHeuristic to use more information than the first line of data
        final List<String> headerNames;
        try (final CSVParser headerParser = parse(dataUrl, CHARACTER_ENCODING, format)) {
            final CSVRecord firstLine = headerParser.iterator().next();
            for (final String field : firstLine) {
                headerStrategy.addValue(field);
            }
            final boolean hasHeaders = headerStrategy.isHeader();
            headerStrategy.reset();
            format = hasHeaders ? format.withHeader() : format;
            headerNames = hasHeaders ? newArrayList(firstLine) : emptyHeaders(firstLine.size());
        }

        final List<TypeDeterminingHeuristic<?>> typeStrategies;
        final List<RangeDeterminingHeuristic<?>> rangeStrategies;
        final List<EnumeratedValuesHeuristic<?>> enumStrategies;

        // scan values up to the limit
        final TabularScanner scanner;
        try (final CSVParser parser = parse(dataUrl, CHARACTER_ENCODING, format)) {
            scanner = new TabularScanner(parser, typeStrategy, rangeStrategy, enumStrategy);
            scanner.scan(scanLimit);
        }
        typeStrategies = scanner.getTypeStrategies();
        rangeStrategies = scanner.getRangeStrategies();
        enumStrategies = scanner.getEnumStrategies();

        // extract the results for each field
        final List<SortedSet<DataType>> columnTypes = transform(typeStrategies, extractType);
        final List<Map<DataType, Range<?>>> minMaxes = transform(rangeStrategies, extractMinMax);
        final List<Map<DataType, Set<String>>> enumValues = transform(enumStrategies, extractEnumValues);

        return new TabularMetadata(headerNames, columnTypes, minMaxes, enumValues);
    }

    private static final Function<TypeDeterminingHeuristic<?>, SortedSet<DataType>> extractType =
            new Function<TypeDeterminingHeuristic<?>, SortedSet<DataType>>() {

                @Override
                public SortedSet<DataType> apply(final TypeDeterminingHeuristic<?> strategy) {
                    return strategy.typesAsLikely();
                }
            };

    private static final Function<RangeDeterminingHeuristic<?>, Map<DataType, Range<?>>> extractMinMax =
            new Function<RangeDeterminingHeuristic<?>, Map<DataType, Range<?>>>() {

                @Override
                public Map<DataType, Range<?>> apply(final RangeDeterminingHeuristic<?> strategy) {
                    return strategy.getRanges();
                }
            };

    private static final Function<EnumeratedValuesHeuristic<?>, Map<DataType, Set<String>>> extractEnumValues =
            new Function<EnumeratedValuesHeuristic<?>, Map<DataType, Set<String>>>() {

                @Override
                public Map<DataType, Set<String>> apply(final EnumeratedValuesHeuristic<?> strategy) {
                    return strategy.getEnumeratedValues();
                }
            };

    private static final List<String> emptyHeaders(final int numFields) {
        final List<String> headers = new ArrayList<>(numFields);
        for (int i = 0; i <= numFields; i++) {
            headers.add("Variable " + i);
        }
        return headers;
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
