/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.services.sidora.cameratrap;

import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the Unified Camera Trap Ingest Pipeline
 * TODO: Add more tests
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false",
        "si.ct.wi.faceBlur.script=target/config/FaceBlurrer/FaceBlurrer.py",
        "si.ct.wi.faceBlur.classifier=target/config/FaceBlurrer/haarcascades/haarcascade_frontalface_alt.xml",
        "camel.springboot.java-routes-exclude-pattern=UnifiedCameraTrapInFlightConceptStatusPolling,UnifiedCameraTrapStartProcessing"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UnifiedCameraTrapIngestRoutesTest {

    private static final Logger log = LoggerFactory.getLogger(UnifiedCameraTrapIngestRoutesTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = "src/test/resources/UnifiedManifest-TestFiles";

    //Camera Trap Deployment Info for testing
    private String deploymentPackageId = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint to be used for assertions
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File(testDataDir + "/scbi_unified_stripped_p125d18981/deployment_manifest.xml");
    private String manifest;
    File deploymentZip;

    @PropertyInject(value = "{{si.ct.uscbi.process.dir.path}}")
    private String processDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.done.dir.path}}")
    private String processDoneDirPath;
    @PropertyInject(value = "{{si.ct.uscbi.process.error.dir.path}}")
    private String processErrorDirPath;

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {

        //Store the Deployment Manifest as string to set the camel ManifestXML header
        manifest = readFileToString(manifestFile, "utf-8");

        //Initialize the expected camel headers
        headers = new HashMap<>();
        headers.put("deploymentPackageId", deploymentPackageId);
        headers.put("ManifestCameraDeploymentId", ManifestCameraDeploymentId);
        headers.put("ManifestXML", String.valueOf(manifest));
        headers.put("validationErrors", "validationErrors");
        headers.put("ProjectPID", "test:0000");
        headers.put("SitePID", "test:0000");

        deleteDirectory(processDirPath);
        deleteDirectory(processDoneDirPath);
        deleteDirectory(processErrorDirPath);

        //Modify the default error handler so that we can send failed exchanges to mock:result for assertions
        // Sending to dead letter does not seem to work as expected for this
        context.adapt(ExtendedCamelContext.class).setErrorHandlerFactory(new DefaultErrorHandlerBuilder().onPrepareFailure(new Processor() {
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
        AdviceWith.adviceWith(context, routeId, false, a ->
                //add the mock:result endpoint and stop the route after the headers we are testing have been created
                a.weaveById("readImageResource").before().to("mock:result").stop());

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

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
        assertEquals(imageidHeaderExpected, imageidHeaderResult, "imageid header assertEquals failed!");
        assertEquals(imageSequenceIDHeaderExpected, imageSequenceIDHeaderResult, "ImageSequenceID header assertEquals failed!");
        assertEquals(imageSequenceIndexHeaderExpected, imageSequenceIndexHeaderResult, "ImageSequenceIndex header assertEquals failed!");
        assertEquals(imageSequenceCountHeaderExpected, imageSequenceCountHeaderResult, "ImageSequenceCount header assertEquals failed!");

        mockEndpoint.assertIsSatisfied();

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

        String MODS_DatastreamExpected = readFileToString(MODS_DatastreamTestFile, "utf-8");

        //the header thats used for the Unified_ManifestImage.xsl param
        headers.put("imageid", "d18981s1i1");

        //manually setting the FITSCreatedDate header
        headers.put("FITSCreatedDate", "2016:02:13 12:11:26");

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, routeId, false, a ->
                //Remove the setheader definition for FITSCreatedDate we are manually passing this header in already
                //weaveByType(SetHeaderDefinition.class).selectFirst().remove();
                //set the mockEndpoint result and stop the route before adding the MODS datastream to fedora
                a.weaveByToString(".*fedora:addDatastream.*").before().log(LoggingLevel.DEBUG, "uct.test", "============ BODY ============\n${body}").to("mock:result").stop());

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodyReceived().body().isEqualTo(MODS_DatastreamExpected);
        mockEndpoint.expectedBodiesReceived(MODS_DatastreamExpected);

        template.sendBodyAndHeaders(routeURI, "d18981s1i1.JPG", headers);

        String resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        log.debug("expectedBody:\n" + MODS_DatastreamExpected + "\nresultBody:\n" + resultBody);

        log.debug("expectedBody Type:\n" + MODS_DatastreamExpected.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals(MODS_DatastreamExpected, resultBody, "mock:result Body containing MODS datastream xml assertEquals failed!");

        mockEndpoint.assertIsSatisfied();

    }

    @Test
    public void testFitsSocketException() throws Exception {

        Integer minSocketExRedelivery = Integer.valueOf(context.resolvePropertyPlaceholders("{{min.socketEx.redeliveries}}"));

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.setAssertPeriod(1500);

        MockEndpoint mockError = context.getEndpoint("mock:error", MockEndpoint.class);
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SocketException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minSocketExRedelivery);
        mockError.setAssertPeriod(1500);

        AdviceWith.adviceWith(context, "UnifiedCameraTrapAddFITSDataStream", false, a ->  {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new SocketException("Simulating java.net.SocketException: Connection reset");
                    }
                };

                a.weaveById("fitsServiceException").after().to("mock:error");
                a.weaveById("UnifiedCameraTrapAddFITSDataStream_getFITSReport").replace().process(processor);
                a.weaveById("fitsAddDatastream").replace().log(LoggingLevel.INFO, "Skipping Fedora addDatastream!!!").to("mock:result");
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFileAbsolutePath", "src/test/resources/UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/d18981s1i1.JPG");

        template.send("direct:addFITSDataStream", exchange);

        mockResult.assertIsSatisfied();
        mockError.assertIsSatisfied();
    }

    @Test
    public void testFitsSocketExceptionAndExecFail() throws Exception {

        Integer minSocketExRedelivery = Integer.valueOf(context.resolvePropertyPlaceholders("{{min.socketEx.redeliveries}}"));

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(0); //we should not
        mockResult.setAssertPeriod(1500);

        MockEndpoint mockError = context.getEndpoint("mock:error", MockEndpoint.class);
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SocketException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minSocketExRedelivery);
        mockError.setAssertPeriod(1500);

        AdviceWith.adviceWith(context, "UnifiedCameraTrapAddFITSDataStream", false, a ->  {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new SocketException("Simulating java.net.SocketException: Connection reset");
                    }
                };

                a.weaveById("fitsServiceException").after().to("mock:error");
                a.weaveById("UnifiedCameraTrapAddFITSDataStream_getFITSReport").replace().process(processor);
                a.weaveById("fitsAddDatastream").replace().log(LoggingLevel.INFO, "Skipping Fedora addDatastream!!!").to("mock:result");
        });

        Exchange exchange = new DefaultExchange(context);
        //exchange.getIn().setHeader("CamelFileAbsolutePath", "UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/d18981s1i1.JPG");

        template.send("direct:addFITSDataStream", exchange);

        mockResult.assertIsSatisfied();
        mockError.assertIsSatisfied();
    }

    @Test
    public void testImageResourceFaceBlur() throws Exception {
        String manifest = readFileToString(new File("src/test/resources/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml"), "utf-8");

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists("target/output/testWildLifeInsightsDeploymentIds1i3.JPG");
        mockResult.expectedFileExists("target/staging/testWildLifeInsightsDeploymentIds1i3.JPG");

        AdviceWith.adviceWith(context, "UnifiedCameraTrapAddImageResource", false, a ->  {
                a.weaveById("createResourcePID").replace().log("Skipping fedora create!!!");
                a.weaveById("addResourceOriginalVersion").replace().log("Skipping fedora addDatastream OBJ");
                a.weaveById("addResourceBlurVersion").replace().log("Skipping fedora addDatastream OBJ").to("file:target/output")
                        .to("mock:result").stop();
                a.weaveById("addSidoraDS").replace().log("Skipping fedora addSidoraDS OBJ");
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("CamelFileAbsolutePath", "src/test/resources/wildlife_insights_test_data/unified-test-deployment");
        exchange.getIn().setBody("testWildLifeInsightsDeploymentIds1i3.JPG");

        template.send("direct:addImageResource", exchange);

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testImageResourceFaceBlurUseOriginalOnFail() throws Exception {
        String manifest = readFileToString(new File("src/test/resources/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml"), "utf-8");

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists("target/output/testWildLifeInsightsDeploymentIds1i3.JPG");

        AdviceWith.adviceWith(context, "UnifiedCameraTrapAddImageResource", false, a -> {
                a.weaveById("createResourcePID").replace().log("Skipping fedora create!!!");
                a.weaveById("addResourceOriginalVersion").replace().log("Skipping fedora addDatastream OBJ");
                a.weaveById("addResourceBlurVersion").replace().log("Skipping fedora addDatastream OBJ");
                a.weaveById("addSidoraDS").replace().log("Skipping fedora addSidoraDS OBJ");

                a.weaveById("execPythonFaceblur").after()
                        .setHeader("CamelExecExitValue").simple("1");
                a.weaveById("addResourceCreateThumbnail").before()
                        .to("file:target/output")
                        .to("mock:result").stop();
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("CamelFileAbsolutePath", "src/test/resources/wildlife_insights_test_data/unified-test-deployment");
        exchange.getIn().setBody("testWildLifeInsightsDeploymentIds1i3.JPG");

        template.send("direct:addImageResource", exchange);

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testImageResourceExiftoolUseOriginalOnFail() throws Exception {
        String manifest = readFileToString(new File("src/test/resources/wildlife_insights_test_data/unified-test-deployment/deployment_manifest.xml"), "utf-8");

        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedFileExists("target/output/testWildLifeInsightsDeploymentIds1i3.JPG");
        mockResult.expectedFileExists("target/staging/testWildLifeInsightsDeploymentIds1i3.JPG");

        AdviceWith.adviceWith(context, "UnifiedCameraTrapAddImageResource", false, a -> {
                a.weaveById("createResourcePID").replace().log("Skipping fedora create!!!");
                a.weaveById("addResourceOriginalVersion").replace().log("Skipping fedora addDatastream OBJ");
                a.weaveById("addResourceBlurVersion").replace().log("Skipping fedora addDatastream OBJ");

                a.weaveById("execExiftool").after()
                        .setHeader("CamelExecStderr").simple("Simulating Exiftool error!!!")
                        .setHeader("CamelExecExitValue").simple("1");
                a.weaveById("addResourceCreateThumbnail").before()
                        .to("file:target/output")
                        .to("mock:result").stop();
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", manifest);
        exchange.getIn().setHeader("CamelFileAbsolutePath", "src/test/resources/wildlife_insights_test_data/unified-test-deployment");
        exchange.getIn().setBody("testWildLifeInsightsDeploymentIds1i3.JPG");

        template.send("direct:addImageResource", exchange);

        mockResult.assertIsSatisfied();
    }
}
