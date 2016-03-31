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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

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

    //Validation test configuration
    private String validationTest;
    private String validationField;

    //Camera Trap Deployment Manifest and Field values
    private File manifestFile = new File("src/test/resources/SID-569TestFiles/p151d18321/deployment_manifest.xml");
    private Document manifestXML;
    private String manifest;
    private String manifestFieldValue;
    
    //Datastream and Field values
    private File datastreamFile;
    private Document datastreamXML;
    private String datastream;
    private String datastreamFieldValue;

    //Validation message bean configuration
    CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    CameraTrapValidationMessage.MessageBean expectedValidationMessage;
    List<CameraTrapValidationMessage.MessageBean> expectedBody; //The expected body that we are performing assertions on

    //Datastream Test Files and metadata field xPaths
    Map<String, File> datastreamTestFileMap;
    Map<String, ArrayList<String>> fieldXPathMap;

    //Xml parsing
    private DocumentBuilder builder;
    private XPath xPath;

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

        //Initialize xml parsing configuration
        initializeXmlParsing();

        //Initialize the Datastream test files and metadata field xPaths maps
        initializeTestDataMaps();

        //The cameratrap deployment manifest used for xml parsing
        manifestXML = builder.parse(manifestFile);

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
     * @throws Exception
     */
    private void runValidationAdviceWithTest(String validationRouteDefinition, String validateDatastreamFieldsRoute, AdviceWithRouteBuilder adviceWithRouteBuilder) throws Exception {
        //using adviceWith to mock for testing purpose
        context.getRouteDefinition(validationRouteDefinition).adviceWith(context, adviceWithRouteBuilder);

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders(validateDatastreamFieldsRoute, "test", headers);

        //Get the body from the mock endpoint for assertions
        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);

        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    /**
     * Validation test of the EAC-CPF Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_EAC_CPF_ProjectName_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "EAC-CPF_ProjectName_Fail";

        //The datastream matadata field map key
        validationField = "EAC-CPF ProjectName";


        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, false);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateEAC-CPF").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     *  Validation tests for newer manifest (see jira ticket SID-618 )

    @Test
    public void testValidate_EAC_CPF_Latitude_Fail() throws Exception {
        //setup test
        getValidationTestValues("EAC-CPF_Latitude_Fail", xpathListEAC_CPF);

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
        datastreamFieldName, false);

        expectedBody = new ArrayList<>();

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
        expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateEAC-CPF").replace().to("mock:result").stop();

            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
        (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testValidate_EAC_CPF_Longitude_Fail() throws Exception {
        //setup test
        getValidationTestValues("EAC-CPF_Longitude_Fail", xpathListEAC_CPF);

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
        datastreamFieldName, false);

        expectedBody = new ArrayList<>();

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
        expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateEAC-CPF").replace().to("mock:result").stop();

            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
        (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    //Future Test for newer Camera Trap Manifest
    @Ignore
    @Test
    public void testValidate_EAC_CPF_PublishDate_Fail() throws Exception {
        //setup test
        getValidationTestValues("EAC-CPF_PublishDate_Fail", xpathListEAC_CPF);

        //Remove once new manifest is used
        manifestFieldValue = "???";
        datastreamFieldValue = "???";

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
        datastreamFieldName, false);

        expectedBody = new ArrayList<>();

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
        expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateEAC-CPF").replace().to("mock:result").stop();
            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
        (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    //Future Test for newer Camera Trap Manifest
    @Ignore
    @Test
    public void testValidate_EAC_CPF_ProjectDataAccessandUseConstraints_Fail() throws Exception {
        //setup test
        getValidationTestValues("EAC-CPF_ProjectDataAccessandUseConstraints_Fail", xpathListEAC_CPF);

        //Remove once new manifest is used
        manifestFieldValue = "???";
        datastreamFieldValue = "???";

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
        datastreamFieldName, false);

        expectedBody = new ArrayList<>();

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
        expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                //weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(datastream)));


                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateEAC-CPF").replace().to("mock:result").stop();

            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:validateDatastreamFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
        (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }
    */

    /**
     * Validation test of the EAC-CPF Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_EAC_CPF_Passed() throws Exception {
        //The datastream test file map key
        validationTest = "EAC-CPF_Passed";

        //The datastream matadata field map key
        validationField = "EAC-CPF Passed";

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, true);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateEAC-CPF").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }


    /**
     * Validation test of the FGDC Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_CameraDeploymentID_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "FGDC_CameraDeploymentID_Fail";

        //The datastream matadata field map key
        validationField = "FGDC CameraDeploymentID";

        //Cause the previous datastream validations in the route to pass
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("EAC-CPF_Passed"));

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, false);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder =  new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the FGDC Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Bait_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "FGDC_Bait_Fail";

        //The datastream matadata field map key
        validationField = "FGDC Bait";

        //Cause the previous datastream validations in the route to pass
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("EAC-CPF_Passed"));

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, false);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder =  new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the FGDC Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Feature_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "FGDC_Feature_Fail";

        //The datastream matadata field map key
        validationField = "FGDC Feature";

        //Cause the previous datastream validations in the route to pass
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("EAC-CPF_Passed"));

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, false);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder =  new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the FGDC Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_FGDC_Passed() throws Exception {
        //The datastream test file map key
        validationTest = "FGDC_Passed";

        //The datastream matadata field map key
        validationField = "FGDC Passed";

        //Cause the previous datastream validations in the route to pass
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("EAC-CPF_Passed"));

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, true);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder =  new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the MODS Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_MODS_ImageSequenceId_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "MODS_ImageSequenceId_Fail";

        //The datastream matadata field map key
        validationField = "MODS ImageSequenceId";

        //Cause the previous datastream validations in the route to pass
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("EAC-CPF_Passed"));
        String fgdcDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("FGDC_Passed"));

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, false);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(fgdcDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getMODSDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateMODS").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the MODS Datastream for the defined Field.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_MODS_Passed() throws Exception {
        //The datastream test file map key
        validationTest = "MODS_Passed";

        //The datastream matadata field map key
        validationField = "MODS Passed";

        //Cause the previous datastream validations in the route to pass
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("EAC-CPF_Passed"));
        String fgdcDatastream = FileUtils.readFileToString(datastreamTestFileMap.get("FGDC_Passed"));

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        getValidationTestValues(validationTest, validationField, true);

        //Configure and use adviceWith to mock for testing purpose
        validationRouteDefinition = "CameraTrapValidateDatastreamFields";
        validationEndpoint = "direct:validateDatastreamFields";
        adviceWithRouteBuilder = new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(fgdcDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getMODSDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateMODS").replace().to("mock:result").stop();
            }
        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservation_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "CSV_ResearcherObservation_Fail";

        //The expected validation message
        StringBuilder expectedMessage = new StringBuilder();
        expectedMessage.append("CSV ImageSequence: 1 containing ImageSequenceId: 0000000 matches Manifest validation failed.\n");
        expectedMessage.append("Expected 2970s10,2014-03-16 11:33:32,2014-03-16 11:33:33 ");
        expectedMessage.append("but found 0000000,2014-03-16 11:33:32,2014-03-16 11:33:33.");

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        csvValidationTestValues(validationTest, expectedMessage, false);

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
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the CSV ResearcherObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_ResearcherObservation_Passed() throws Exception {
        //The datastream test file map key
        validationTest = "CSV_ResearcherObservation_Passed";

        //The expected validation message
        StringBuilder expectedMessage = new StringBuilder();
        expectedMessage.append("Validation Passed");

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        csvValidationTestValues(validationTest, expectedMessage, true);

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
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     *
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservation_Fail() throws Exception {
        //The datastream test file map key
        validationTest = "CSV_VolunteerObservation_Fail";

        //The expected validation message
        StringBuilder expectedMessage = new StringBuilder();
        expectedMessage.append("CSV ImageSequence: 1 containing ImageSequenceId: 0000000 matches Manifest validation failed.\n");
        expectedMessage.append("Expected 2970s10,2014-03-16 11:33:32,2014-03-16 11:33:33 ");
        expectedMessage.append("but found 0000000,2014-03-16 11:33:32,2014-03-16 11:33:33.");

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        csvValidationTestValues(validationTest, expectedMessage, false);

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
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Validation test of the CSV VolunteerObservation Datastream.
     * @throws Exception
     */
    @Test
    public void testValidate_CSV_VolunteerObservation_Passed() throws Exception {
        //The datastream test file map key
        validationTest = "CSV_VolunteerObservation_Passed";

        //The expected validation message
        StringBuilder expectedMessage = new StringBuilder();
        expectedMessage.append("Validation Passed");

        //Sets up the expected validation message and the datastream that normally would be provides by the fedora endpoint
        csvValidationTestValues(validationTest, expectedMessage, true);

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
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        };

        runValidationAdviceWithTest(validationRouteDefinition, validationEndpoint, adviceWithRouteBuilder);
    }

    /**
     * Initialize xml parsing configuration
     * @throws ParserConfigurationException
     */
    private void initializeXmlParsing() throws ParserConfigurationException {
        //Define the namespaces used for xml processing
        Map<String, String> namespacePrefMap = new HashMap<String, String>() {{
            put("main", "http://schemas.openxmlformats.org/spreadsheetml/2006/main");
            put("objDatastreams", "http://www.fedora.info/definitions/1/0/access/");
            put("ri", "http://www.w3.org/2001/sw/DataAccess/rf1/result");
            put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
            put("fedora", "info:fedora/fedora-system:def/relations-external#");
            put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            put("eac", "urn:isbn:1-931666-33-4");
            put("mods", "http://www.loc.gov/mods/v3");
        }};

        //Define the namespace context defined in the namespacePrefMap
        NamespaceContext namespaces = new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new NullPointerException("Null prefix");
                } else if ("xml".equals(prefix)) {
                    return XMLConstants.XML_NS_URI;
                } else if (namespacePrefMap.containsKey(prefix)){
                    return namespacePrefMap.get(prefix);
                }
                return XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        };

        //Initialize xpath parsing
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(namespaces);
    }

    /**
     * Initialize the Datastream test files and metadata field xPaths
     */
    private void initializeTestDataMaps() {
        //Map of the datastream test file locations
        datastreamTestFileMap = new HashMap<String, File>() {{
            put("EAC-CPF_ProjectName_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-projectName-EAC-CPF.xml"));
//        put("EAC-CPF_Latitude_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-latitude-EAC-CPF.xml"));
//        put("EAC-CPF_Longitude_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-longitude-EAC-CPF.xml"));
//        put("EAC-CPF_PublishDate_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-PublishDate-EAC-CPF.xml"));
//        put("EAC-ProjectDataAccessandUseConstraints_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/fail-ProjectDataAccessandUseConstraints-EAC-CPF.xml"));
            put("EAC-CPF_Passed", new File("src/test/resources/SID-569TestFiles/Datastreams/EAC-CPF/valid-EAC-CPF.xml"));
            put("FGDC_CameraDeploymentID_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/fail-CameraDeploymentID-FGDC.xml"));
            put("FGDC_Bait_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/fail-Bait-FGDC.xml"));
            put("FGDC_Feature_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/fail-Feature-FGDC.xml"));
            put("FGDC_Passed", new File("src/test/resources/SID-569TestFiles/Datastreams/FGDC/validFGDC.xml"));
            put("MODS_ImageSequenceId_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/fail-ImageSequenceId-MODS.xml"));
            put("MODS_Passed", new File("src/test/resources/SID-569TestFiles/Datastreams/MODS/validMODS.xml"));
            put("CSV_ResearcherObservation_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/ResearcherObservations/failResearcherCSV.bin"));
            put("CSV_ResearcherObservation_Passed", new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/ResearcherObservations/validResearcherCSV.bin"));
            put("CSV_VolunteerObservation_Fail", new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/VolunteerObservations/failVolunteerCSV.bin"));
            put("CSV_VolunteerObservation_Passed", new File("src/test/resources/SID-569TestFiles/Datastreams/CSV/VolunteerObservations/validVolunteerCSV.bin"));
        }};

        //Map of the datastream and deployment manifest metadata fields and there xpaths
        fieldXPathMap = new HashMap<String, ArrayList<String>>() {{

            put("EAC-CPF ProjectName", new ArrayList<String>() {
                {
                    add("//eac:eac-cpf/eac:cpfDescription/eac:identity/eac:nameEntry[1]/eac:part");
                    add("//CameraTrapDeployment/ProjectName/text()");
                }
            });

            put("EAC-CPF Latitude", new ArrayList<String>() {
                {
                    add("//eac:eac-cpf/eac:cpfDescription/eac:description/eac:place/eac:placeEntry/@latitude");
                    add("//CameraTrapDeployment/ActualLatitude/text()");
                }
            });

            put("EAC-CPF Longitude", new ArrayList<String>() {
                {
                    add("//eac:eac-cpf/eac:cpfDescription/eac:description/eac:place/eac:placeEntry/@longitude");
                    add("//CameraTrapDeployment/ActualLongitude/text()");
                }
            });

            put("EAC-CPF PublishDate", new ArrayList<String>() {
                {
                    add("//eac:eac-cpf/eac:control/eac:localControl/eac:date");
                    add("//*[@PublishDate]");
                }
            });

            put("EAC-CPF ProjectDataAccessandUseConstraints", new ArrayList<String>() {
                {
                    add("//eac:eac-cpf/eac:cpfDescription/eac:description/eac:functions/eac:function/eac:descriptiveNote/eac:p");
                    add("//*[@ProjectDataAccessandUseConstraints]");
                }
            });

            put("FGDC CameraDeploymentID", new ArrayList<String>() {
                {
                    add("//metadata/idinfo/citation/citeinfo/othercit/text()");
                    add("//CameraTrapDeployment/CameraDeploymentID/text()");
                }
            });

            put("FGDC Bait", new ArrayList<String>() {
                {
                    add("//metadata/dataqual/lineage/method[1]/methodid/methkey/text()");
                    add("//CameraTrapDeployment/Bait/text()");
                }
            });

            put("FGDC Feature", new ArrayList<String>() {
                {
                    add("//metadata/dataqual/lineage/method[2]/methodid/methkey/text()");
                    add("//CameraTrapDeployment/Feature/text()");
                }
            });

            put("MODS ImageSequenceId", new ArrayList<String>() {
                {
                    add("//mods:mods/mods:relatedItem[1]/mods:identifier[1]");
                    add("//CameraTrapDeployment/ImageSequence[1]/ImageSequenceId[1]/text()");
                }
            });
        }};
    }

    private void getValidationTestValues(String validationTest, String validationField, Boolean passedValidation) throws Exception {
        //The the datastream file for testing from the map
        datastreamFile = datastreamTestFileMap.get(validationTest);

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        //setup the datastream xml for xpath parsing
        datastreamXML = builder.parse(datastreamFile);

        StringBuilder message = new StringBuilder();

        //Check if the validation test is pass or fail and set the expected validation message
        if (passedValidation) {
            message.append("Deployment Package ID - " + camelFileParent);
            message.append(", Message - " + validationField + " Field matches the Manifest Field. Validation passed...");
        } else {
            //Get the field values from the deployment manifest and datastream xml files
            datastreamFieldValue = xPath.compile(fieldXPathMap.get(validationField).get(0)).evaluate(datastreamXML);
            manifestFieldValue = xPath.compile(fieldXPathMap.get(validationField).get(1)).evaluate(manifestXML);

            message.append("Deployment Package ID - " + camelFileParent);
            message.append(", Message - " + validationField + " Field validation failed. ");
            message.append("Expected " + manifestFieldValue + " but found " +datastreamFieldValue + ".");
        }

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                message.toString(), passedValidation);

        //The expected body that we are performing assertions on
        expectedBody = new ArrayList<>();

        //only add validation failed messages
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }
    }

    private void csvValidationTestValues(String validationTest, StringBuilder csvMessage, Boolean passedValidation) throws Exception {
        //The the datastream file for testing from the map
        datastreamFile = datastreamTestFileMap.get(validationTest);

        //The datastream that will be used in adviceWith to replace the getDatastreamDissemination endpoint
        // with the same exchange body that fedora would return
        datastream = FileUtils.readFileToString(datastreamFile);

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                csvMessage.toString(), passedValidation);

        //The expected body that we are performing assertions on
        expectedBody = new ArrayList<>();

        //only add validation failed messages
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }
    }

}
