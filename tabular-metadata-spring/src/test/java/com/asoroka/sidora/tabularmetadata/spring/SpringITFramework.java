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


package com.asoroka.sidora.tabularmetadata.spring;

import static com.asoroka.sidora.tabularmetadata.datatype.DataType.Decimal;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.PositiveInteger;
import static com.asoroka.sidora.tabularmetadata.datatype.DataType.String;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;
import com.asoroka.sidora.tabularmetadata.datatype.DataType;
import com.google.common.collect.Range;

/**
 * Framework for running Spring integration tests.
 * 
 * @author A. Soroka
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class SpringITFramework {

    protected static final File testDataDir = new File("src/test/resources/test-data");

    protected static URL getTestFile(final String fileName) throws MalformedURLException {
        return new File(testDataDir, fileName).toURI().toURL();
    }

    @Inject
    protected TabularMetadataGenerator testGenerator;

    public TabularMetadata testFile(final URL testFile, final List<DataType> expectedMostLikelyDatatypes,
            final Range<?> minMaxes, final DataType expectedDataTypeForRangeTest)
            throws IOException {
        final TabularMetadata result = testGenerator.getMetadata(testFile);
        assertEquals("Got incorrect column types!", expectedMostLikelyDatatypes, result.fieldTypes());
        assertEquals("Got wrong range for a field!", minMaxes, result.minMaxes().get(2).get(
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

}
