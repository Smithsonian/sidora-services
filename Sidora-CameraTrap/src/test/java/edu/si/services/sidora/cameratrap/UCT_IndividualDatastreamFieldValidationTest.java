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
 * Tests for the Camera Trap Validate Fields Route
 *
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
public class UCT_IndividualDatastreamFieldValidationTest {

    private static final Logger log = LoggerFactory.getLogger(UCT_IndividualDatastreamFieldValidationTest.class);

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

    //Datastream and Field values
    private File datastreamFile;
    private String datastream;

    //Validation message bean configuration
    private CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    private CameraTrapValidationMessage.MessageBean expectedValidationMessage;

    /**
     * Initialize the camel headers, deployment manifest, and test data
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        //Store the Deployment Manifest as string to set the camel ManifestXML header
        manifest = FileUtils.readFileToString(manifestFile);

        //Initialize the expected camel headers
        headers = new HashMap<>();
        headers.put("deploymentPackageId", deploymentPackageId);
        headers.put("ManifestCameraDeploymentId", ManifestCameraDeploymentId);
        headers.put("ManifestXML", String.valueOf(manifest));
        headers.put("validationErrors", "validationErrors");
        headers.put("ProjectPID", "test:0000");
        headers.put("SitePID", "test:0000");
        headers.put("modsImageSequenceId", "d18981s1");
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
        AdviceWith.adviceWith(context, validationRouteDefinition,false, a -> {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return but modified for our test
                a.weaveByToString(".*getDatastreamDissemination.*").replace().setBody().simple(String.valueOf(datastream));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                a.weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
        });

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders(validateDatastreamFieldsRoute, "test", headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals(expectedBody, resultBody, "mock:result Body assertEquals failed!");

        mockEndpoint.assertIsSatisfied();
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
        message.append("Deployment Package ID - " + deploymentPackageId);
        message.append(", Message - EAC-CPF ProjectName Field validation failed. ");
        message.append("Expected Sample Triangle Camera Trap Survey Project but found Sample Blah Blah Blah Project.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
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
        message.append("Deployment Package ID - " + deploymentPackageId);
        message.append(", Message - FGDC CameraDeploymentID Field validation failed. ");
        message.append("Expected d18981 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
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
        message.append("Deployment Package ID - " + deploymentPackageId);
        message.append(", Message - MODS ImageSequenceId Field validation failed. ");
        message.append("Expected d18981s1 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
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
        expectedBody.append("//ImageSequenceId[text()=\"d18981s1\"]/text()");

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
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
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
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
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
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(deploymentPackageId,
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
        AdviceWith.adviceWith(context, "UnifiedCameraTrapValidateCSVFields",false, a -> {

                a.weaveByToString(".*getDatastreamDissemination.*").replace().setBody().simple(String.valueOf(datastream));

            a.weaveAddLast().to("mock:result").stop();
        });

        mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);

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

            assertEquals(expectedBody, resultBody, "mock:result Body assertEquals failed!");
        }

        mockEndpoint.assertIsSatisfied();
    }
}
