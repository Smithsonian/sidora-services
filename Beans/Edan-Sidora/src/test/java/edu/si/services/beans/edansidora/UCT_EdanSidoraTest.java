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

package edu.si.services.beans.edansidora;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.FilterDefinition;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class UCT_EdanSidoraTest extends EDAN_CT_BlueprintTestSupport {

    protected static final String EDAN_TEST_URI = System.getProperty("si.ct.uscbi.server");
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static final String JMS_TEST_QUEUE = "edanIds.apim.update.test";

    private static File testManifest = new File(KARAF_HOME + "/unified-test-deployment/deployment_manifest.xml");
    private static String deploymentZipLoc = "src/test/resources/idsTest.zip"; //scbi_unified_test_deployment.zip";
    private static File deploymentZip;

    private static Server server;

    private static String TEST_EDAN_ID = "p2b-1515252134647-1516215519247-0"; //QUOTIENTPROD
    private static String TEST_PROJECT_ID = "testProjectId";
    private static String TEST_DEPLOYMENT_ID = "testDeploymentId";
    private static String TEST_IAMGE_ID = "testRaccoonAndFox";
    private static String TEST_TITLE = "Camera Trap Image Northern Raccoon, Red Fox";
    private static String TEST_TYPE = "emammal_image";

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/edan-ids-sidora-route.xml";
    }

    /**
     * Starts up an edan endpoint to test against
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startServer() throws Exception {

        System.getProperties().list(System.out);

//        EDAN_TEST_URI = "http://localhost:"+ System.getProperty("dynamic.test.port");
//
//        System.setProperty("si.ct.uscbi.server", EDAN_TEST_URI);

        System.out.println("===========[ EDAN_TEST_URI = " + EDAN_TEST_URI + " ]============");

        // start a simple front service
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        //factory.setAddress(EDAN_TEST_URI);
        factory.setResourceClasses(EdanTestService.class);

        server = factory.create();
        server.start();
        System.out.println("===========[ JAXRSServerFactory Address = " + "" + " ]============");
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deploymentZip = new File(deploymentZipLoc);
        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void ctIngestMessageTest() throws Exception {
        String testDatastreamsXML = readFileToString(new File(KARAF_HOME + "/test-data/fedoraObjectDatastreams.xml"));

        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(6);
        resultEndpoint.expectedHeaderReceived("imageid", "testDeploymentIds1i1");
        resultEndpoint.expectedHeaderValuesReceivedInAnyOrder("pid", "test:004", "test:005", "test:006", "test:007", "test:008", "test:009");
        resultEndpoint.setAssertPeriod(1500);

        context.getRouteDefinition("EdanIdsProcessCtDeployment").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("ctProcessGetFedoraDatastream").replace().setBody().simple(testDatastreamsXML);
                weaveById("ctProcessEdanUpdate").replace().to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("addEdanIds", "true");
        exchange.getIn().setHeader("ProjectId", "testProjectId");
        exchange.getIn().setHeader("SiteId", "testDeploymentId");
        exchange.getIn().setHeader("SitePID", "test:003");
        exchange.getIn().setHeader("PIDAggregation", "test:004,test:005,test:006,test:007,test:008,test:009,test:010,test:011,test:012");
        exchange.getIn().setHeader("ResearcherObservationPID", "test:010");
        exchange.getIn().setHeader("VolunteerObservationPID", "test:011");
        exchange.getIn().setHeader("ImageObservationPID", "test:012");
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        //exchange.getIn().setBody(expectedBody, String.class);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();

    }

    @Test
    public void fedoraMessageTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);
        String testDatastreamsXML = readFileToString(new File(KARAF_HOME + "/test-data/fedoraObjectDatastreams.xml"));
        String testFedoraMessageBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("imageid", "testDeploymentIds1i1");
        mockResult.expectedHeaderReceived("parentPid", "test:001");
        mockResult.expectedBodiesReceived(testManifestXML);
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("EdanIdsProcessFedoraMessage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(testDatastreamsXML);
                weaveById("processFedoraFindParentObject").replace().setHeader("parentPid", simple("test:001"));
                weaveById("processFedoraGetManifestDatastream").replace().setBody().simple(testManifestXML);
                weaveById("processFedoraEdanUpdate").replace().log(LoggingLevel.INFO, "Skipping Edan Update!!!");
                weaveById("processFedoraIdsAddAsset").replace().log(LoggingLevel.INFO, "Skipping Ids Add Asset!!!");
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", getExtra().getProperty("si.ct.namespace") + ":test");
        exchange.getIn().setBody(testFedoraMessageBody);

        template.send("activemq:queue:" + JMS_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void filterSpeciesNameTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByType(FilterDefinition.class).before().log("Checking imageid: ${header.imageid}...");
                weaveByType(FilterDefinition.class).after().log(LoggingLevel.INFO, "==========================[ TO MOCK RESULT ]=============================").to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);

        // Double-identification set with raccoon and human:
        exchange.getIn().setHeader("imageid", "testImageRaccoonAndMan");
        template.send("direct:edanUpdate", exchange);

        // Set with human:
        exchange.getIn().setHeader("imageid", "testImageMan");
        exchange.setProperty(Exchange.ROUTE_STOP, false);
        template.send("direct:edanUpdate", exchange);

        // Set with human and vehicle:
        exchange.getIn().setHeader("imageid", "testImageRaccoonAndVehicleAndMan");
        exchange.setProperty(Exchange.ROUTE_STOP, false);
        template.send("direct:edanUpdate", exchange);

        // Double-identification set with Raccoon and Fox:
        exchange.getIn().setHeader("imageid", "RaccoonAndFox");
        exchange.setProperty(Exchange.ROUTE_STOP, false);
        template.send("direct:edanUpdate", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing the updateEdan route when there is an existing edan record and an EDAN editRecord request is needed.
     * @throws Exception
     */
    @Test
    public void updateEdanEditContentTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("edanId", "p2b-1515252134647-1516215519247-0");
        mockResult.expectedHeaderReceived("edanTitle", "Camera Trap Image Northern Raccoon, Red Fox");
        mockResult.expectedHeaderReceived("edanType", "emammal_image");
        mockResult.expectedHeaderReceived("SiteId", "testDeploymentId");
        mockResult.expectedHeaderReceived("edanJson", readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.txt")));
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("updateEdanSearchRequest").replace().log(LoggingLevel.INFO, "Skipping Edan ImageId Search!!!")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/test-json-data/edanImageIdSearch_ImageId_Exists.json";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, String.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
                weaveById("edanUpdateEditContent").replace().log(LoggingLevel.INFO, "Skipping Edan Edit Content!!!");
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");

        template.send("direct:edanUpdate", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing the updateEdan route when there is no existing edan record and an EDAN createRecord request is needed.
     * @throws Exception
     */
    @Test
    public void updateEdanCreateContentTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("edanId", "p2b-1515252134647-1516215519247-0");
        mockResult.expectedHeaderReceived("edanJson", readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.txt")));
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("updateEdanSearchRequest").replace().log(LoggingLevel.INFO, "Skipping Edan ImageId Search!!!")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/test-json-data/edanImageIdSearch_ImageId_NotExist.json";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, String.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
                weaveById("edanUpdateCreateContent").replace().log(LoggingLevel.INFO, "Skipping Edan Create Content!!!")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/test-json-data/edanCreateContent_Response.json";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, String.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");

        template.send("direct:edanUpdate", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing the createEdanJsonContent route when there is an existing edan record and edanId. Situation occurs when an existing EDAN record is found for the imageid during the edan search in the updateEdan route and an EDAN editRecord request is made.
     * @throws Exception
     */
    @Test
    public void createEdanJsonContentWithEdanIdTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedHeaderReceived("edanJson", readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.txt")));
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("createEdanJsonContent").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setHeader("edanId", "p2b-1515252134647-1516215519247-0");
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");
        exchange.getIn().setHeader("testImage", "testRaccoonAndFox.JPG");

        template.send("direct:createEdanJsonContent", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing the createEdanJsonContent route when there is no existing edan record and no edanId. Situation occurs when an existing EDAN record is not found for the imageid during the edan search in the updateEdan route and an EDAN createRecord request is made.
     * @throws Exception
     */
    @Test
    public void createEdanJsonContentNoEdanIdTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMinimumMessageCount(1);
        mockResult.expectedHeaderReceived("edanJson", readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.txt")));
        mockResult.setAssertPeriod(1500);

        context.getRouteDefinition("createEdanJsonContent").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");
        exchange.getIn().setHeader("testImage", "testRaccoonAndFox.JPG");

        template.send("direct:createEdanJsonContent", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void idsVelocityTest() throws Exception {
        String expectedBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<Assets>\n" +
                "  <Asset Name=\"" + getExtra().getProperty("si.edu.idsAssetImagePrefix") + TEST_IAMGE_ID + ".JPG\" IsPublic=\"Yes\" IsInternal=\"No\" MaxSize=\"3000\" InternalMaxSize=\"4000\">" + getExtra().getProperty("si.edu.idsAssetImagePrefix") + TEST_IAMGE_ID + "</Asset>\n" +
                "</Assets>\n";

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(expectedBody);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:idsVelocityTest").routeId("idsVelocityTest")
                        .setHeader("idsAssetImagePrefix", simple("{{si.edu.idsAssetImagePrefix}}"))
                        .toD("velocity:file:{{karaf.home}}/Input/templates/ids_template.vsl")
                        .log(LoggingLevel.INFO, "${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("imageid", TEST_IAMGE_ID);

        template.send("direct:idsVelocityTest", exchange);

        assertMockEndpointsSatisfied();
    }



    @Test
    public void idsAddAssetTest() throws Exception {
        String idsAssetFilePrefix = getExtra().getProperty("si.edu.idsAssetFilePrefix");
        String idsAssetImagePrefix = getExtra().getProperty("si.edu.idsAssetImagePrefix");
        String testDeployment1 = "testDeploymentId";
        String testDeployment2 = "d00001";

        String expectedAssetDir_1 = getExtra().getProperty("si.ct.uscbi.idsPushLocation") + idsAssetFilePrefix + testDeployment1;
        String expectedAssetDir_2 = getExtra().getProperty("si.ct.uscbi.idsPushLocation") + idsAssetFilePrefix + testDeployment2;
        String expectedAssetXmlFile_1 =  expectedAssetDir_1 + "/" + idsAssetFilePrefix + testDeployment1 + ".xml";
        String expectedAssetXmlFile_2 =  expectedAssetDir_2 + "/" + idsAssetFilePrefix + testDeployment2 + ".xml";

        deleteDirectory(expectedAssetDir_1);
        deleteDirectory(expectedAssetDir_2);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(4);

        context.getRouteDefinition("idsAddAsset").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("idsAddAssetGetFedoraOBJDatastreamContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/unified-test-deployment/" + out.getHeader("testImage");
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, String.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
                //.to("log:test.edanIds?showAll=true&multiline=true&maxChars=100000");
                weaveAddLast().to("mock:result");
            }
        });

        //Test assets are added to the asset xml and ids dir

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("SiteId", testDeployment1);

        // Will create a new asset Xml file and directory
        exchange.getIn().setHeader("imageid", "testDeploymentIds1i1");
        exchange.getIn().setHeader("testImage", "testDeploymentIds1i1.JPG");
        template.send("direct:idsAddAsset", exchange);

        Thread.sleep(2500);

        // will update the existing asset xml adding the new idsId
        exchange.getIn().setHeader("imageid", "testDeploymentIds1i2");
        exchange.getIn().setHeader("testImage", "testDeploymentIds1i2.JPG");
        template.send("direct:idsAddAsset", exchange);

        Thread.sleep(2500);

        // will update the existing asset xml adding the new idsId
        exchange.getIn().setHeader("imageid", "testDeploymentIds1i3");
        exchange.getIn().setHeader("testImage", "testDeploymentIds1i3.JPG");
        template.send("direct:idsAddAsset", exchange);

        Thread.sleep(2500);

        exchange.getIn().setHeader("SiteId", testDeployment2);
        exchange.getIn().setHeader("imageid", "testImageMan");
        exchange.getIn().setHeader("testImage", "testImageMan.JPG");
        template.send("direct:idsAddAsset", exchange);

        Thread.sleep(2500);

        assertMockEndpointsSatisfied();

        //Asserts for the first test deployment
        assertFileExists(expectedAssetXmlFile_1);
        assertFileExists(expectedAssetDir_1 + "/" + idsAssetImagePrefix + "testDeploymentIds1i1.JPG");
        assertFileExists(expectedAssetDir_1 + "/" + idsAssetImagePrefix + "testDeploymentIds1i2.JPG");
        String assetXml_1 = readFileToString(new File(expectedAssetXmlFile_1));
        XMLAssert.assertXpathEvaluatesTo("3", "count(/Assets/Asset)", assetXml_1);
        //XML Assert asset 1
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testDeploymentIds1i1", "/Assets/Asset[1]/text()", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testDeploymentIds1i1.JPG", "string(/Assets/Asset[1]/@Name)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("Yes", "string(/Assets/Asset[1]/@IsPublic)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("No", "string(/Assets/Asset[1]/@IsInternal)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("3000", "string(/Assets/Asset[1]/@MaxSize)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("4000", "string(/Assets/Asset[1]/@InternalMaxSize)", assetXml_1);
        //XML Assert asset 2
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testDeploymentIds1i2", "/Assets/Asset[2]/text()", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testDeploymentIds1i2.JPG", "string(/Assets/Asset[2]/@Name)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("Yes", "string(/Assets/Asset[2]/@IsPublic)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("No", "string(/Assets/Asset[2]/@IsInternal)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("3000", "string(/Assets/Asset[2]/@MaxSize)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("4000", "string(/Assets/Asset[2]/@InternalMaxSize)", assetXml_1);
        //XML Assert asset 3
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testDeploymentIds1i3", "/Assets/Asset[3]/text()", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testDeploymentIds1i3.JPG", "string(/Assets/Asset[3]/@Name)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("Yes", "string(/Assets/Asset[3]/@IsPublic)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("No", "string(/Assets/Asset[3]/@IsInternal)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("3000", "string(/Assets/Asset[3]/@MaxSize)", assetXml_1);
        XMLAssert.assertXpathEvaluatesTo("4000", "string(/Assets/Asset[3]/@InternalMaxSize)", assetXml_1);


        //Asserts for the second test deployment
        assertFileExists(expectedAssetXmlFile_2);
        assertFileExists(expectedAssetDir_2 + "/" + idsAssetImagePrefix + "testImageMan.JPG");
        String assetXml_2 = readFileToString(new File(expectedAssetXmlFile_2));
        XMLAssert.assertXpathEvaluatesTo("1", "count(/Assets/Asset)", assetXml_2);
        //XML Assert asset 1
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testImageMan", "/Assets/Asset[1]/text()", assetXml_2);
        XMLAssert.assertXpathEvaluatesTo(idsAssetImagePrefix +"testImageMan.JPG", "string(/Assets/Asset[1]/@Name)", assetXml_2);
        XMLAssert.assertXpathEvaluatesTo("Yes", "string(/Assets/Asset[1]/@IsPublic)", assetXml_2);
        XMLAssert.assertXpathEvaluatesTo("No", "string(/Assets/Asset[1]/@IsInternal)", assetXml_2);
        XMLAssert.assertXpathEvaluatesTo("3000", "string(/Assets/Asset[1]/@MaxSize)", assetXml_2);
        XMLAssert.assertXpathEvaluatesTo("4000", "string(/Assets/Asset[1]/@InternalMaxSize)", assetXml_2);
    }

    @Test
    public void edanIdsExceptionTest () throws Exception {
        Integer minEdanRedelivery = Integer.valueOf(getExtra().getProperty("min.edan.redeliveries"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);
        mockResult.expectedHeaderReceived("redeliveryCount", minEdanRedelivery);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(EdanIdsException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minEdanRedelivery);

        context.getRouteDefinition("edanHttpRequest").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        try {
                            if (in.getHeader("redeliveryCount", Integer.class) == minEdanRedelivery) {
                                throw new IOException("Outer try exception");
                            }
                            try {
                                throw new ConnectException("Inner Try Exception");
                            } catch (Exception e) {
                                throw new EdanIdsException("Simulated EdanApiBean Error!!!", e);
                            }
                        } catch (Exception e) {
                            throw new EdanIdsException(e);
                        }
                    }
                };

                //advice sending to database and replace with processor
                weaveById("edanApiSendRequest").replace().process(processor);
                weaveById("logEdanIdsException").after().to("mock:error");
                weaveAddLast().to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");

        template.send("direct:edanUpdate", exchange);

        assertMockEndpointsSatisfied();

    }
}
