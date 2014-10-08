
package com.asoroka.sidora.csvmetadata.spring;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-xml/operation-with-fraction-heuristic.xml")
public class OperationWithFractionHeuristicIT extends IT {

    private static File testFileSimple = new File("src/test/resources/test-data/simple.csv");

    private static File testFileSlightlyLessSimple = new File("src/test/resources/test-data/slightlysimple.csv");

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        testInputs(testFileSimple);
    }

    @Test
    public void testWithFractionallyGoodData() throws MalformedURLException, IOException {
        testInputs(testFileSlightlyLessSimple);
    }

}
