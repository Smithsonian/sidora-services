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

    private static File tempDirectory, processDirectory;

    private DocumentBuilder builder;
    private Document manifestXML;
    private Document datastreamXML;
    private XPath xPath;

    private MockEndpoint mockEndpoint;

    //Manifest
    private File manifestFile = new File("src/test/resources/SID-569TestFiles/p151d18321/deployment_manifest.xml");
    private String manifest;
    private String manifestFieldValue;
    
    //Datastream
    private File datastreamFile;
    private String datastream;
    private String datastreamFieldName;
    private String datastreamFieldValue;
    
    //Camera Trap Deployment Info
    private String camelFileParent = "10002000";
    private int ManifestCameraDeploymentId = 0000;
    private int CameraDeploymentId = 0000;    
    
    private Map<String, Object> headers = new HashMap<>();
    CameraTrapValidationMessage cameraTrapValidationMessage = new CameraTrapValidationMessage();
    CameraTrapValidationMessage.MessageBean expectedValidationMessage;
    List<CameraTrapValidationMessage.MessageBean> expectedBody;

    Map<String, File> datastreamTestFileList = new HashMap<String, File>() {{
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

    String[][] xpathListEAC_CPF = {
            {"EAC-CPF ProjectName", "//eac:eac-cpf/eac:cpfDescription/eac:identity/eac:nameEntry[1]/eac:part", "//CameraTrapDeployment/ProjectName/text()"}
            //{"EAC-CPF Latitude", "//eac:eac-cpf/eac:cpfDescription/eac:description/eac:place/eac:placeEntry/@latitude", "//CameraTrapDeployment/ActualLatitude/text()"},
            //{"EAC-CPF Longitude", "//eac:eac-cpf/eac:cpfDescription/eac:description/eac:place/eac:placeEntry/@longitude", "//CameraTrapDeployment/ActualLongitude/text()"},
            //{"EAC-CPF PublishDate", "//eac:eac-cpf/eac:control/eac:localControl/eac:date", "//*[@PublishDate]"},
            //{"EAC-CPF ProjectDataAccessandUseConstraints", " //eac:eac-cpf/eac:cpfDescription/eac:description/eac:functions/eac:function/eac:descriptiveNote/eac:p", "//*[@ProjectDataAccessandUseConstraints]"}
    };

    String[][] xpathListFGDC = {
            {"FGDC CameraDeploymentID", "//metadata/idinfo/citation/citeinfo/othercit/text()", "//CameraTrapDeployment/CameraDeploymentID/text()"},
            {"FGDC Bait", "//metadata/dataqual/lineage/method[1]/methodid/methkey/text()", "//CameraTrapDeployment/Bait/text()"},
            {"FGDC Feature", "//metadata/dataqual/lineage/method[2]/methodid/methkey/text()", "//CameraTrapDeployment/Feature/text()"},
    };

    String[][] xpathListMODS = {
            {"MODS ImageSequenceId", "//mods:mods/mods:relatedItem[1]/mods:identifier[1]", "//CameraTrapDeployment/ImageSequence[1]/ImageSequenceId[1]/text()"}
    };

    /*String[][] xpathListCSV = {
            {"//CameraTrapDeployment/ImageSequence[]/ImageSequenceId", "//CameraTrapDeployment/ImageSequence[$in:xpathIndex]/ImageSequenceBeginTime", "//CameraTrapDeployment/ImageSequence[$in:xpathIndex]/ImageSequenceEndTime)"}
    };*/

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

    @BeforeClass
    public static void setSystemProps() {
        System.setProperty("karaf.home", "src/test/resources/SID-569TestFiles");
    }

    @BeforeClass
    public static void setup() throws IOException {
        processDirectory = new File("Process");
        tempDirectory = new File("Input");
        if(!tempDirectory.exists()){
            tempDirectory.mkdir();
        }

        File srcDir = new File("../../Routes/Camera Trap/Input");

        FileUtils.copyDirectory(srcDir, tempDirectory);
    }

    @AfterClass
    public static void teardown() throws IOException {
        if(tempDirectory.exists()){
            FileUtils.deleteDirectory(tempDirectory); //apache-commons-io
        }
        if(processDirectory.exists()){
            FileUtils.deleteDirectory(processDirectory); //apache-commons-io
        }
    }

    // override this method, and return the location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        //use the production route for testing that the pom copied into the test resources
        return "Route/camera-trap-route.xml";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        //Initialize Camera Trap Deployment Manifest xml for xpath parsing
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        manifestXML = builder.parse(manifestFile);
        xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(namespaces);

        //Store the Deployment Manifest as string to set the camel ManifestXML header
        manifest = FileUtils.readFileToString(manifestFile);

        //setting up expected camel headers for the validation route
        headers.put("CamelFileParent", camelFileParent);
        headers.put("ManifestCameraDeploymentId", ManifestCameraDeploymentId);
        headers.put("ManifestXML", String.valueOf(manifest));
        headers.put("ValidationErrors", "ValidationErrors");
        headers.put("ProjectPID", "ct:0000");
        headers.put("SitePID", "ct:0000");
    }

    private void getValidationTestValues(String validationTest, String[][] xpathList,Integer row) throws Exception {
        datastreamFile = datastreamTestFileList.get(validationTest);
        datastream = FileUtils.readFileToString(datastreamFile); //save the datastream as string for route

        if (validationTest.contains("CSV")) {
            StringBuilder sb = new StringBuilder();

            int imageSeqIdCount = Integer.parseInt(xPath.compile("count(/CameraTrapDeployment/ImageSequence/ImageSequenceId)").evaluate(manifestXML));

            for (int i = 0; i < imageSeqIdCount; i++) {
                //"//CameraTrapDeployment/ImageSequence[]/ImageSequenceId", "//CameraTrapDeployment/ImageSequence[$in:xpathIndex]/ImageSequenceBeginTime", "//CameraTrapDeployment/ImageSequence[$in:xpathIndex]/ImageSequenceEndTime)"}
                sb.append(xPath.compile("//CameraTrapDeployment/ImageSequence[" + i + "]/ImageSequenceId").evaluate(manifestXML));
                sb.append(",");
                sb.append(xPath.compile("//CameraTrapDeployment/ImageSequence[" + i + "]/ImageSequenceBeginTime").evaluate(manifestXML));
                sb.append(",");
                sb.append(xPath.compile("//CameraTrapDeployment/ImageSequence[" + i + "]/ImageSequenceEndTime").evaluate(manifestXML));
            }

            manifestFieldValue = sb.toString();

        } else {
            datastreamXML = builder.parse(datastreamFile); //setup the datastream xml for xpath parsing
            datastreamFieldName = xpathList[row][0];
            datastreamFieldValue = xPath.compile(xpathList[row][1]).evaluate(datastreamXML);
            manifestFieldValue = xPath.compile(xpathList[row][2]).evaluate(manifestXML);
        }
    }

    @Test
    public void testValidate_EAC_CPF_ProjectName_Fail() throws Exception {
        //setup test
        getValidationTestValues("EAC-CPF_ProjectName_Fail", xpathListEAC_CPF, 0);

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

    @Test
    public void testValidate_EAC_CPF_Passed() throws Exception {
        //setup test
        getValidationTestValues("EAC-CPF_Passed", xpathListEAC_CPF, 0);

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                datastreamFieldName, true);

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
    public void testValidate_FGDC_CameraDeploymentID_Fail() throws Exception {
        //The test datastream
        getValidationTestValues("FGDC_CameraDeploymentID_Fail", xpathListFGDC, 0);
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileList.get("EAC-CPF_Passed"));

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
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();

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
    public void testValidate_FGDC_Bait_Fail() throws Exception {
        //The test datastream
        getValidationTestValues("FGDC_Bait_Fail", xpathListFGDC, 1);
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileList.get("EAC-CPF_Passed"));

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
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();

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
    public void testValidate_FGDC_Feature_Fail() throws Exception {
        //The test datastream
        getValidationTestValues("FGDC_Feature_Fail", xpathListFGDC, 2);
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileList.get("EAC-CPF_Passed"));

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
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();

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
    public void testValidate_FGDC_Passed() throws Exception {
        //The test datastream
        getValidationTestValues("FGDC_Passed", xpathListFGDC, 2);
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileList.get("EAC-CPF_Passed"));

        expectedBody = new ArrayList<>();

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateFGDC").replace().to("mock:result").stop();

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
    public void testValidate_MODS_ImageSequenceId_Fail() throws Exception {
        //The test datastream
        getValidationTestValues("MODS_ImageSequenceId_Fail", xpathListMODS, 0);
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileList.get("EAC-CPF_Passed"));
        String fgdcDatastream = FileUtils.readFileToString(datastreamTestFileList.get("FGDC_Passed"));

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
                weaveById("getEAC-CPFDatastream").replace().setBody(simple(String.valueOf(eac_cpfDatastream)));
                weaveById("getFGDCDatastream").replace().setBody(simple(String.valueOf(fgdcDatastream)));

                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveById("getMODSDatastream").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveById("aggregateMODS").replace().to("mock:result").stop();

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
    public void testValidate_MODS_Passed() throws Exception {
        //The test datastream
        getValidationTestValues("MODS_Passed", xpathListMODS, 0);
        String eac_cpfDatastream = FileUtils.readFileToString(datastreamTestFileList.get("EAC-CPF_Passed"));
        String fgdcDatastream = FileUtils.readFileToString(datastreamTestFileList.get("FGDC_Passed"));

        //creating a new messageBean that is expected from the test route
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                datastreamFieldName, true);

        expectedBody = new ArrayList<>();

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateDatastreamFields").adviceWith(context, new AdviceWithRouteBuilder() {

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
    public void testValidate_CSV_ResearcherObservation_Fail() throws Exception {
        //The test datastream
        getValidationTestValues("CSV_ResearcherObservation_Fail", null, 0);

        //creating a new Arraylist of messageBean that is expected from the test route
        expectedBody = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("CSV ImageSequence: 1 containing ImageSequenceId: 0000000 matches Manifest validation failed.\n");
        sb.append("Expected 2970s10,2014-03-16 11:33:32,2014-03-16 11:33:33 ");
        sb.append("but found 0000000,2014-03-16 11:33:32,2014-03-16 11:33:33.");
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                sb.toString(), false);
        expectedBody.add(expectedValidationMessage);

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:ValidateCSVFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testValidate_CSV_ResearcherObservation_Passed() throws Exception {
        //The test datastream
        getValidationTestValues("CSV_ResearcherObservation_Passed", null, 0);

        expectedBody = new ArrayList<>();

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:ValidateCSVFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testValidate_CSV_VolunteerObservation_Fail() throws Exception {
        //The test datastream
        getValidationTestValues("CSV_VolunteerObservation_Fail", null, 1);

        //creating a new Arraylist of messageBean that is expected from the test route
        expectedBody = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("CSV ImageSequence: 1 containing ImageSequenceId: 0000000 matches Manifest validation failed.\n");
        sb.append("Expected 2970s10,2014-03-16 11:33:32,2014-03-16 11:33:33 ");
        sb.append("but found 0000000,2014-03-16 11:33:32,2014-03-16 11:33:33.");
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                sb.toString(), false);
        expectedBody.add(expectedValidationMessage);

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:ValidateCSVFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testValidate_CSV_VolunteerObservation_Passed() throws Exception {
        //The test datastream
        getValidationTestValues("CSV_VolunteerObservation_Passed", null, 0);

        //creating a new Arraylist of messageBean that is expected from the test route
        expectedBody = new ArrayList<>();
        expectedValidationMessage = cameraTrapValidationMessage.createValidationMessage(camelFileParent,
                "Validation Passed", true);
        expectedBody.add(expectedValidationMessage);

        expectedBody = new ArrayList<>();

        //only add validation failed messages  to the bucket
        if (!expectedValidationMessage.getValidationSuccess()) {
            expectedBody.add(expectedValidationMessage);
        }

        //using adviceWith to mock for testing purpose
        context.getRouteDefinition("CameraTrapValidateCSVFields").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                //replace the getDatastreamDissemination endpoint with the same exchange body that fedora would return
                weaveByToString(".*getDatastreamDissemination.*").replace().setBody(simple(String.valueOf(datastream)));

                //replace the validationErrorMessage Aggregation with mock:result and stop the route from continuing
                //weaveByToString(".*validationErrorMessageAggregationStrategy.*").replace().to("mock:result").stop();
                weaveByType(SplitDefinition.class).after().to("mock:result").stop();
            }

        });

        mockEndpoint = getMockEndpoint("mock:result");

        // set mock expectations
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedBodiesReceived(expectedBody.toString());

        template.sendBodyAndHeaders("direct:ValidateCSVFields", "test", headers);

        ArrayList<CameraTrapValidationMessage.MessageBean> resultBody =
                (ArrayList<CameraTrapValidationMessage.MessageBean>) mockEndpoint.getExchanges().get(0).getIn().getBody();

        log.info("expectedBody:\n" + expectedBody + "\nresultBody:\n" + resultBody);
        assertEquals("mock:result Body assertEquals failed!", expectedBody, resultBody);

        assertMockEndpointsSatisfied();
    }


}
