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

import static org.apache.camel.builder.Builder.header;
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
public class UnifiedCameraTrapPostValidationTest {

    private static final Logger log = LoggerFactory.getLogger(UnifiedCameraTrapPostValidationTest.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = "src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles";

    //Camera Trap Deployment Info for testing
    private String deploymentPackageId = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint to be used for assertions
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File("src/test/resources/UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/deployment_manifest.xml");
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
     * Integration Test of the CameraTrapValidateDatastreamFields route
     * All Datastream Field Validations Fail
     * @throws Exception
     */
    @Test
    public void validation_Fail_iTest() throws Exception {
        //Set the headers used in the route
        headers.put("VolunteerObservationPID", "test:00000");
        headers.put("ImageObservationPID", "test:00000");
        headers.put("ImageResourcePID", "test:00000");
        headers.put("modsImageSequenceId", "d18981s1");

        //Setup each datastream validation route to use our test data and set the expected validation error message
        setupEAC_CPF(false, new File(testDataDir + "/EAC-CPF/fail_EAC_CPF.xml"));
        setupFGDC(false, new File(testDataDir + "/FGDC/fail_FGDC.xml"));
        setupMODS(false, new File(testDataDir + "/MODS/fail_MODS.xml"));

        //Test data for CSV observations
        File[] datastreamFileCSV = {
                new File(testDataDir + "/ResearcherObservation/fail_ResearcherObservationCSV.csv"),
                new File(testDataDir + "/VolunteerObservation/fail_VolunteerObservationCSV.csv"),
                new File(testDataDir + "/ImageObservation/fail_ImageObservationCSV.csv")
        };

        //Setup the CSV validation error messages and use AdviceWith to make sure the datastream matches the Observer being tested
        setupCSV_ValidationAdviceWith(false, false, false, datastreamFileCSV);
        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Integration Test of the CameraTrapValidateDatastreamFields route
     * All Datastream Field Validations PASS
     * @throws Exception
     */
    @Test
    public void validation_Pass_iTest() throws Exception {
        //Set the headers used in the route
        headers.put("VolunteerObservationPID", "test:00000");
        headers.put("ImageObservationPID", "test:00000");
        headers.put("ImageResourcePID", "test:00000");
        headers.put("modsImageSequenceId", "d18981s1");

        //Setup each datastream validation route to use our test data and set the expected validation error message
        setupEAC_CPF(true, new File(testDataDir + "/EAC-CPF/valid_EAC_CPF.xml"));
        setupFGDC(true, new File(testDataDir + "/FGDC/valid_FGDC.xml"));
        setupMODS(true, new File(testDataDir + "/MODS/valid_MODS.xml"));

        //Test data for CSV observations
        File[] datastreamFileCSV = {
                new File(testDataDir + "/ResearcherObservation/valid_ResearcherObservationCSV.csv"),
                new File(testDataDir + "/VolunteerObservation/valid_VolunteerObservationCSV.csv"),
                new File(testDataDir + "/ImageObservation/valid_ImageObservationCSV.csv")
        };

        //Setup the CSV validation error messages and use AdviceWith to make sure the datastream matches the Observer being tested
        setupCSV_ValidationAdviceWith(true, true, true, datastreamFileCSV); //volunteer is optional

        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Integration Test of the CameraTrapValidateDatastreamFields route
     * Mixed PASS / Fail Datastream Field Validations
     * @throws Exception
     */
    @Test
    public void validation_MixedPassFail_iTest() throws Exception {
        //Set the headers used in the route
        headers.put("VolunteerObservationPID", "test:00000");
        headers.put("ImageObservationPID", "test:00000");

        //Setup each datastream validation route to use our test data and set the expected validation error message
        setupEAC_CPF(true, new File(testDataDir + "/EAC-CPF/valid_EAC_CPF.xml"));
        setupFGDC(false, new File(testDataDir + "/FGDC/fail_FGDC.xml"));
        setupMODS(true, new File(testDataDir + "/MODS/valid_MODS.xml"));

        //Test data for CSV observations
        File[] datastreamFileCSV = {
                new File(testDataDir + "/ResearcherObservation/fail_ResearcherObservationCSV.csv"),
                new File(testDataDir + "/VolunteerObservation/fail_VolunteerObservationCSV.csv"),
                new File(testDataDir + "/ImageObservation/valid_ImageObservationCSV.csv")
        };

        //Setup the CSV validation error messages and use AdviceWith to make sure the datastream matches the Observer being tested
        setupCSV_ValidationAdviceWith(false, false, true, datastreamFileCSV); //volunteer is optional

        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Testing the UnifiedCameraTrapValidateCSVFields route using only Researcher Observation
     * @throws Exception
     */
    @Test
    public void unifiedCameraTrapValidateCSVFields_ResearcherObservation_Test() throws Exception {

        //RouteId and endpoint uri for the test
        String routeId = "UnifiedCameraTrapValidateCSVFields";
        String routeURI = "direct:ValidateCSVFields";

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(new File(testDataDir + "/ResearcherObservation/valid_ResearcherObservationCSV.csv"), "utf-8");

        //Setup the expected validation message for CSV validations
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                "ResearcherIdentifications CSV: Validation Failed!", true);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, routeId, false, a -> {

                //provide the datastream as Fedora normally would but with out test data
                a.weaveByToString(".*getDatastreamDissemination.*")
                        .replace()
                        .setBody().simple(String.valueOf(datastream));
                //at the end of the route send body to the validation aggregator to aggregate any validation errors
                //and again with completion predicate set then stop the route
                a.weaveAddLast()
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .setHeader("validationComplete").simple("true")
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .stop();
        });

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodyReceived().body().isEqualTo(expectedBody);

        template.sendBodyAndHeaders(routeURI, "testCSV", headers);

        //get the result from the route
        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.debug("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.debug("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals(expectedBody, resultBody, "mock:result expectedBody equals resultBodyBody assertEquals failed!");

        mockEndpoint.assertIsSatisfied();

    }

    /**
     * Method that runs the CameraTrapValidateDatastreamFields route using adviceWith to mock for testing purposes
     * and check the assertions
     * @throws Exception
     */
    public void validate_DatastreamFieldsRoute_IT() throws Exception {

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidateDatastreamFields", false, a ->  {

                // need to set header for the test created in the setupCSV AdviceWith so that
                // the correct observation csv datastream is used for researcher, volunteer, or image.
                a.weaveByToString(".*validationErrorMessageAggregationStrategy.*").selectFirst().after().setHeader("testingVolunteerObservation").simple("true");
                a.weaveByToString(".*validationErrorMessageAggregationStrategy.*").selectIndex(1).after().setHeader("testingImageObservation").simple("true");

                a.weaveAddLast().stop();
        });

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
//        mockEndpoint.expectedBodiesReceived(expectedBody);

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.debug("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.debug("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals(expectedBody, resultBody, "mock:result expectedBody equals resultBodyBody assertEquals failed!");

        mockEndpoint.assertIsSatisfied();
    }

    /**
     * EAC-CPF Datastream test setup
     *
     * @param pass
     * @param datastreamFile
     * @throws Exception
     */
    public void setupEAC_CPF(Boolean pass, File datastreamFile) throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(datastreamFile, "utf-8");

        //Setup the expected validation error message for EAC-CPF validations
        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + deploymentPackageId);
        message.append(", Message - EAC-CPF ProjectName Field validation failed. ");
        message.append("Expected Sample Triangle Camera Trap Survey Project but found Sample Blah Blah Blah Project.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                message.toString(), pass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidate_EAC-CPF_Datastream", false, a ->
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                a.weaveByToString(".*getDatastreamDissemination.*").replace().setBody().simple(String.valueOf(datastream)));
    }

