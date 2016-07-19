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

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.LogDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author jbirkhimer
 */
public class WCSCameraTrapPostIngestValidationTest extends CamelBlueprintTestSupport {
    
    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = "src/test/resources/WCS-TestFiles/ECU-011-D0128/TestDatasreams";
    
    //Camera Trap Deployment Info for testing
    private String camelFileParent = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint to be used for assertions
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest
    private File manifestFile = new File("src/test/resources/WCS-TestFiles/ECU-011-D0128/TestDeploymentPkgs/ECU-011-D0128/deployment_manifest.xml");
    private String manifest;

    //Datastream and Field values
    private String datastream;

    //Validation message bean configuration
    private CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    private CameraTrapValidationMessage.MessageBean expectedValidationMessage;

    //The mock:result expected body
    private ArrayList expectedBody = new ArrayList<>();

    //Temp directories created for testing the camel validation route
    private static File tempInputDirectory, processDirectory, tempConfigDirectory;

    /**
     * Sets up the system properties and Temp directories used by the
     * camera trap route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        //Define the Process directory that the camera trap route creates
        // to be able to clean the project up after tests
        processDirectory = new File("ProcessWCS");

        //Create and Copy the Input dir xslt, etc. files used in the camera trap route
        tempInputDirectory = new File("Input");
        if(!tempInputDirectory.exists()){
            tempInputDirectory.mkdir();
        }

        //The Location of the original Input dir in the project
        File inputSrcDirLoc = new File("../../Routes/Camera Trap/Input");

        //Copy the Input src files to the CameraTrap root so the camel route can find them
        FileUtils.copyDirectory(inputSrcDirLoc, tempInputDirectory);

        tempConfigDirectory = new File("Karaf-config");
        if(!tempConfigDirectory.exists()){
            tempConfigDirectory.mkdir();
        }

        FileUtils.copyDirectory(new File("../../Routes/Camera Trap/Karaf-config"), tempConfigDirectory);

        // TODO: using the Karaf-config directory for karaf.home and system.properties file
        //Set the karaf.home property use by the camera trap route
        System.setProperty("karaf.home", "Karaf-config");
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = new Properties();
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("Karaf-config/etc/edu.si.sidora.karaf.cfg");

            props.load(in);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return props;
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
        if(tempConfigDirectory.exists()){
            FileUtils.deleteDirectory(tempConfigDirectory);
        }
        if(processDirectory.exists()){
            FileUtils.deleteDirectory(processDirectory);
        }
    }

    /**
     * Override this method, and return the location of our Blueprint XML file to be used for testing.
     * The actual camera trap route that the maven lifecycle phase process-test-resources executes
     * to copy test resources to output folder target/test-classes.
     */
    @Override
    protected String getBlueprintDescriptor() {
        //use the production route for testing that the pom copied into the test resources
        return "Route/wcs-route.xml";
    }

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

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
    }

    /**
     * Integration Test of the WCSValidatePostIngestResourceCount route
     * Test Resource Counts Match and Resource Objects Found
     * @throws Exception
     */
    @Test
    public void WCSValidatePostIngestResourceCount_Matches_ObjectFound_iTest() throws Exception {
        runWCSValidatePostIngestResourceCountRoute_iTest(1, "/RELS-EXT/resource_RELS-EXT.xml", "/fcrepo_findObject_result/found.xml");
    }

    /**
     * Integration Test of the WCSValidatePostIngestResourceCount route
     * Test Resource Counts Do Not Match and Resource Objects Not Found
     * @throws Exception
     */
    @Test
    public void WCSValidatePostIngestResourceCount_NotMatch_ObjectNotFound_iTest() throws Exception {
        runWCSValidatePostIngestResourceCountRoute_iTest(2, "/RELS-EXT/resource_RELS-EXT.xml", "/fcrepo_findObject_result/notFound.xml");
    }

    /**
     * Integration Test of the WCSValidatePostIngestResourceCount route
     * Test Resource Counts Match and Resource Objects Not Found
     * @throws Exception
     */
    @Test
    public void WCSValidatePostIngestResourceCount_Match_ObjectNotFound_iTest() throws Exception {
        runWCSValidatePostIngestResourceCountRoute_iTest(1, "/RELS-EXT/resource_RELS-EXT.xml", "/fcrepo_findObject_result/notFound.xml");
    }

    /**
     * Setup and run the WCSValidatePostIngestResourceCount Route (direct:validatePostIngestResourceCount) using AdviceWith
     * for stubbing in test datastreams and values for testing the route.
     * @param resourceCount our test resource count, this is normally generated by the route during ingest
     * @param resourceRels_Ext RELS-EXT datastream, this is  normally provided by Fedora
     * @param fcrepo_objectResponce our test response query of Fedora relational db and PID, this is normally provided by the FcrepoRest endpoint that will query Fedora relational db and PID
     */
    public void runWCSValidatePostIngestResourceCountRoute_iTest(Integer resourceCount, String resourceRels_Ext, String fcrepo_objectResponce) throws Exception {
        //Set headers
        headers.put("SitePID", "test:00000");
        headers.put("ValidationErrors", "ValidationErrors"); //Set the header for aggregation correlation
        headers.put("ResourceCount", resourceCount); //Header that's incremented after a resource obj is ingested.

        //The RELS-EXT datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(new File(testDataDir + resourceRels_Ext));

        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidatePostIngestResourceCount").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(datastream));

                //set body for fedora ri search result
                weaveByType(ToDynamicDefinition.class)
                        .replace()
                        .setBody(simple(FileUtils.readFileToString(new File(testDataDir + fcrepo_objectResponce))));

                //Send Validation complete and stop route
                weaveAddLast()
                        .setHeader("ValidationComplete", simple("true"))
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .stop();
            }
        });

        //Set mock endpoint for assertion
        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        //mockEndpoint.setMinimumExpectedMessageCount(1);

        //Send the datastream and headers to the WCSValidatePostIngestResourceCount route
        template.sendBodyAndHeaders("direct:validatePostIngestResourceCount", datastream, headers);

        //the mock:result body and header values
        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();
        Integer relsExtResourceCountResult = mockEndpoint.getExchanges().get(0).getIn().getHeader("RelsExtResourceCount", Integer.class);

        //Setup the Resource Count Validation expected validation error message
        if (!relsExtResourceCountResult.equals(resourceCount)) {
            StringBuilder message = new StringBuilder();
            message.append("Post Resource Count validation failed. ");
            message.append("Expected " + resourceCount + " but found " + relsExtResourceCountResult);

            expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                    message.toString(), false);
            expectedBody.add(expectedValidationMessage);
        }

        //Setup the Resource Object Not Found expected validation error message
        if (fcrepo_objectResponce.contains("not")) {
            expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent, String.valueOf(headers.get("SitePID")),
                    "Resource Object not found from Fedora Repository", false);
            expectedBody.add(expectedValidationMessage);
        }

        //assertions
        if (!expectedBody.isEmpty()) {
            log.debug("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
            log.debug("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        } else {
            log.debug("expectedBody:\n" + datastream.trim() + "\nresultBody:\n" + resultBody);
            log.debug("expectedBody Type:\n" + datastream.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", datastream.trim(), resultBody);
        }

        assertMockEndpointsSatisfied();
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

        //Setup each datastream validation route to use our test data and set the expected validation error message
        setupEAC_CPF(false, new File(testDataDir + "/EAC-CPF/fail_EAC_CPF.xml"));
        setupFGDC(false, new File(testDataDir + "/FGDC/fail_FGDC.xml"));
        setupMODS(false, new File(testDataDir + "/MODS/fail_MODS.xml"));

        //Test data for CSV observations
        File[] datastreamFileCSV = {
                new File(testDataDir + "/ResearcherObservation/fail_ResearcherObservationCSV.csv"),
                new File(testDataDir + "/VolunteerObservation/fail_VolunteerObservationCSV.csv"), //volunteer is optional
                new File(testDataDir + "/ImageObservation/fail_ImageObservationCSV.csv")
        };

        //Setup the CSV validation error messages and use AdviceWith to make sure the datastream matches the Observer being tested
        setupCSV_ValidationAdviceWith(false, false, false, datastreamFileCSV); //volunteer is optional
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

        //Setup each datastream validation route to use our test data and set the expected validation error message
        setupEAC_CPF(true, new File(testDataDir + "/EAC-CPF/valid_EAC_CPF.xml"));
        setupFGDC(true, new File(testDataDir + "/FGDC/valid_FGDC.xml"));
        setupMODS(true, new File(testDataDir + "/MODS/valid_MODS.xml"));

        //Test data for CSV observations
        File[] datastreamFileCSV = {
                new File(testDataDir + "/ResearcherObservation/valid_ResearcherObservationCSV.csv"),
                new File(testDataDir + "/VolunteerObservation/valid_VolunteerObservationCSV.csv"), //volunteer is optional
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
                new File(testDataDir + "/VolunteerObservation/fail_VolunteerObservationCSV.csv"), //volunteer is optional
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
     * Testing the WCSValidateCSVFields route using only Researcher Observation
     * @throws Exception
     */
    @Test
    public void WCSValidateCSVFields_ResearcherObservation_Test() throws Exception {

        //RouteId and endpoint uri for the test
        String routeId = "WCSValidateCSVFields";
        String routeURI = "direct:ValidateCSVFields";

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastream = FileUtils.readFileToString(new File(testDataDir + "/ResearcherObservation/valid_ResearcherObservationCSV.csv"));

        //Setup the expected validation message for CSV validations
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "ResearcherIdentifications CSV: Validation Failed!", true);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //add the mock:result endpoint to the end of the ValidationErrorMessageAggregationStrategy route using AdviceWith
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition(routeId).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //provide the datastream as Fedora normally would but with out test data
                weaveByToString(".*getDatastreamDissemination.*")
                        .replace()
                        .setBody(simple(String.valueOf(datastream)));
                //at the end of the route send body to the validation aggregator to aggregate any validation errors
                //and again with completion predicate set then stop the route
                weaveAddLast()
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .setHeader("ValidationComplete", simple("true"))
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodyReceived().body().isEqualTo(expectedBody);

        template.sendBodyAndHeaders(routeURI, "testCSV", headers);

        //get the result from the route
        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.debug("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.debug("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result expectedBody equals resultBodyBody assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();

    }

    /**
     * Method that runs the CameraTrapValidateDatastreamFields route using adviceWith to mock for testing purposes
     * and check the assertions
     * @throws Exception
     */
    public void validate_DatastreamFieldsRoute_IT() throws Exception {

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                // need to set header for the test created in the setupCSV advicewithroutebuilder so that
                // the correct observation csv datastream is used for researcher, volunteer, or image.
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").selectFirst().after().setHeader("testingVolunteerObservation", simple("true")); //volunteer is optional
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").selectIndex(1).after().setHeader("testingImageObservation", simple("true"));

                weaveAddLast().stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.debug("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.debug("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result expectedBody equals resultBodyBody assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
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
        datastream = FileUtils.readFileToString(datastreamFile);

        //Setup the expected validation error message for EAC-CPF validations
        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - EAC-CPF ProjectName Field validation failed. ");
        message.append("Expected Jauguar Survey ITT but found blah-blah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidate_EAC-CPF_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));
            }
        });
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
        datastream = FileUtils.readFileToString(datastreamFile);

        //Setup the expected validation error message for FGDC validations
        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - FGDC CameraDeploymentID Field validation failed. ");
        message.append("Expected WCS-ECU-011-D0128 but found blah-blah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        };

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidate_FGDC_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));
            }
        });
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
        datastream = FileUtils.readFileToString(datastreamFile);

        //Setup the expected validation error message for MODS validations
        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - MODS ImageSequenceId Field validation failed. ");
        message.append("Expected WCS-ECU-011-D0128-1 but found blah-blah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidate_MODS_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));
            }
        });
    }

    /**
     * Researcher and Volunteer CSV Datastream test setup
     *
     * NOTE: The csv route is modified slightly only for testing purposes. A header is created in the
     * validate_DatastreamFieldsRoute_IT advicewithroutebuilder after the researcher csv validation completes
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
        String researcherCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[0]);
        String volunteerCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[1]); //volunteer is optional
        String imageCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[2]);

        //Setup the Researcher Observation expected validation error message
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "ResearcherIdentifications CSV: Validation Failed!", researcherPass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Setup the Volunteer Observation expected error message (Volunteer Observations are optional)
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "VolunteerIdentifications CSV: Validation Failed!", volunteerPass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }
        
        //Setup the Image Observation expected validation error message
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "ImageIdentifications CSV: Validation Failed!", imagePass);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // The csv route is modified slightly only for testing purposes. A header is created in the
                // validate_DatastreamFieldsRoute_IT advicewithroutebuilder after the researcher csv validation completes
                // The modified csv validation route will use the volunteer datastream if the header value is true.
                weaveByToString(".*getDatastreamDissemination.*").replace()
                        .choice()
                        .when(header("testingImageObservation").isEqualTo("true"))
                        .setBody(simple(String.valueOf(imageCSVdatastream)))
                        .when(header("testingVolunteerObservation").isEqualTo("true")) //volunteer is optional
                        .setBody(simple(String.valueOf(volunteerCSVdatastream))) //volunteer is optional
                        .otherwise()
                        .setBody(simple(String.valueOf(researcherCSVdatastream)));
            }
        });
    }

    /**
     * Adding the mock:result endpoint at the end of the ValidationErrorMessageAggregationStrategy for asertions
     * The calling tests will set the completion predicate to notify the aggregator that aggregation is complete.
     * @throws Exception
     */
    private void setupValidationErrorMessageAggregationStrategyAdviceWith() throws Exception {
        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("WCSValidationErrorMessageAggregationStrategy").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                weaveByType(LogDefinition.class).after().to("mock:result");
            }
        });
    }
}
