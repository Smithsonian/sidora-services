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
import org.apache.camel.model.SplitDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the Camera Trap Validate Fields Route
 * Created by jbirkhimer on 2/29/16.
 */
public class DatastreamFieldValidationTest extends CamelBlueprintTestSupport {

    //Camera Trap Deployment Info
    private String camelFileParent = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint and AdviceWith configuration to be used
    private MockEndpoint mockEndpoint;
    private AdviceWithRouteBuilder adviceWithRouteBuilder;
    private String validationEndpoint;
    private String validationRouteDefinition;

    //Camel Headers Map
    private Map<String, Object> headers;


    //Camera Trap Deployment Manifest and Field values
    private File manifestFile = new File("src/test/resources/SID-569TestFiles/p151d18321/deployment_manifest.xml");
    private Document manifestXML;
    private String manifest;
    
    //Datastream and Field values
    private File datastreamFile;
    private String datastream;

    //Validation message bean configuration
    CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    CameraTrapValidationMessage.MessageBean expectedValidationMessage;

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
     * Used by each unit test to run the validation route using adviceWith to mock for testing purposes
     * and check the assertions using the params provided as the configuration
     * @param validationRouteDefinition The route id
     * @param validateDatastreamFieldsRoute The route endpoint
     * @param adviceWithRouteBuilder The AdviceWithRouteBuilder configuration
     * @param expectedBody
     * @throws Exception
     */
    private void runValidationAdviceWithTest(String validationRouteDefinition, String validateDatastreamFieldsRoute, AdviceWithRouteBuilder adviceWithRouteBuilder, ArrayList expectedBody) throws Exception {

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition(validationRouteDefinition).adviceWith(context, adviceWithRouteBuilder);

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders(validateDatastreamFieldsRoute, "test", headers);

        //Get the body from the mock endpoint for assertions
        /*ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();*/

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    /**
     * Validation test of the EAC-CPF Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_EAC_CPF_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-projectName-EAC-CPF.xml");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        message.append("Datastream EAC-CPF ProjectName Field Validation failed");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), false);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add(expectedValidationMessage);

        //Configure and use adviceWith to mock for testing purpose
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest("Validate_EAC-CPF_Datastream", "direct:validate_EAC-CPF_Datastream", adviceWithRouteBuilder, expectedBody);
    }

    /**
     * Validation test of the EAC-CPF Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_EAC_CPF_Passed() throws Exception {

        datastreamFile = new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/valid-EAC-CPF.xml");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add("EAC-CPF ProjectName");
        expectedBody.add("//eac:nameEntry[1]/eac:part/text()");
        expectedBody.add("//ProjectName/text()");

        //Configure and use adviceWith to mock for testing purpose
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest("Validate_EAC-CPF_Datastream", "direct:validate_EAC-CPF_Datastream", adviceWithRouteBuilder, expectedBody);
    }


    /**
     * Validation test of the FGDC Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/fail-CameraDeploymentID-FGDC.xml");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        message.append("Datastream FGDC CameraDeploymentID Field Validation failed");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), false);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add(expectedValidationMessage);

        adviceWithRouteBuilder =  new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest("Validate_FGDC_Datastream", "direct:validate_FGDC_Datastream", adviceWithRouteBuilder, expectedBody);
    }


    /**
     * Validation test of the FGDC Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Passed() throws Exception {

        datastreamFile = new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/validFGDC.xml");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add("FGDC CameraDeploymentID");
        expectedBody.add("//citeinfo/othercit/text()");
        expectedBody.add("//CameraDeploymentID/text()");

        adviceWithRouteBuilder =  new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest("Validate_FGDC_Datastream", "direct:validate_FGDC_Datastream", adviceWithRouteBuilder, expectedBody);
    }

    /**
     * Validation test of the MODS Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_MODS_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/fail-ImageSequenceId-MODS.xml");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        StringBuilder message = new StringBuilder();
        message.append("Datastream MODS ImageSequenceId Field Validation failed");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), false);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add(expectedValidationMessage);

        //Configure and use adviceWith to mock for testing purpose
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest("Validate_MODS_Datastream", "direct:validate_MODS_Datastream", adviceWithRouteBuilder, expectedBody);
    }

    /**
     * Validation test of the MODS Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_MODS_Passed() throws Exception {

        datastreamFile = new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/validMODS.xml");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        ArrayList expectedBody = new ArrayList<>();
        expectedBody.add("MODS ImageSequenceId");
        expectedBody.add("//mods:relatedItem/mods:identifier/text()");
        expectedBody.add("//ImageSequence[1]/ImageSequenceId[1]/text()");

        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest("Validate_MODS_Datastream", "direct:validate_MODS_Datastream", adviceWithRouteBuilder, expectedBody);
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservation_Passed() throws Exception {

        datastreamFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/ResearcherObservationPASS.csv");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifestFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/deployment_manifest.xml");

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        /*//The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("Validation Passed");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);*/

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint


