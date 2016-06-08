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
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.*;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author jbirkhimer
 */
public class UnifiedCameraTrapPostValidationTest extends CamelBlueprintTestSupport {

    //Camera Trap Deployment Info
    private String camelFileParent = "10002000";
    private int ManifestCameraDeploymentId = 0000;

    //Mock endpoint and AdviceWith configuration to be used
    private MockEndpoint mockEndpoint;

    //Camel Headers Map
    private Map<String, Object> headers;

    //Camera Trap Deployment Manifest and Field values
    private File manifestFile = new File("src/test/resources/UnifiedManifest-TestFiles/scbi_unified_stripped_p125d18981/deployment_manifest.xml");
    private String manifest;

    //Datastream and Field values
    private String datastream;

    //Validation message bean configuration
    private CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    private CameraTrapValidationMessage.MessageBean expectedValidationMessage;

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

        // TODO: Look into using the Karaf-config directory for karaf.home and system.properties file
        //Set the karaf.home property use by the camera trap route
        System.setProperty("karaf.home", "src/test/resources/SID-569TestFiles");

        //Define the Process directory that the camera trap route creates
        // to be able to clean the project up after tests
        processDirectory = new File("ProcessUnified");

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
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = new Properties();
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("Karaf-config/edu.si.sidora.karaf.cfg");

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
        return "Route/unified-camera-trap-route.xml";
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
     * UnifiedCameraTrapValidatePostIngestResourceCount Route (direct:validatePostIngestResourceCount) Test
     */
    //@Test
    public void unifiedCameraTrapValidatePostIngestResourceCount_Test() throws Exception {
        // RELS-EXT Resource reference count for validation. The number includes the observation resource objs
        headers.put("ResourceCount", "6"); //Header that's incremented after a resource obj is ingested.
        headers.put("SitePID", "ct:16593");

        //The RELS-EXT datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        String datastream_RELS_EXTfilePath = "src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/resource_RELS-EXT.xml";
        datastream = FileUtils.readFileToString(new File(datastream_RELS_EXTfilePath));

        InputStream datastream_RELS_EXT = new FileInputStream(datastream_RELS_EXTfilePath);

        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("UnifiedCameraTrapValidatePostIngestResourceCount").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                //weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(datastream));

                //weaveByToString(".*fcrepo:objects.*").replace().setBody(simple(fcrepo_objectResponce));


                /*weaveByType(ToDynamicDefinition.class)
                        .after()
                        .to("mock:result")
                        .stop();*/

                //weaveAddLast().to("mock:result").stop();

            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        /*if (!postResourceCountMatchPassed) {
            mockEndpoint.expectedBodiesReceived(expectedBody.toString());
        } else {
            mockEndpoint.expectedBodiesReceived(datastream);
        }*/

        template.sendBodyAndHeaders("direct:validatePostIngestResourceCount", datastream_RELS_EXT, headers);

        /*Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        if (!postResourceCountMatchPassed) {
            log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
            log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        } else {
            log.info("expectedBody:\n" + datastream + "\nresultBody:\n" + resultBody);
            log.info("expectedBody Type:\n" + datastream.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", datastream, resultBody);
        }*/

        assertMockEndpointsSatisfied();
    }