    /**
     * FGDC Datastream test setup
     *
     * @throws Exception
     * @param pass
     * @param datastreamFile
     */
    public void setupFGDC(boolean pass, File datastreamFile) throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(datastreamFile, "utf-8");

        //Setup the expected validation error message for FGDC validations
        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + deploymentPackageId);
        message.append(", Message - FGDC CameraDeploymentID Field validation failed. ");
        message.append("Expected d18981 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                message.toString(), pass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        };

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidate_FGDC_Datastream", false, a ->
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                a.weaveByToString(".*getDatastreamDissemination.*").replace().setBody().simple(String.valueOf(datastream)));
    }

    /**
     * MODS Datastream test setup
     *
     * @throws Exception
     * @param pass
     * @param datastreamFile
     */
    public void setupMODS(boolean pass, File datastreamFile) throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(datastreamFile, "utf-8");

        //Setup the expected validation error message for MODS validations
        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + deploymentPackageId);
        message.append(", Message - MODS ImageSequenceId Field validation failed. ");
        message.append("Expected d18981s1 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                message.toString(), pass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidate_MODS_Datastream", false, a ->
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                a.weaveByToString(".*getDatastreamDissemination.*").replace().setBody().simple(String.valueOf(datastream)));
    }

    /**
     * Researcher and Volunteer CSV Datastream test setup
     *
     * NOTE: The csv route is modified slightly only for testing purposes. A header is created in the
     * validate_DatastreamFieldsRoute_IT AdviceWith after the researcher csv validation completes
     * The modified csv validation route will use the volunteer datastream if the header value is true.
     *
     * @throws Exception
     * @param researcherPass
     * @param volunteerPass
     * @param datastreamFileCSV
     */
    private void setupCSV_ValidationAdviceWith(boolean researcherPass, boolean volunteerPass, boolean imagePass, File[] datastreamFileCSV) throws Exception {

        //The researcher or volunteer datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        String researcherCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[0], "utf-8");
        String volunteerCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[1], "utf-8");
        String imageCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[2], "utf-8");

        //Setup the Researcher Observation expected validation error message
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                "ResearcherIdentifications CSV: Validation Failed!", researcherPass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Setup the Volunteer Observation expected error message
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                "VolunteerIdentifications CSV: Validation Failed!", volunteerPass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Setup the Image Observation expected validation error message
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
                "ImageIdentifications CSV: Validation Failed!", imagePass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidateCSVFields", false, a ->
                // The csv route is modified slightly only for testing purposes. A header is created in the
                // validate_DatastreamFieldsRoute_IT AdviceWith after the researcher csv validation completes
                // The modified csv validation route will use the volunteer datastream if the header value is true.
                a.weaveByToString(".*getDatastreamDissemination.*").replace()
                        .choice()
                        .when(header("testingImageObservation").isEqualTo("true"))
                            .setBody().simple(String.valueOf(imageCSVdatastream))
                        .endChoice()
                        .when(header("testingVolunteerObservation").isEqualTo("true"))
                            .setBody().simple(String.valueOf(volunteerCSVdatastream))
                        .endChoice()
                        .otherwise()
                            .setBody().simple(String.valueOf(researcherCSVdatastream)));
    }

    /**
     * Adding the mock:result endpoint at the end of the ValidationErrorMessageAggregationStrategy for assertion's
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
