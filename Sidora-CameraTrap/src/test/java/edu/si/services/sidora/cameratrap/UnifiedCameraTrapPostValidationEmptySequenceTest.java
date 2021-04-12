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

import edu.si.services.sidora.cameratrap.validation.CameraTrapValidationMessage;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author jbirkhimer
 */
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false",
        "camel.springboot.java-routes-exclude-pattern=UnifiedCameraTrapInFlightConceptStatusPolling,UnifiedCameraTrapStartProcessing"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UnifiedCameraTrapPostValidationEmptySequenceTest {

    private static final Logger log = LoggerFactory.getLogger(UnifiedCameraTrapPostValidationEmptySequenceTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = "src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles";

    //Camera Trap Deployment Info for testing
    private String deploymentPackageId = "10002000";
    private int ManifestCameraDeploymentId = 0001;

    //Mock endpoint to be used for assertions
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File("src/test/resources/UnifiedManifest-TestFiles/emptySequenceDeploymentPkg/deployment_manifest.xml");
    private String manifest;

    //Datastream and Field values
    private String datastream;

    //Validation message bean configuration
    private CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    private CameraTrapValidationMessage.MessageBean expectedValidationMessage;

    //The mock:result expected body
    private ArrayList expectedBody = new ArrayList<>();

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        //Store the Deployment Manifest as string to set the camel ManifestXML header
        manifest = FileUtils.readFileToString(manifestFile, "utf-8");

        //Initialize the expected camel headers
        headers = new HashMap<>();
        headers.put("deploymentPackageId", deploymentPackageId);
        headers.put("ManifestCameraDeploymentId", ManifestCameraDeploymentId);
        headers.put("ManifestXML", String.valueOf(manifest));
        headers.put("validationErrors", "validationErrors");
        headers.put("ProjectPID", "test:0000");
        headers.put("SitePID", "test:0000");
        headers.put("CamelFedoraPid", "test:1");
    }

    /**
     * Integration Test of the UnifiedCameraTrapValidatePostIngestResourceCount route
     * Test Resource Counts Match and Resource Objects Found
     * @throws Exception
     */
    @Test
    public void unifiedCameraTrapValidatePostIngestResourceCount_Matches_ObjectFound_iTest() throws Exception {
        runUnifiedCameraTrapValidatePostIngestResourceCountRoute_Test(1, "/RELS-EXT/resource_RELS-EXT.xml", "/fcrepo_findObject_result/found.xml");
    }

    /**
     * Integration Test of the UnifiedCameraTrapValidatePostIngestResourceCount route
     * Test Resource Counts Do Not Match and Resource Objects Not Found
     * @throws Exception
     */
    @Test
    public void unifiedCameraTrapValidatePostIngestResourceCount_NotMatch_ObjectNotFound_Test() throws Exception {
        runUnifiedCameraTrapValidatePostIngestResourceCountRoute_Test(2, "/RELS-EXT/resource_RELS-EXT.xml", "/fcrepo_findObject_result/notFound.xml");
    }

    /**
     * Integration Test of the UnifiedCameraTrapValidatePostIngestResourceCount route
     * Test Resource Counts Match and Resource Objects Not Found
     * @throws Exception
     */
    @Test
    public void unifiedCameraTrapValidatePostIngestResourceCount_Match_ObjectNotFound_Test() throws Exception {
        runUnifiedCameraTrapValidatePostIngestResourceCountRoute_Test(1, "/RELS-EXT/resource_RELS-EXT.xml", "/fcrepo_findObject_result/notFound.xml");
    }

    /**
     * Setup and run the UnifiedCameraTrapValidatePostIngestResourceCount Route (direct:validatePostIngestResourceCount) using AdviceWith
     * for stubbing in test datastreams and values for testing the route.
     * @param resourceCount our test resource count, this is normally generated by the route during ingest
     * @param resourceRels_Ext RELS-EXT datastream, this is  normally provided by Fedora
     * @param fcrepo_objectResponse our test response query of Fedora relational db and PID, this is normally provided by the FcrepoRest endpoint that will query Fedora relational db and PID
     */
    public void runUnifiedCameraTrapValidatePostIngestResourceCountRoute_Test(Integer resourceCount, String resourceRels_Ext, String fcrepo_objectResponse) throws Exception {
        //Set headers
        headers.put("SitePID", "test:00000");
        headers.put("validationErrors", "validationErrors"); //Set the header for aggregation correlation
        headers.put("ResourceCount", resourceCount); //Header that's incremented after a resource obj is ingested.

        //The RELS-EXT datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(new File(testDataDir + resourceRels_Ext), "utf-8");

        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidatePostIngestResourceCount", false, a -> {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                a.weaveByToString(".*getDatastreamDissemination.*").replace().setBody().simple(datastream);

                //set body for fedora ri search result
                a.weaveByType(ToDynamicDefinition.class)
                        .replace()
                        .setBody().simple(FileUtils.readFileToString(new File(testDataDir + fcrepo_objectResponse), "utf-8"));

                //Send Validation complete and stop route
                a.weaveAddLast()
                        .setHeader("validationComplete").simple("true")
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .stop();
        });

        //Set mock endpoint for assertion
        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        //mockEndpoint.setMinimumExpectedMessageCount(1);

        //Send the datastream and headers to the UnifiedCameraTrapValidatePostIngestResourceCount route
        template.sendBodyAndHeaders("direct:validatePostIngestResourceCount", datastream, headers);

        //the mock:result body and header values
        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();
        Integer relsExtResourceCountResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("RelsExtResourceCount", Integer.class);

        //Setup the Resource Count Validation expected validation error message
        if (!relsExtResourceCountResult.equals(resourceCount)) {
            StringBuilder message = new StringBuilder();
            message.append("Post Resource Count validation failed. ");
            message.append("Expected " + resourceCount + " but found " + relsExtResourceCountResult);

            expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                    message.toString(), false);
            expectedBody.add(expectedValidationMessage);
        }

        //Setup the Resource Object Not Found expected validation error message
        if (fcrepo_objectResponse.contains("not")) {
            expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId, String.valueOf(headers.get("SitePID")),
                    "Resource Object not found from Fedora Repository", false);
            expectedBody.add(expectedValidationMessage);
        }

        //assertions
        if (expectedBody instanceof ArrayList) {
            log.debug("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
            log.debug("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals(expectedBody, resultBody, "mock:result Body assertEquals failed!");

        } else {
            log.debug("expectedBody:\n" + datastream.trim() + "\nresultBody:\n" + resultBody);
            log.debug("expectedBody Type:\n" + datastream.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals(datastream.trim(), resultBody, "mock:result Body assertEquals failed!");
        }

        mockEndpoint.assertIsSatisfied();
    }

    /**
     * Adding the mock:result endpoint at the end of the ValidationErrorMessageAggregationStrategy for asertions
     * The calling tests will set the completion predicate to notify the aggregator that aggregation is complete.
     * @throws Exception
     */
    private void setupValidationErrorMessageAggregationStrategyAdviceWith() throws Exception {
        //using adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidationErrorMessageAggregationStrategy", false, a -> {

            a.weaveByType(ChoiceDefinition.class).after().to("mock:result");
                /*a.weaveByType(ChoiceDefinition.class).before()
                        .process(exchange -> {
                            Message out = exchange.getIn();
                            log.debug("debug here");
                        })
                        .choice()
                            .when().simple("${body} not is 'java.util.List'")
                                .to("mock:result").stop()
                            .endChoice()
                        .end()
                        .to("mock:result");*/
        });
    }
}
