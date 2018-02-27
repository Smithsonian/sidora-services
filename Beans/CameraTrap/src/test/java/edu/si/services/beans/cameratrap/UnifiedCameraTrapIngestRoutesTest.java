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

package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.SetBodyDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the Unified Camera Trap Ingest Pipeline
 * TODO: Add more tests
 * @author jbirkhimer
 */
public class UnifiedCameraTrapIngestRoutesTest extends CT_BlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");

    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = KARAF_HOME + "/UnifiedManifest-TestFiles";

    //Camera Trap Deployment Info for testing
    private String camelFileParent = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint to be used for assertions
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File(testDataDir + "/scbi_unified_stripped_p125d18981/deployment_manifest.xml");
    private String manifest;
    File deploymentZip;

    private String processDirPath;
    private String processDoneDirPath;
    private String processErrorDirPath;

    /**
     * Override this method, and return the location of our Blueprint XML file to be used for testing.
     * The actual camera trap route that the maven lifecycle phase process-test-resources executes
     * to copy test resources to output folder target/test-classes.
     */
    @Override
    protected String getBlueprintDescriptor() {
        //use the production route for testing that the pom copied into the test resources
        return "Routes/unified-camera-trap-route.xml";
    }

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        disableJMX();

        //Store the Deployment Manifest as string to set the camel ManifestXML header
        manifest = FileUtils.readFileToString(manifestFile);

        //Initialize the expected camel headers
        headers = new HashMap<>();
        headers.put("CamelFileParent", camelFileParent);
        headers.put("ManifestCameraDeploymentId", ManifestCameraDeploymentId);
        headers.put("ManifestXML", String.valueOf(manifest));
        headers.put("ValidationErrors", "ValidationErrors");
        headers.put("ProjectPID", "test:0000");
        headers.put("SitePID", "test:0000");

        super.setUp();

        processDirPath = getExtra().getProperty("si.ct.uscbi.process.dir.path");
        processDoneDirPath = getExtra().getProperty("si.ct.uscbi.process.done.dir.path");
        processErrorDirPath = getExtra().getProperty("si.ct.uscbi.process.error.dir.path");

        deleteDirectory(processDirPath);
        deleteDirectory(processDoneDirPath);
        deleteDirectory(processErrorDirPath);

        //Modify the default error handler so that we can send failed exchanges to mock:result for assertions
        // Sending to dead letter does not seem to work as expected for this
        context.setErrorHandlerBuilder(new DefaultErrorHandlerBuilder().onPrepareFailure(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                template.send("mock:result", exchange);
            }
        }));
    }

    /**
     * Testing the headers created in the UnifiedCameraTrapAddImageResource route
     * @throws Exception
     * TODO: Add more test coverage for this route
     */
    @Test
    public void addImageResource_Test() throws Exception {
        //RouteId and endpoint uri
        String routeId = "UnifiedCameraTrapAddImageResource";
        String routeURI = "direct:addImageResource";

        //The expected header values
        String imageidHeaderExpected = "d18981s1i1";
        String imageSequenceIDHeaderExpected = "d18981s1";
        String imageSequenceIndexHeaderExpected = "1";
        String imageSequenceCountHeaderExpected = "2";

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition(routeId).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //add the mock:result endpoint and stop the route after the headers we are testing have been created
                weaveByToString(".*reader:file.*").before().to("mock:result").stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("imageid", imageidHeaderExpected);
        mockEndpoint.expectedHeaderReceived("ImageSequenceID", imageSequenceIDHeaderExpected);
        mockEndpoint.expectedHeaderReceived("ImageSequenceIndex", imageSequenceIndexHeaderExpected);
        mockEndpoint.expectedHeaderReceived("ImageSequenceCount", imageSequenceCountHeaderExpected);

        template.sendBodyAndHeaders(routeURI, "d18981s1i1.JPG", headers);

        //get the mockEndpoint header values for assertions
        String imageidHeaderResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("imageid", String.class);
        String imageSequenceIDHeaderResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("ImageSequenceID", String.class);
        String imageSequenceIndexHeaderResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("ImageSequenceIndex", String.class);
        String imageSequenceCountHeaderResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("ImageSequenceCount", String.class);

        //Assertions
        assertEquals("imageid header assertEquals failed!", imageidHeaderExpected, imageidHeaderResult);
        assertEquals("ImageSequenceID header assertEquals failed!", imageSequenceIDHeaderExpected, imageSequenceIDHeaderResult);
        assertEquals("ImageSequenceIndex header assertEquals failed!", imageSequenceIndexHeaderExpected, imageSequenceIndexHeaderResult);
        assertEquals("ImageSequenceCount header assertEquals failed!", imageSequenceCountHeaderExpected, imageSequenceCountHeaderResult);

        assertMockEndpointsSatisfied();

    }

    /**
     * Testing the MODS datastream is created and has the correct field values in the UnifiedCameraTrapAddMODSDataStream route
     * @throws Exception
     */
    @Test
    public void addMODSDatastream_Test() throws Exception {
        //RouteId and endpoint uri
        String routeId = "UnifiedCameraTrapAddMODSDataStream";
        String routeURI = "direct:addMODSDataStream";

        File MODS_DatastreamTestFile = new File(testDataDir + "/DatastreamTestFiles/MODS/valid_MODS.xml");

        String MODS_DatastreamExpected = FileUtils.readFileToString(MODS_DatastreamTestFile);

        //the header thats used for the Unified_ManifestImage.xsl param
        headers.put("imageid", "d18981s1i1");

        //manually setting the FITSCreatedDate header
        headers.put("FITSCreatedDate", "2016:02:13 12:11:26");

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition(routeId).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //Remove the setheader definition for FITSCreatedDate we are manually passing this header in already
                //weaveByType(SetHeaderDefinition.class).selectFirst().remove();
                //set the mockEndpoint result and stop the route before adding the MODS datastream to fedora
                weaveByToString(".*fedora:addDatastream.*").before().log(LoggingLevel.DEBUG, "uct.test", "============ BODY ============\n${body}").to("mock:result").stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodyReceived().body().isEqualTo(MODS_DatastreamExpected);
        mockEndpoint.expectedBodiesReceived(MODS_DatastreamExpected);

        template.sendBodyAndHeaders(routeURI, "d18981s1i1.JPG", headers);

        String resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        log.debug("expectedBody:\n" + MODS_DatastreamExpected + "\nresultBody:\n" + resultBody);

        log.debug("expectedBody Type:\n" + MODS_DatastreamExpected.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result Body containing MODS datastream xml assertEquals failed!", MODS_DatastreamExpected, resultBody);

        assertMockEndpointsSatisfied();

    }
}