    /**
     * CameraTrapValidatePostResourceCount Route (direct:validatePostResourceCount) Test
     */
    @Test
    public void cameraTrapValidatePostResourceCount_Test() throws Exception {

        // RELS-EXT Resource reference count for validation. The number includes the observation resource objs
        headers.put("RelsExtResourceCount", "5");
        headers.put("ResourceCount", "6"); //Header that's incremented after a resource obj is ingested.

        //The RELS-EXT datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/resource_RELS-EXT.xml"));

        String routeId = "CameraTrapValidatePostResourceCount";
        String routeURI = "direct:validatePostResourceCount";

        headers.put("ValidationErrors", "ValidationErrors");

        StringBuilder message = new StringBuilder();
        message.append("Post Resource Count validation failed. ");
        message.append("Expected " + headers.get("ResourceCount") + " but found " + headers.get("RelsExtResourceCount"));

        boolean postResourceCountMatchPassed = headers.get("RelsExtResourceCount").equals(headers.get("ResourceCount"));

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), postResourceCountMatchPassed);

        //only add validation failed messages to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition(routeId).adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                //weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //Modify the route if testing post resource counts match failed
                if (!postResourceCountMatchPassed) {
                    weaveByToString(".*direct:validationErrorMessageAggregationStrategy.*")
                            .before()
                            .setHeader("ValidationComplete", simple("true"));
                    weaveAddLast().stop();
                } else {

                    weaveAddLast().to("mock:result").stop();
                }
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        if (!postResourceCountMatchPassed) {
            mockEndpoint.expectedBodiesReceived(expectedBody.toString());
        } else {
            mockEndpoint.expectedBodiesReceived(datastream);
        }

        template.sendBodyAndHeaders(routeURI, datastream, headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        if (!postResourceCountMatchPassed) {
            log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
            log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        } else {
            log.info("expectedBody:\n" + datastream + "\nresultBody:\n" + resultBody);
            log.info("expectedBody Type:\n" + datastream.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

            assertEquals("mock:result Body assertEquals failed!", datastream, resultBody);
        }

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing the CameraTrapValidateDatastreamFields route
     * All Datastream Field Validations Fail
     * @throws Exception
     */
    @Test
    public void validation_FailTest() throws Exception {
        setupEAC_CPF(false, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/EAC-CPF/fail_EAC_CPF.xml"));
        setupFGDC(false, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/FGDC/fail_FGDC.xml"));
        setupMODS(false, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/MODS/fail_MODS.xml"));

        File[] datastreamFileCSV = {
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ResearcherObservation/fail_ResearcherObservationCSV.csv"),
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/VolunteerObservation/fail_VolunteerObservationCSV.csv"),
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ImageObservation/fail_ImageObservationCSV.csv")
        };

        headers.put("VolunteerObservationPID", "test:00000");
        headers.put("ImageObservationPID", "test:00000");

        setupCSV_ValidationAdviceWith(false, false, false, datastreamFileCSV);
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
        setupEAC_CPF(true, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/EAC-CPF/valid_EAC_CPF.xml"));
        setupFGDC(true, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/FGDC/valid_FGDC.xml"));
        setupMODS(true, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/MODS/valid_MODS.xml"));

        File[] datastreamFileCSV = {
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ResearcherObservation/valid_ResearcherObservationCSV.csv"),
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/VolunteerObservation/valid_VolunteerObservationCSV.csv"),
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ImageObservation/valid_ImageObservationCSV.csv")
        };

        headers.put("VolunteerObservationPID", "test:00000");
        headers.put("ImageObservationPID", "test:00000");

        setupCSV_ValidationAdviceWith(true, true, true, datastreamFileCSV);
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
        setupEAC_CPF(true, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/EAC-CPF/valid_EAC_CPF.xml"));
        setupFGDC(false, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/FGDC/fail_FGDC.xml"));
        setupMODS(true, new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/MODS/valid_MODS.xml"));

        File[] datastreamFileCSV = {
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ResearcherObservation/valid_ResearcherObservationCSV.csv"),
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/VolunteerObservation/fail_VolunteerObservationCSV.csv"),
                new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ImageObservation/valid_ImageObservationCSV.csv")
        };

        headers.put("VolunteerObservationPID", "test:00000");
        headers.put("ImageObservationPID", "test:00000");

        setupCSV_ValidationAdviceWith(true, false, true, datastreamFileCSV);
        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Start Running the validation tests
        validate_DatastreamFieldsRoute_IT();
    }

    /**
     * Testing the UnifiedCameraTrapValidateCSVFields route
     * @throws Exception
     */
    @Test
    public void unifiedCameraTrapValidateCSVFields_ResearcherObservation_Test() throws Exception {

        //RouteId and endpoint uri
        String routeId = "UnifiedCameraTrapValidateCSVFields";
        String routeURI = "direct:ValidateCSVFields";

        File ResearcherObservationCSV_DatastreamTestFile = new File("src/test/resources/UnifiedManifest-TestFiles/DatastreamTestFiles/ResearcherObservation/valid_ResearcherObservationCSV.csv");

        String ResearcherObservationCSV_DatastreamExpected = FileUtils.readFileToString(ResearcherObservationCSV_DatastreamTestFile);

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "ResearcherIdentifications CSV: Validation Failed!", true);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        setupValidationErrorMessageAggregationStrategyAdviceWith();

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition(routeId).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //Remove the setheader definition for FITSCreatedDate we are manually passing this header in already
                //weaveByType(SetHeaderDefinition.class).selectFirst().remove();
                //set the mockEndpoint result and stop the route before adding the MODS datastream to fedora
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(ResearcherObservationCSV_DatastreamExpected)));
                weaveAddLast().to("direct:validationErrorMessageAggregationStrategy").setHeader("ValidationComplete", simple("true")).to("direct:validationErrorMessageAggregationStrategy").stop();
            }
        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        /*mockEndpoint.expectedBodyReceived().body().isEqualTo(ResearcherObservationCSV_DatastreamExpected);
        mockEndpoint.expectedBodiesReceived(ResearcherObservationCSV_DatastreamExpected);*/

        template.sendBodyAndHeaders(routeURI, "testCSV", headers);

        Object resultBody = mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        log.info("expectedBody Type:\n" + expectedBody.getClass() + "\nresultBody Type:\n" + resultBody.getClass());

        assertEquals("mock:result Body containing MODS datastream xml assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();

    }

    /**
     * Method that runs the CameraTrapValidateDatastreamFields route using adviceWith to mock for testing purposes
     * and check the assertions
     * @throws Exception
     */
    public void validate_DatastreamFieldsRoute_IT() throws Exception {

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("UnifiedCameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                // need to set header for the test created in the setupCSV advicewithroutebuilder so that
                // the correct observation csv datastream is used.
                weaveByToString(".*validationErrorMessageAggregationStrategy.*").selectFirst().after().setHeader("testingVolunteerObservation", simple("true"));
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
        message.append("Expected Sample Triangle Camera Trap Survey Project (p125) but found Sample Blah Blah Blah Project (p125).");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("UnifiedCameraTrapValidate_EAC-CPF_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

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
        message.append("Expected d18981 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        };

        context.getRouteDefinition("UnifiedCameraTrapValidate_FGDC_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

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
        message.append("Expected d18981s1 but found blahblah.");

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), pass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //Configure and use adviceWith to mock for testing purpose
        context.getRouteDefinition("UnifiedCameraTrapValidate_MODS_Datastream").adviceWith(context, new AdviceWithRouteBuilder() {

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
    private void setupCSV_ValidationAdviceWith(boolean researcherPass, boolean volunteerPass, boolean imagePass, File[] datastreamFileCSV) throws Exception {

        //The researcher or volunteer datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        String researcherCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[0]);
        String volunteerCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[1]);
        String imageCSVdatastream = FileUtils.readFileToString(datastreamFileCSV[2]);

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

        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "ImageIdentifications CSV: Validation Failed!", imagePass);

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("UnifiedCameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // The csv route is modified slightly only for testing purposes. A header is created in the
                // validate_DatastreamFieldsRoute_IT advicewithroutebuilder after the researcher csv validation completes
                // The modified csv validation route will use the volunteer datastream if the header value is true.
                weaveByToString(".*getDatastreamDissemination.*").replace()
                        .choice()
                        .when(header("testingImageObservation").isEqualTo("true"))
                        .setBody(simple(String.valueOf(imageCSVdatastream)))
                        .when(header("testingVolunteerObservation").isEqualTo("true"))
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
        context.getRouteDefinition("UnifiedCameraTrapValidationErrorMessageAggregationStrategy").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                weaveByType(LogDefinition.class).after().to("mock:result");
            }
        });
    }
}
