
package com.asoroka.sidora.tabularmetadata.spring;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.asoroka.sidora.tabularmetadata.TabularMetadata;

@ContextConfiguration("/spring-xml/operation-with-specified-header-strategy.xml")
public class OperationWithSpecifiedHeaderStrategyIT extends SpringITFramework {

    private static String testFileSimpleWithCrazyHeaders = "simple-with-crazy-headers.csv";

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        final TabularMetadata result = testSimpleFile(getTestFile(testFileSimple), STRING_TYPES, getStringRange());
        assertEquals("Found header names when we should not have!", asList(), result.headerNames());
    }

    @Test
    public void testWithDataWithHeadersMatching() throws MalformedURLException, IOException {
        final TabularMetadata result =
                testSimpleFile(getTestFile(testFileSimpleWithCrazyHeaders), SIMPLE_TYPES, getIntRange());
        assertEquals("Didn't find header names when we should have!", asList("NEVER MATCH1", "NEVER MATCH2",
                "NEVER MATCH3"), result.headerNames());
    }

}
