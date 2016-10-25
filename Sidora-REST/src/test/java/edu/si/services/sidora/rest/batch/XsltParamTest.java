/*
 * Copyright 2015-2016 Smithsonian Institution.
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

package edu.si.services.sidora.rest.batch;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jbirkhimer
 */
public class XsltParamTest extends CamelTestSupport {

    //Manifest for tests
    private File manifestFile = new File("src/test/resources/metadataWithTitle.xml");

    private String manifest;
    private String xsdFile, schFile;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Temp directories created for testing the camel validation route
    private static File tempInputDirectory;

    private Namespaces ns = new Namespaces("mods", "http://www.loc.gov/mods/v3");

    /**
     * Sets up the Temp directories used by the
     * route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        //Create and Copy the Input dir xslt, etc. files used in the route
        tempInputDirectory = new File("Input");
        if(!tempInputDirectory.exists()){
            tempInputDirectory.mkdir();
        }

        //The Location of the Input dir in the project
        File inputSrcDirLoc = new File("../Routes/Camera Trap/Input");

        //Copy the Input src files so the camel route can find them
        FileUtils.copyDirectory(inputSrcDirLoc, tempInputDirectory);
    }

    /**
     * Clean up the temp directories after tests are finished
     * @throws IOException
     */
    @AfterClass
    public static void teardown() throws IOException {
        if(tempInputDirectory.exists()){
            FileUtils.deleteDirectory(tempInputDirectory);
        }
    }

    //@Test
    public void ManifestSchematromValidationTests() throws Exception {
        manifest = FileUtils.readFileToString(manifestFile);
//        xsdFile = "Manifest.xsd";
//        schFile = "Manifest2014.sch";

        runManifestSchematronValidationTest("title", "TestTitle(1)", "SUCCESS"); //title text value change
        //runManifestSchematronValidationTest("title", "FAILED"); //title with empty text value
        //runManifestSchematronValidationTest("SUCCESS"); //Valid Manifest
    }

    /**
     * Manifest Schema Validation Testing Route
     * @throws Exception
     */
    private void runManifestSchematronValidationTest(String validationStatus) throws Exception {
        runManifestSchematronValidationTest(null, null, validationStatus);
    }

    /**
     * Manifest Schema Validation Testing Route
     * @param fieldToTest the manifest node being tested
     * @throws Exception
     */
    private void runManifestSchematronValidationTest(String fieldToTest, String validationStatus) throws Exception {
        runManifestSchematronValidationTest(fieldToTest, null, validationStatus);
    }

    /**
     * Manifest Schema Validation Testing Route
     * @param titleNode the manifest node being tested
     * @param titleLabel the new value for the node being tested
     * @throws Exception
     */
    public void runManifestSchematronValidationTest(String titleNode, String titleLabel, String validationStatus) throws Exception {

        headers = new HashMap<>();
        if (titleNode != null) {
            headers.put("titleNode", titleNode);
            headers.put("titleLabel", titleLabel);
        }
//        headers.put("xsdFile", xsdFile);
//        headers.put("schFile", schFile);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.setRetainLast(1);
        mockEndpoint.setMinimumExpectedMessageCount(1);
        //mockEndpoint.expectedHeaderReceived("CamelSchematronValidationStatus", validationStatus);
        //mockEndpoint.expectedHeaderReceived("validationStatus", validationStatus);
        mockEndpoint.expectedHeaderReceived("xsltResultTitle", titleLabel);
        //mockEndpoint.expectedBodiesReceived(FileUtils.readFileToString(new File("src/test/resources/test-data/xslt-expected-MODS.xml")));


        template.sendBodyAndHeaders("direct:start", manifest, headers);

        assertEquals(
                FileUtils.readFileToString(new File("src/test/resources/test-data/xslt-expected-MODS.xml")),
                mockEndpoint.getExchanges().get(0).getIn().getBody()
        );

        assertMockEndpointsSatisfied();

        resetMocks();
        //mockEndpoint.reset();
    }

    /**
     * Registering the velocityToolsHandler
     *
     * @return JndiRegistry
     * @throws Exception
     */
    /*@Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("velocityToolsHandler", edu.si.services.beans.velocityToolsHandler.VelocityToolsHandler.class);

        return jndi;
    }*/

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // logging configuration for tracer and formatter
                //Tracer tracer = new Tracer();
                //tracer.setLogLevel(LoggingLevel.WARN);
                //tracer.setTraceOutExchanges(true);
                //DefaultTraceFormatter formatter = new DefaultTraceFormatter();
                //formatter.setShowOutBody(true);
                //formatter.setShowOutBodyType(true);
                //formatter.setShowShortExchangeId(true);
                //formatter.setMaxChars(500);
                //tracer.setFormatter(formatter);
                //getContext().addInterceptStrategy(tracer);

                /**
                 * Generic Route used for AdviceWithRouteBuilder
                 */
                from("direct:start")
                        .routeId("testRoute")
                        .choice()
                        .when(header("titleNode"))
                        .log(LoggingLevel.INFO, "=======(0)======= Testing Manifest with \"${header.titleLabel}\" value for: ${header.titleNode}")
                                //use and xslt transform to modify the manifest for testing purposes
                        .to("xslt:file:Input/xslt/BatchProcess_ManifestResource.xsl?saxon=true")
                        .log(LoggingLevel.INFO, "=======(1)======= Generated Manifest:\n${body}")
                                //.endChoice()
                        .otherwise()
                        .log(LoggingLevel.INFO, "=======(0)======= Testing Unmodified Manifest")
                        .log(LoggingLevel.DEBUG, "=======(1)======= Unmodified manifest being tested:\n${body}")
                                //.endChoice()
                        .end()

                        //TODO: Create xsd and schematron validation tests
                        /*        //check the manifest using xsd schema
                        .toD("validator:file:Input/schemas/${header.xsdFile}")
                                //check the manifest using schematron
                        .toD("schematron:file:Input/schemas/${header.schFile}")
                        .log(LoggingLevel.INFO, "=======(2)======= Schematron Validation Status - ${header.CamelSchematronValidationStatus}")
                        .choice()
                        .when(simple("${header.CamelSchematronValidationStatus} == 'FAILED'"))
                        .log(LoggingLevel.DEBUG, "*************************************************************\n"
                                + "Schematron Validation Status - ${header.CamelSchematronValidationStatus}\n"
                                + "----------------------------------------------------------------\n"
                                + "Schematron Validation Report -\n ${header.CamelSchematronValidationReport}\n"
                                + "*************************************************************\n")
                        .end()*/
                        .setHeader("xsltResultTitle").xpath("//mods:title/text()", ns).convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "Result: ${body}")
                        .to("mock:result");
            }
        };
    }

    /*@Override
    public boolean isUseDebugger() {
        // must enable debugger
        return true;
    }

    @Override
    protected void debugBefore(Exchange exchange, org.apache.camel.Processor processor, ProcessorDefinition<?> definition, String id, String label) {
        // this method is invoked before we are about to enter the given processor
        // from your Java editor you can just add a breakpoint in the code line below
        log.info("Before " + definition + " with body " + exchange.getIn().getBody());
    }

    @Override
    protected void debugAfter(Exchange exchange, org.apache.camel.Processor processor, ProcessorDefinition<?> definition, String id, String label, long timeTaken) {
        log.info("After " + definition + " with body " + exchange.getIn().getBody());
    }*/
}
