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
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jbirkhimer
 */
public class DatastreamValidationRoute_IT_Test extends CamelBlueprintTestSupport{

    //Camera Trap Deployment Info
    private String camelFileParent = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint and AdviceWith configuration to be used
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest and Field values
    private File manifestFile = new File("src/test/resources/SID-569TestFiles/p151d18321/deployment_manifest.xml");
    private String manifest;

    //Datastream and Field values
    private String datastream;

    //Validation message bean configuration
    private CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    private CameraTrapValidationMessage.MessageBean expectedValidationMessage;

    private ArrayList expectedBody = new ArrayList<>();

    //Temp directories created for testing the camel validation route
    private static File tempInputDirectory, processDirectory;

    /**
     * Sets up the system properties and Temp directories used by the
     * camera trap route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        //Set the karaf.home property use by the camera trap route
        System.setProperty("karaf.home", "src/test/resources/SID-569TestFiles");

        //Define the Process directory that the camera trap route creates
        // to be able to clean the project up after tests
        processDirectory = new File("Process");

        //Create and Copy the Input dir xslt, etc. files used in the camera trap route
        tempInputDirectory = new File("Input");
        if(!tempInputDirectory.exists()){
            tempInputDirectory.mkdir();
        }

        //The Location of the original Input dir in the project
        File inputSrcDirLoc = new File("../../Routes/Camera Trap/Input");

        //Copy the Input src files to the CameraTrap root so the camel route can find them
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
        return "Route/camera-trap-route.xml";
    }

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

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
     * Testing the CameraTrapValidateDatastreamFields route
     * All Datastream Field Validations Fail
     * @throws Exception
     */
    @Test
    public void validation_FailTest() throws Exception {
        setupEAC_CPF(false, new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-projectName-EAC-CPF.xml"));
        setupFGDC(false, new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/fail-CameraDeploymentID-FGDC.xml"));
        setupMODS(false, new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/fail-ImageSequenceId-MODS.xml"));

        File[] datastreamFileCSV = {new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/ResearcherObservations/failResearcherCSV.bin"),
                new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/VolunteerObservations/failVolunteerCSV.bin")};

        setupCSV_ValidationAdviceWith(false, false, datastreamFileCSV);
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Testing the CameraTrapValidateDatastreamFields route
     * All Datastream Field Validations PASS
     * @throws Exception
     */
    @Test
    public void validationPassTest() throws Exception {
        setupEAC_CPF(true, new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/valid-EAC-CPF.xml"));
        setupFGDC(true, new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/validFGDC.xml"));
        setupMODS(true, new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/validMODS.xml"));

        File[] datastreamFileCSV = {new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/ResearcherObservations/validResearcherCSV.bin"),
                new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/VolunteerObservations/validVolunteerCSV.bin")};

        setupCSV_ValidationAdviceWith(true, true, datastreamFileCSV);
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Testing the CameraTrapValidateDatastreamFields route
     * Mixed PASS / Fail Datastream Field Validations
     * @throws Exception
     */
    @Test
    public void validationMixedPassFailTest() throws Exception {
        setupEAC_CPF(true, new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/valid-EAC-CPF.xml"));
        setupFGDC(false, new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/fail-CameraDeploymentID-FGDC.xml"));
        setupMODS(true, new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/validMODS.xml"));

        File[] datastreamFileCSV = {new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/ResearcherObservations/validResearcherCSV.bin"),
                new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/VolunteerObservations/failVolunteerCSV.bin")};

        setupCSV_ValidationAdviceWith(true, false, datastreamFileCSV);
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Method that runs the CameraTrapValidateDatastreamFields route using adviceWith to mock for testing purposes
     * and check the assertions
     * @throws Exception
     */
    public void validate_DatastreamFieldsRoute_IT() throws Exception {

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                // need to set header for the test created in the setupCSV advicewithroutebuilder so that
                // the correct observation csv datastream is used.
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").selectFirst().after().setHeader("testingVolunteer", simple("true"));

                weaveAddLast().stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

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
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        //message.append("Datastream EAC-CPF ProjectName Field Validation failed");
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - EAC-CPF ProjectName Field validation failed. ");
        message.append("Expected Prairie Ridge Project but found No Project Name.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("Validate_EAC-CPF_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
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
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        //message.append("Datastream FGDC CameraDeploymentID Field Validation failed");
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - FGDC CameraDeploymentID Field validation failed. ");
        message.append("Expected 0000 but found 1111.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        };

        context.getRouteDefinition("Validate_FGDC_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
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
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        //message.append("Datastream MODS ImageSequenceId Field Validation failed");
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - MODS ImageSequenceId Field validation failed. ");
        message.append("Expected 2970s1 but found 000000.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("Validate_MODS_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
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
    private void setupCSV_ValidationAdviceWith(boolean researcherPass, boolean volunteerPass, File[] datastreamFileCSV) throws Exception {

        //The researcher or volunteer datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        String researcherCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[0]);
        String volunteerCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[1]);

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "ResearcherIdentifications CSV: Validation Failed!", researcherPass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "VolunteerIdentifications CSV: Validation Failed!", volunteerPass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // The csv route is modified slightly only for testing purposes. A header is created in the
                // validate_DatastreamFieldsRoute_IT advicewithroutebuilder after the researcher csv validation completes
                // The modified csv validation route will use the volunteer datastream if the header value is true.
                weaveByToString(".*getDatastreamDissemination.*").replace()
                        .choice()
                        .when(header("testingVolunteer").isEqualTo("true"))
                            .setBody(simple(String.valueOf(volunteerCSVdatastream)))
                        .otherwise()
                            .setBody(simple(String.valueOf(researcherCSVdatastream)));
            }
        });
    }

    /**
     * Setting the mock endpoint at the end of the ValidationErrorMessageAggregationStrategy
     * when the completion predicate is used to notify the aggregator that aggregation is complete.
     * @throws Exception
     */
    private void setupValidationErrorMessageAggregationStrategyAdviceWith() throws Exception {
        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("ValidationErrorMessageAggregationStrategy").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                weaveByType(LogDefinition.class).after().to("mock:result");
            }
        });
    }
}
