
package com.asoroka.sidora.tabularmetadata.spring;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.asoroka.sidora.tabularmetadata.CsvMetadata;

@ContextConfiguration("/spring-xml/operation-with-specified-header-strategy.xml")
public class OperationWithSpecifiedHeaderStrategyIT extends SpringITFramework {

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        final CsvMetadata result = testSimpleFile(getTestFile(testFileSimple), STRING_TYPES, getStringRange());
        assertEquals("Found header names when we should not have!", asList(), result.headerNames());
    }

}
