
package com.asoroka.sidora.tabularmetadata.spring;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-xml/operation-with-fraction-heuristic.xml")
public class OperationWithFractionHeuristicIT extends SpringITFramework {

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        testSimpleFile(getTestFile(testFileSimple), SIMPLE_TYPES, getIntRange());
    }

    @Test
    public void testWithFractionallySimpleData() throws MalformedURLException, IOException {
        testSimpleFile(getTestFile(testFileSlightlyLessSimple), SIMPLE_TYPES, getIntRange());
    }

}