        ArrayList expectedBody = new ArrayList();
        expectedBody.add(Arrays.asList(datastream.split("\\s*,\\s*")));



        //Configure and use adviceWith to mock for testing purpose
        validationEndpoint = "direct:ValidateCSVFields";
        validationRouteDefinition = "CameraTrapValidateCSVFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveAddLast().to("mock:result").stop();
            }

        };

        //runValidationAdviceWithTestCSV(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder, expectedBody);
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservation_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/ResearcherObservationFAIL.csv");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifestFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/deployment_manifest.xml");

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("ResearcherIdentifications CSV: Validation Failed!");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        //Configure and use adviceWith to mock for testing purpose
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveAddLast().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTestCSV("CameraTrapValidateCSVFields", "direct:ValidateCSVFields", adviceWithRouteBuilder, expectedValidationMessage);
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservationCounts_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/ResearcherObservationCountsFAIL.csv");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifestFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/deployment_manifest.xml");

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("ResearcherIdentifications CSV: Validation Failed!");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        //Configure and use adviceWith to mock for testing purpose
        validationEndpoint = "direct:ValidateCSVFields";
        validationRouteDefinition = "CameraTrapValidateCSVFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveAddLast().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTestCSV(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder, expectedValidationMessage);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservation_Passed() throws Exception {

        datastreamFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/VolunteerObservationPASS.csv");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifestFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/deployment_manifest.xml");

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("Validation Passed");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        //Configure and use adviceWith to mock for testing purpose
        validationEndpoint = "direct:ValidateCSVFields";
        validationRouteDefinition = "CameraTrapValidateCSVFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveAddLast().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTestCSV(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder, expectedValidationMessage);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservation_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/VolunteerObservationFAIL.csv");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifestFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/deployment_manifest.xml");

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("CSV Observation: 5 containing ImageSequenceId: d18434s13 does not match the Manifest... Validation Failed!\n");
        csvMessage.append("Expected d18434s13,2015-08-10 07:01:38,2015-08-10 07:02:31 ");
        csvMessage.append("but found d18434s13,2000-08-10 07:01:38,2015-08-10 07:02:31.");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        //Configure and use adviceWith to mock for testing purpose
        validationEndpoint = "direct:ValidateCSVFields";
        validationRouteDefinition = "CameraTrapValidateCSVFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveAddLast().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTestCSV(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder, expectedValidationMessage);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservationCounts_Fail() throws Exception {

        datastreamFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/VolunteerObservationCountsFAIL.csv");

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        manifestFile = new File("src/test/resources/SID-647_TestFiles/scbi_deployments_validation/fail/3191d18434/deployment_manifest.xml");

        manifest = FileUtils.readFileToString(manifestFile);

        headers.put("ManifestXML", String.valueOf(manifest));

        //The expected validation message
        StringBuilder csvMessage = new StringBuilder();
        csvMessage.append("VolunteerIdentifications CSV: Validation Failed!");

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), false);

        //Configure and use adviceWith to mock for testing purpose
        validationEndpoint = "direct:ValidateCSVFields";
        validationRouteDefinition = "CameraTrapValidateCSVFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveAddLast().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTestCSV(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder, expectedValidationMessage);
    }

    /**
     * Used by each unit test to run the validation route using adviceWith to mock for testing purposes
     * and check the assertions using the params provided as the configuration
     * @param validationRouteDefinition The route id
     * @param validateDatastreamFieldsRoute The route endpoint
     * @param adviceWithRouteBuilder The AdviceWithRouteBuilder configuration
     * @param expectedBody
     * @throws Exception
     */
    private void runValidationAdviceWithTestCSV(String validationRouteDefinition, String validateDatastreamFieldsRoute, AdviceWithRouteBuilder adviceWithRouteBuilder, CameraTrapValidationMessage.MessageBean expectedBody) throws Exception {

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition(validationRouteDefinition).adviceWith(context, adviceWithRouteBuilder);

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody);

        template.sendBodyAndHeaders(validateDatastreamFieldsRoute, "test", headers);

        //Get the body from the mock endpoint for assertions
        /*ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();*/

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

}
