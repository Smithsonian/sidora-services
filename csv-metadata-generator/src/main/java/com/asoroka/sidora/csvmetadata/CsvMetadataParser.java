
package com.asoroka.sidora.csvmetadata;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.emptyList;
import static org.apache.commons.csv.CSVParser.parse;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.asoroka.sidora.csvmetadata.datatype.DataType;
import com.asoroka.sidora.csvmetadata.heuristics.DataTypeHeuristic;
import com.asoroka.sidora.csvmetadata.heuristics.HeaderHeuristic;
import com.asoroka.sidora.csvmetadata.heuristics.HeaderHeuristic.Default;
import com.asoroka.sidora.csvmetadata.statistics.CsvScanner;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Range;

public class CsvMetadataParser {

    private static final Charset CHARACTER_ENCODING = UTF_8;

    private CSVFormat format;

    /**
     * Default value of {@code 0} indicates no limit. See {@link CsvScanner.scan(final int limit)}.
     */
    private Integer scanLimit = 0;

    private DataTypeHeuristic<?> strategy;

    private HeaderHeuristic headerAcceptor = new Default();

    /**
     * Default constructor.
     */
    @Inject
    public CsvMetadataParser(final Supplier<CSVFormat> format, final DataTypeHeuristic<?> strategy) {
        this.format = format.get();
        this.strategy = strategy;
    }

    public CsvMetadata getMetadata(final URL csvFile) throws IOException {

        final List<String> headerNames;
        try (final CSVParser headerParser = parse(csvFile, CHARACTER_ENCODING, format)) {
            final CSVRecord firstLine = headerParser.iterator().next();
            final boolean hasHeaders = headerAcceptor.apply(firstLine);
            format = hasHeaders ? format.withHeader() : format;
            headerNames = hasHeaders ? newArrayList(firstLine) : emptyHeaders;
        }

        final List<DataTypeHeuristic<?>> strategies;
        try (final CSVParser parser = parse(csvFile, CHARACTER_ENCODING, format)) {
            final CsvScanner scanner = new CsvScanner(parser, strategy);
            scanner.scan(scanLimit);
            strategies = scanner.getStrategies();
        }

        final List<DataType> columnTypes = transform(strategies, extractType);
        final List<Range<?>> minMaxes = transform(strategies, extractMinMax);

        return new CsvMetadata(headerNames, columnTypes, minMaxes);
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

    private List<String> emptyHeaders = emptyList();

    @Inject
    public void setScanLimit(@ScanLimit final Integer scanLimit) {
        this.scanLimit = scanLimit;
    }

    @Inject
    public void setHeaderAcceptor(final HeaderHeuristic acceptor) {
        this.headerAcceptor = acceptor;
    }
}
