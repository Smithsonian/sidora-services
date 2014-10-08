/**
 * 
 */

package com.asoroka.sidora.csvmetadata.spring;

import static com.asoroka.sidora.csvmetadata.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.csvmetadata.datatype.DataType.String;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.asoroka.sidora.csvmetadata.CsvMetadata;
import com.asoroka.sidora.csvmetadata.CsvMetadataGenerator;
import com.google.common.collect.Range;

/**
 * @author ajs6f
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class IT {

    protected static final File testDataDir = new File("src/test/resources/test-data");

    @Inject
    protected CsvMetadataGenerator testGenerator;

    public void testSimpleFile(final File testFile) throws MalformedURLException, IOException {
        final CsvMetadata result = testGenerator.getMetadata(testFile.toURI().toURL());
        assertEquals("Got incorrect column types!", asList(String, String, PositiveInteger), result.columnTypes());
        assertEquals("Got wrong range for a field!", Range.closed(56, 23423), result.minMaxes().get(2));
    }
}
