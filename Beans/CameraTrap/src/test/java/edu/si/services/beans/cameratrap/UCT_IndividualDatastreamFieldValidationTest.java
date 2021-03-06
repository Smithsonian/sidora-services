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
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * Tests for the Camera Trap Validate Fields Route
 *
 * @author jbirkhimer
 */
public class UCT_IndividualDatastreamFieldValidationTest extends CT_BlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");

    //Test Data Directory contains the datastreams and other resources for the tests
    private String testDataDir = "src/test/resources/UnifiedManifest-TestFiles";

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

    //Datastream and Field values
    private File datastreamFile;
    private String datastream;

    //Validation message bean configuration
    private CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    private CameraTrapValidationMessage.MessageBean expectedValidationMessage;

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
    }

    /**
     * Used by each unit test to run the validation route using adviceWith to mock for testing purposes
     * and check the assertions using the params provided as the configuration
     * @param validationRouteDefinition The route id
     * @param validateDatastreamFieldsRoute The route endpoint
     * @param expectedBody The expected exchange body
     * @throws Exception
     */
    private void runValidationAdviceWithTest(String validationRouteDefinition, String validateDatastreamFieldsRoute, Object expectedBody) throws Exception {

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition(validationRouteDefinition).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders(validateDatastreamFieldsRoute, "test", headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    /**
     * Validation test of the EAC-CPF Datastream for the ProjectName Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_EAC_CPF_Fail() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return but modified for our test
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/EAC-CPF/fail_EAC_CPF.xml");

        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - EAC-CPF ProjectName Field validation failed. ");
        message.append("Expected Sample Triangle Camera Trap Survey Project but found Sample Blah Blah Blah Project.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), false);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add(expectedValidationMessage);

        runValidationAdviceWithTest("UnifiedCameraTrapValidate_EAC-CPF_Datastream", "direct:validate_EAC-CPF_Datastream", expectedBody);
    }

    /**
     * Validation test of the EAC-CPF Datastream for the ProjectName Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_EAC_CPF_Passed() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/EAC-CPF/valid_EAC_CPF.xml");

        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder expectedBody = new StringBuilder();
        expectedBody.append("EAC-CPF ProjectName|");
        expectedBody.append("//eac:nameEntry[1]/eac:part/text()|");
        expectedBody.append("//ProjectName/text()");

        runValidationAdviceWithTest("UnifiedCameraTrapValidate_EAC-CPF_Datastream", "direct:validate_EAC-CPF_Datastream", expectedBody.toString());
    }


    /**
     * Validation test of the FGDC Datastream for the CameraDeploymentID Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Fail() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/FGDC/fail_FGDC.xml");

        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - FGDC CameraDeploymentID Field validation failed. ");
        message.append("Expected d18981 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), false);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add(expectedValidationMessage);

        runValidationAdviceWithTest("UnifiedCameraTrapValidate_FGDC_Datastream", "direct:validate_FGDC_Datastream", expectedBody);
    }


    /**
     * Validation test of the FGDC Datastream for the CameraDeploymentID Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Passed() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/FGDC/valid_FGDC.xml");

        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder expectedBody = new StringBuilder();
        expectedBody.append("FGDC CameraDeploymentID|");
        expectedBody.append("//citeinfo/othercit/text()|");
        expectedBody.append("//CameraDeploymentID/text()");

        runValidationAdviceWithTest("UnifiedCameraTrapValidate_FGDC_Datastream", "direct:validate_FGDC_Datastream", expectedBody.toString());
    }

    /**
     * Validation test of the MODS Datastream for the ImageSequenceId Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_MODS_Fail() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/MODS/fail_MODS.xml");

        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        message.append("Deployment Package ID - " + camelFileParent);
        message.append(", Message - MODS ImageSequenceId Field validation failed. ");
        message.append("Expected d18981s1 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), false);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add(expectedValidationMessage);

        runValidationAdviceWithTest("UnifiedCameraTrapValidate_MODS_Datastream", "direct:validate_MODS_Datastream", expectedBody);
    }

    /**
     * Validation test of the MODS Datastream for the ImageSequenceId Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_MODS_Passed() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/MODS/valid_MODS.xml");

        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder expectedBody = new StringBuilder();
        expectedBody.append("MODS ImageSequenceId|");
        expectedBody.append("//mods:relatedItem/mods:identifier/text()|");
        expectedBody.append("//ImageSequence[1]/ImageSequenceId[1]/text()");

        runValidationAdviceWithTest("UnifiedCameraTrapValidate_MODS_Datastream", "direct:validate_MODS_Datastream", expectedBody.toString());
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservation_Passed() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/ResearcherObservation/valid_ResearcherObservationCSV.csv");

        runValidationAdviceWithTestCSV(datastreamFile, null);
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     * Fail when ImageSeqId or Observation Counts are not valid
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservation_Fail() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/ResearcherObservation/fail_ResearcherObservationCSV.csv");

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("ResearcherIdentifications CSV: Validation Failed!");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);


        runValidationAdviceWithTestCSV(datastreamFile, expectedValidationMessage);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservation_Passed() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/VolunteerObservation/valid_VolunteerObservationCSV.csv");

        runValidationAdviceWithTestCSV(datastreamFile, null);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     * Fail when ImageSeqId or Observation Counts are not valid
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservation_Fail() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/VolunteerObservation/fail_VolunteerObservationCSV.csv");

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("VolunteerIdentifications CSV: Validation Failed!");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        runValidationAdviceWithTestCSV(datastreamFile, expectedValidationMessage);
    }

    /**
     * Validation test of the CSV ImageObservation Datastream.
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ImageObservation_Passed() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/ImageObservation/valid_ImageObservationCSV.csv");

        runValidationAdviceWithTestCSV(datastreamFile, null);
    }

    /**
     * Validation test of the CSV ImageObservation Datastream.
     * Fail when ImageSeqId or Observation Counts are not valid
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ImageObservation_Fail() throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastreamFile = new File(testDataDir + "/DatastreamTestFiles/ImageObservation/fail_ImageObservationCSV.csv");

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("ImageIdentifications CSV: Validation Failed!");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        runValidationAdviceWithTestCSV(datastreamFile, expectedValidationMessage);
    }

    /**
     * Used by each csv unit test to run the validation route using adviceWith to mock for testing purposes
     * and check the assertions using the params provided as the configuration
     * @param expectedBody
     * @throws Exception
     */
    private void runValidationAdviceWithTestCSV(File datastreamFile, CameraTrapValidationMessage.MessageBean expectedBody) throws Exception {

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("UnifiedCameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                weaveAddLast().to("mock:result").stop();

            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);

        if (expectedBody != null) {
            mockEndpoint.expectedBodiesReceived(expectedBody);
        }

        template.sendBodyAndHeaders("direct:ValidateCSVFields", "test", headers);

        if (expectedBody != null) {
            Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

            log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

            log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);
        }

        assertMockEndpointsSatisfied();
    }
}
