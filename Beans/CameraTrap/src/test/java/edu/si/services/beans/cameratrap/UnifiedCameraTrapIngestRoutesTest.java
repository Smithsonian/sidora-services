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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.readFileToString;

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
        manifest = readFileToString(manifestFile);

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
                weaveById("readImageResource").before().to("mock:result").stop();
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

        String MODS_DatastreamExpected = readFileToString(MODS_DatastreamTestFile);

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

    @Test
    public void testFitsSocketException() throws Exception {

        Integer minSocketExRedelivery = Integer.valueOf(getExtra().getProperty("min.socketEx.redeliveries"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.setAssertPeriod(1500);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SocketException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minSocketExRedelivery);
        mockResult.setAssertPeriod(1500);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .to("direct:addFITSDataStream");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapAddFITSDataStream").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new SocketException("Simulating java.net.SocketException: Connection reset");
                    }
                };

                weaveById("fitsServiceException").after().to("mock:error");
                weaveById("fitsHttpRequest").replace().process(processor);
                weaveById("fitsAddDatastream").replace().log(LoggingLevel.INFO, "Skipping Fedora addDatastream!!!").to("mock:result");

            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFileAbsolutePath", KARAF_HOME + "/UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/d18981s1i1.JPG");

        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFitsSocketExceptionAndExecFail() throws Exception {

        Integer minSocketExRedelivery = Integer.valueOf(getExtra().getProperty("min.socketEx.redeliveries"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(0); //we should not
        mockResult.setAssertPeriod(1500);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SocketException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minSocketExRedelivery);
        mockResult.setAssertPeriod(1500);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .to("direct:addFITSDataStream");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapAddFITSDataStream").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new SocketException("Simulating java.net.SocketException: Connection reset");
                    }
                };

                weaveById("fitsServiceException").after().to("mock:error");
                weaveById("fitsHttpRequest").replace().process(processor);
                weaveById("fitsAddDatastream").replace().log(LoggingLevel.INFO, "Skipping Fedora addDatastream!!!").to("mock:result");

            }
        });

        Exchange exchange = new DefaultExchange(context);
        //exchange.getIn().setHeader("CamelFileAbsolutePath", KARAF_HOME + "/UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/d18981s1i1.JPG");

        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testImageResourceFaceBlur() throws Exception {
        String manifest = readFileToString(new File(KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(KARAF_HOME + "/output/testWildLifeInsightsDeploymentIds1i3.JPG");
        mockResult.expectedFileExists(KARAF_HOME + "/staging/testWildLifeInsightsDeploymentIds1i3.JPG");

        context.getRouteDefinition("UnifiedCameraTrapAddImageResource").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("createResourcePID").replace().log("Skipping fedora create!!!");
                weaveById("addResourceOriginalVersion").replace().log("Skipping fedora addDatastream OBJ");
                weaveById("addResourceBlurVersion").replace().log("Skipping fedora addDatastream OBJ");
                weaveById("addSidoraDS").replace().log("Skipping fedora addSidoraDS OBJ");

                weaveById("addResourceBlurVersion").before()
                        .to("file:{{karaf.home}}/output")
                        .to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("CamelFileAbsolutePath", KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment");
        exchange.getIn().setBody("testWildLifeInsightsDeploymentIds1i3.JPG");

        template.send("direct:addImageResource", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testImageResourceFaceBlurUseOriginalOnFail() throws Exception {
        String manifest = readFileToString(new File(KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(KARAF_HOME + "/output/testWildLifeInsightsDeploymentIds1i3.JPG");

        context.getRouteDefinition("UnifiedCameraTrapAddImageResource").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("createResourcePID").replace().log("Skipping fedora create!!!");
                weaveById("addResourceOriginalVersion").replace().log("Skipping fedora addDatastream OBJ");
                weaveById("addResourceBlurVersion").replace().log("Skipping fedora addDatastream OBJ");
                weaveById("addSidoraDS").replace().log("Skipping fedora addSidoraDS OBJ");

                weaveById("execPythonFaceblur").after()
                        .setHeader("CamelExecExitValue").simple("1");
                weaveById("addResourceCreateThumbnail").before()
                        .to("file:{{karaf.home}}/output")
                        .to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("CamelFileAbsolutePath", KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment");
        exchange.getIn().setBody("testWildLifeInsightsDeploymentIds1i3.JPG");

        template.send("direct:addImageResource", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testImageResourceExiftoolUseOriginalOnFail() throws Exception {
        String manifest = readFileToString(new File(KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists(KARAF_HOME + "/output/testWildLifeInsightsDeploymentIds1i3.JPG");
        mockResult.expectedFileExists(KARAF_HOME + "/staging/testWildLifeInsightsDeploymentIds1i3.JPG");

        context.getRouteDefinition("UnifiedCameraTrapAddImageResource").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("createResourcePID").replace().log("Skipping fedora create!!!");
                weaveById("addResourceOriginalVersion").replace().log("Skipping fedora addDatastream OBJ");
                weaveById("addResourceBlurVersion").replace().log("Skipping fedora addDatastream OBJ");

                weaveById("execExiftool").after()
                        .setHeader("CamelExecStderr").simple("Simulating Exiftool error!!!")
                        .setHeader("CamelExecExitValue").simple("1");
                weaveById("addResourceCreateThumbnail").before()
                        .to("file:{{karaf.home}}/output")
                        .to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("CamelFileAbsolutePath", KARAF_HOME + "/wildlife_insights_test_data/unified-test-deployment");
        exchange.getIn().setBody("testWildLifeInsightsDeploymentIds1i3.JPG");

        template.send("direct:addImageResource", exchange);

        assertMockEndpointsSatisfied();
    }
}
