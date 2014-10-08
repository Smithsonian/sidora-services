
package com.asoroka.sidora.csvmetadata.spring;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-xml/operation-with-fraction-heuristic.xml")
public class OperationWithFractionHeuristicIT extends IT {

    private static File testFileSimple = new File(testDataDir, "simple.csv");

    private static File testFileSlightlyLessSimple = new File(testDataDir, "slightlysimple.csv");

    @Test
    public void testWithSimpleData() throws MalformedURLException, IOException {
        testSimpleFile(testFileSimple);
    }

    @Test
    public void testWithFractionallySimpleData() throws MalformedURLException, IOException {
        testSimpleFile(testFileSlightlyLessSimple);
    }

}
