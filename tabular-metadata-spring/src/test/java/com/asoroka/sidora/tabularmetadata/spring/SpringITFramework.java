
package com.asoroka.sidora.tabularmetadata.spring;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static com.google.common.collect.ImmutableList.builder;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.SortedSet;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

/**
 * Framework for running Spring integration tests.
 * 
 * @author ajs6f
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class SpringITFramework {

    protected static final File testDataDir = new File("src/test/resources/test-data");

    protected static URL getTestFile(final String fileName) throws MalformedURLException {
        return new File(testDataDir, fileName).toURI().toURL();
    }

    @Inject
    protected TabularMetadataGenerator testGenerator;

    public TabularMetadata testFile(final URL testFile, final List<DataType> expectedDatatypes,
            final Range<?> minMaxes, final DataType expectedDataTypeForRangeTest)
            throws IOException {
        final TabularMetadata result = testGenerator.getMetadata(testFile);
        assertEquals("Got incorrect column types!", expectedDatatypes, getFirstElements(result.fieldTypes));
        assertEquals("Got wrong range for a field!", minMaxes, result.minMaxes.get(2).get(
                expectedDataTypeForRangeTest));
        return result;
    }

    protected static Range<Integer> getIntRange() {
        return Range.closed(56, 23423);
    }

    protected static Range<Float> getFloatRange() {
        return Range.closed(56F, 23423F);
    }

    protected static Range<String> getStringRange() {
        return Range.closed("0056", "SERIAL NUMBER");
    }

    protected static String testFileSimple = "simple.csv";

    protected static String testFileSlightlyLessSimple = "slightlysimple.csv";

    protected static final List<DataType> SIMPLE_TYPES = asList(String, String, PositiveInteger);

    protected static final List<DataType> SLIGHTLY_SIMPLE_TYPES = asList(String, String, Decimal);

    protected static final List<DataType> STRING_TYPES = asList(String, String, String);

    private static <T> List<T> getFirstElements(final Iterable<SortedSet<T>> inputs) {
        final ImmutableList.Builder<T> b = builder();
        for (final SortedSet<T> s : inputs) {
            b.add(s.first());
        }
        return b.build();
    }

}
