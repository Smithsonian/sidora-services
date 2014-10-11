
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
import java.util.SortedSet;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.asoroka.sidora.tabularmetadata.formats.TabularFormat;
import com.asoroka.sidora.tabularmetadata.heuristics.DataTypeHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic;
import com.asoroka.sidora.tabularmetadata.heuristics.HeaderHeuristic.Default;
import com.asoroka.sidora.tabularmetadata.heuristics.StrictHeuristic;
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

    private HeaderHeuristic<?> headerStrategy = new Default();

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
            final boolean hasHeaders = headerStrategy.apply(firstLine);
            format = hasHeaders ? format.withHeader() : format;
            headerNames = hasHeaders ? newArrayList(firstLine) : emptyHeaders;
        }
        // scan values up to the limit
        final List<DataTypeHeuristic<?>> strategies;
        try (final CSVParser parser = parse(dataUrl, CHARACTER_ENCODING, format)) {
            final TabularScanner scanner = new TabularScanner(parser, strategy);
            scanner.scan(scanLimit);
            strategies = scanner.getStrategies();
        }
        // extract the results for each field
        final List<SortedSet<DataType>> columnTypes = transform(strategies, extractType);
        final List<Range<?>> minMaxes = transform(strategies, extractMinMax);

        return new TabularMetadata(headerNames, columnTypes, minMaxes);
    }

    private static final Function<DataTypeHeuristic<?>, SortedSet<DataType>> extractType =
            new Function<DataTypeHeuristic<?>, SortedSet<DataType>>() {

                @Override
                public SortedSet<DataType> apply(final DataTypeHeuristic<?> strategy) {
                    return strategy.typesAsLikely();
                }
            };

    private static final Function<DataTypeHeuristic<?>, Range<?>> extractMinMax =
            new Function<DataTypeHeuristic<?>, Range<?>>() {

                @Override
                public Range<?> apply(final DataTypeHeuristic<?> strategy) {
                    return strategy.getRange();
                }
            };

    private static final List<String> emptyHeaders = emptyList();

    /**
     * @param scanLimit A limit to the number of rows to scan. {@code 0} indicates no limit.
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
    public void setStrategy(final DataTypeHeuristic<?> strategy) {
        this.strategy = strategy;
    }
}
