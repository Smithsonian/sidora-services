
package com.asoroka.sidora.tabularmetadata;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.emptyList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVParser.parse;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.formats.TabularFormat;
import com.asoroka.sidora.tabularmetadata.heuristics.DataTypeHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.StrictHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic.Default;
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
     * Default value of {@code 0} indicates no limit. See {@link TabularScanner.scan(final int limit)}.
     */
    private Integer scanLimit = 0;

    private DataTypeHeuristic<?> strategy = new StrictHeuristic();

    private HeaderHeuristic headerStrategy = new Default();

    /**
     * Workflow.
     * 
     * @param csvUrl
     * @return The results of metadata extraction.
     * @throws IOException
     */
    public TabularMetadata getMetadata(final URL csvUrl) throws IOException {
        // attempt to extract header names
        final List<String> headerNames;
        try (final CSVParser headerParser = parse(csvUrl, CHARACTER_ENCODING, format)) {
            final CSVRecord firstLine = headerParser.iterator().next();
            final boolean hasHeaders = headerStrategy.apply(firstLine);
            format = hasHeaders ? format.withHeader() : format;
            headerNames = hasHeaders ? newArrayList(firstLine) : emptyHeaders;
        }
        // scan values up to the limit
        final List<DataTypeHeuristic<?>> strategies;
        try (final CSVParser parser = parse(csvUrl, CHARACTER_ENCODING, format)) {
            final TabularScanner scanner = new TabularScanner(parser, strategy);
            scanner.scan(scanLimit);
            strategies = scanner.getStrategies();
        }
        // extract the results for each field
        final List<DataType> columnTypes = transform(strategies, extractType);
        final List<Range<?>> minMaxes = transform(strategies, extractMinMax);

        return new TabularMetadata(headerNames, columnTypes, minMaxes);
    }

    @TypeRenaming
    private static interface Extractor<T> extends Function<DataTypeHeuristic<?>, T> {
    }

    private static final Extractor<DataType> extractType = new Extractor<DataType>() {

        @Override
        public DataType apply(final DataTypeHeuristic<?> strategy) {
            return strategy.mostLikelyType();
        }
    };

    private static final Extractor<Range<?>> extractMinMax = new Extractor<Range<?>>() {

        @Override
        public Range<?> apply(final DataTypeHeuristic<?> strategy) {
            return strategy.getRange();
        }
    };

    private static final List<String> emptyHeaders = emptyList();

    /**
     * @param scanLimit A limit to the number of rows to scan.
     */
    public void setScanLimit(final Integer scanLimit) {
        this.scanLimit = scanLimit;
    }

    /**
     * @param strategy The header recognition strategy to use.
     */
    @Inject
    public void setHeaderStrategy(final HeaderHeuristic strategy) {
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
    public void setStrategy(final DataTypeHeuristic<?> strategy) {
        this.strategy = strategy;
    }
}
