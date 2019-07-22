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

import com.amazonaws.services.dynamodbv2.xspec.S;
import edu.si.services.beans.edansidora.aggregation.EdanBulkAggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.FilterDefinition;
import org.apache.camel.model.SplitDefinition;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.custommonkey.xmlunit.XMLAssert;
import org.json.JSONObject;
import org.junit.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.si.services.beans.edansidora.model.IdsAsset;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class UCT_EdanSidoraTest extends EDAN_CT_BlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static String JMS_FEDORA_TEST_QUEUE;
    private static String JMS_CT_INGEST_TEST_QUEUE;

    private static File testManifest = new File(KARAF_HOME + "/unified-test-deployment/deployment_manifest.xml");
    private static String deploymentZipLoc = "src/test/resources/idsTest.zip"; //scbi_unified_test_deployment.zip";
    private static File deploymentZip;

    private static String TEST_EDAN_ID = "p2b-1515252134647-1516215519247-0"; //QUOTIENTPROD
    private static String TEST_PROJECT_ID = "testProjectId";
    private static String TEST_DEPLOYMENT_ID = "testDeploymentId";
    private static String TEST_IAMGE_ID = "testRaccoonAndFox";
    private static String TEST_TITLE = "Camera Trap Image Northern Raccoon, Red Fox";
    private static String TEST_TYPE = "emammal_image";

    private static String CT_PID_NS;
    private static String SI_FEDORA_USER;

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/edan-ids-sidora-route.xml";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deploymentZip = new File(deploymentZipLoc);
        log.debug("Exchange_FILE_NAME = {}", deploymentZip.getName());
        CT_PID_NS = context.resolvePropertyPlaceholders("{{si.ct.namespace}}") + ":";
        SI_FEDORA_USER = context.resolvePropertyPlaceholders("{{si.fedora.user}}");
        JMS_FEDORA_TEST_QUEUE = context.resolvePropertyPlaceholders("{{edanIds.queue}}");
        JMS_CT_INGEST_TEST_QUEUE = context.resolvePropertyPlaceholders("{{edanIds.ct.queue}}");
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void ctIngestMessageTest() throws Exception {

        deleteDirectory(getExtra().getProperty("si.ct.uscbi.idsPushLocation"));

        HashMap<String, Object> headers = new HashMap<>();
        headers.put("testPID", CT_PID_NS +"test");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testObjMimeType", "image/jpg");

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 100; i++) {
            sb.append("test:"+i+",");
        }
        sb.append("test:100");

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(100);
        mockResult.setResultWaitTime(2000*100);
        mockResult.expectedHeaderValuesReceivedInAnyOrder("pid", sb.toString().split(","));

        MockEndpoint mockEnd = getMockEndpoint("mock:end");
        mockEnd.expectedMessageCount(1);

        context.getRouteDefinition("EdanIdsProcessCtDeployment").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                weaveById("ctProcessGetFedoraDatastream").replace()//.setBody().simple(testDatastreamsXML);
                        .setHeader("testPID").simple("${header.pid}")
                        .setHeader("testObjMimeType").simple("image/jp")
                        .setHeader("testDsLabel").simple("testImageId_${header.pid}")
                        .toD("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl");

                weaveById("ctProcessEdanUpdate").replace().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        long leftLimit = 1L;
                        long rightLimit = 2000L;
                        long generatedLong = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
                        log.debug("delay for {}", generatedLong);
                        Thread.sleep(generatedLong);
                    }
                }).to("mock:result");
            }
        });

        context.getRouteDefinition("idsAssetImageUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("idsAssetUpdateGetFedoraOBJDatastreamContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/unified-test-deployment/testDeploymentIds1i1.JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, File.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
            }
        });

        context.getRouteDefinition("idsAssetXMLWriter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:end");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("addEdanIds", "true");
        exchange.getIn().setHeader("ProjectId", "testProjectId");
        exchange.getIn().setHeader("SiteId", "testDeploymentId");
        exchange.getIn().setHeader("SitePID", "test:003");
        exchange.getIn().setHeader("PIDAggregation", sb.toString());
        exchange.getIn().setHeader("ResearcherObservationPID", "test:010");
        exchange.getIn().setHeader("VolunteerObservationPID", "test:011");
        exchange.getIn().setHeader("ImageObservationPID", "test:012");
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        //exchange.getIn().setBody(expectedBody, String.class);

        template.send("activemq:queue:" + JMS_CT_INGEST_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();

    }

    @Test
    public void fedoraMessageFilterTest() throws Exception {
        //String testFedoraMessageBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        log.debug(jmsMsg);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        //resultEndpoint.expectedBodiesReceived(jmsMsg);
        mockResult.expectedHeaderReceived("origin", "otherUser");
        mockResult.expectedHeaderReceived("methodName", "modifyDatastreamByValue");
        mockResult.expectedHeaderReceived("pid", CT_PID_NS+"1");
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(0);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", CT_PID_NS+"1");
        exchange.getIn().setBody(jmsMsg);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void fedoraMessageTest() throws Exception {
        //String testFedoraMessageBody = readFileToString(new File(KARAF_HOME + "/JMS-test-data/otherUser-modifyDatastreamByValue.atom"));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "modifyDatastreamByValue");
        headers.put("testPID", CT_PID_NS+"1");
        headers.put("testDsLabel", "testDeploymentIds1i1");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        log.debug(jmsMsg);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        //resultEndpoint.expectedBodiesReceived(jmsMsg);
        mockResult.expectedHeaderReceived("origin", "otherUser");
        mockResult.expectedHeaderReceived("methodName", "modifyDatastreamByValue");
        mockResult.expectedHeaderReceived("pid", CT_PID_NS+"1");
        mockResult.expectedPropertyReceived(Exchange.FILTER_MATCHED, false);

        MockEndpoint mockFilter = getMockEndpoint("mock:filter");
        mockFilter.expectedMessageCount(0);
        mockFilter.expectedPropertyReceived(Exchange.FILTER_MATCHED, true);

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraGetDatastreams").replace().setBody().simple(dsXML);
                weaveById("processFedoraGetRELS-EXT").replace().setBody().simple(rels_extXML);
                weaveById("logFilteredMessage").after().to("mock:filter");
                weaveById("startProcessingFedoraMessage").replace()
                        .log(LoggingLevel.DEBUG, "${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "modifyDatastreamByValue");
        exchange.getIn().setHeader("pid", CT_PID_NS+"1");
        exchange.getIn().setBody(jmsMsg);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void fedoraDeleteMessageTest() throws Exception {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("origin", "otherUser");
        headers.put("methodName", "purgeObject");
        headers.put("testPID", "test:0001");
        headers.put("testDsLabel", "test_label");
        headers.put("testDsId", "OBJ");
        headers.put("testObjMimeType", "image/jpg");
        headers.put("testFedoraModel", "info:fedora/si:generalImageCModel");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("date", new DateTool());
        velocityContext.put("headers", headers);

        headers.put(VelocityConstants.VELOCITY_CONTEXT, velocityContext);

        String jmsMsg = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_atom.vsl", "test body", headers, String.class);
        String dsXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_datastreams.vsl", "test body", headers, String.class);
        String rels_extXML = template.requestBodyAndHeaders("velocity:file:{{karaf.home}}/JMS-test-data/fedora_RELS-EXT.vsl", "test body", headers, String.class);

        log.debug(jmsMsg);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        MockEndpoint mockDelete = getMockEndpoint("mock:delete");
        mockDelete.expectedMessageCount(2);
        mockDelete.expectedHeaderValuesReceivedInAnyOrder("imageid", "testDeploymentIds1i1000", "testRaccoonAndFox");

        context.getRouteDefinition("EdanIdsStartProcessing").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraEdanDelete").after().to("mock:result").stop();
            }
        });

        context.getRouteDefinition("edanDelete").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("deleteEdanSearchRequest").replace().setBody().simple(readFileToString(new File(KARAF_HOME + "/test-json-data/edanDeletePidSearch_response.json")));
                weaveById("deleteEdanRecordRequest").replace().log(LoggingLevel.INFO, "Skipping Edan Delete HTTP Call!!!").to("mock:delete").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("methodName", "purgeObject");
        //exchange.getIn().setHeader("pid", getExtra().getProperty("si.ct.namespace") + ":test");
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setBody(jmsMsg);

        template.send("activemq:queue:" + JMS_FEDORA_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void filterSpeciesNameTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraFindParentObject").remove();
                weaveById("processFedoraGetManifestDatastream").replace().setBody().simple(testManifestXML);
                weaveByType(FilterDefinition.class).before().log("Checking imageid: ${header.imageid}...");
                weaveByType(FilterDefinition.class).after().log(LoggingLevel.INFO, "==========================[ TO MOCK RESULT ]=============================").to("mock:result").stop();
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);

        // Double-identification set with raccoon and human:
        exchange.getIn().setHeader("imageid", "testImageRaccoonAndMan");
        template.send("seda:edanUpdate", exchange);

        // Set with human:
        exchange.getIn().setHeader("imageid", "testImageMan");
        exchange.setProperty(Exchange.ROUTE_STOP, false);
        template.send("seda:edanUpdate", exchange);

        // Set with human and vehicle:
        exchange.getIn().setHeader("imageid", "testImageRaccoonAndVehicleAndMan");
        exchange.setProperty(Exchange.ROUTE_STOP, false);
        template.send("seda:edanUpdate", exchange);

        // Double-identification set with Raccoon and Fox:
        exchange.getIn().setHeader("imageid", "RaccoonAndFox");
        exchange.setProperty(Exchange.ROUTE_STOP, false);
        template.send("seda:edanUpdate", exchange);

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

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraFindParentObject").remove();
                weaveById("processFedoraGetManifestDatastream").replace().setBody().simple(testManifestXML);

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

        template.send("seda:edanUpdate", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.json")));
        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
    }

    /**
     * Testing the updateEdan route when there is an existing edan record and an EDAN editRecord request is needed.
     * @throws Exception
     */
    @Test
    public void updateEdanBatchEditContentTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("edanId", "p2b-1515252134647-1516215519247-0");
        mockResult.expectedHeaderReceived("edanTitle", "Camera Trap Image Northern Raccoon, Red Fox");
        mockResult.expectedHeaderReceived("edanType", "emammal_image");
        mockResult.expectedHeaderReceived("SiteId", "testDeploymentId");

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraFindParentObject").remove();
                weaveById("processFedoraGetManifestDatastream").replace().setBody().simple(testManifestXML);

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
                //weaveById("edanUpdateEditContent").replace().log(LoggingLevel.INFO, "Skipping Edan Edit Content!!!");
                weaveById("edanUpdateEditContent").replace().log(LoggingLevel.INFO, "Skipping Edan Edit Content!!!")
                        .aggregate(simple("${header.edanServiceEndpoint}"), new EdanBulkAggregationStrategy())
                        .completionTimeout(1000)
                        .parallelProcessing(Boolean.parseBoolean("true"))
                        .setHeader(Exchange.HTTP_QUERY).groovy("\"content=\" + URLEncoder.encode(request.headers.get('edanBulkRequests'));")
                        .to("bean:edanApiBean?method=sendRequest")
                        .convertBodyTo(String.class);
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");

        template.send("seda:edanUpdate", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.json")));
        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
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

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraFindParentObject").remove();
                weaveById("processFedoraGetManifestDatastream").replace().setBody().simple(testManifestXML);

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

        template.send("seda:edanUpdate", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.json")));
        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
    }

    /**
     * TODO: REWORK THIS test
     * Testing the updateEdan route when there is no existing edan record and an EDAN createRecord request is needed.
     * @throws Exception
     */
    @Test
    @Ignore
    public void updateEdanCreateContentBatchTest() throws Exception {
        String testManifestXML = readFileToString(testManifest);
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("edanId", "p2b-1515252134647-1516215519247-0");

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("processFedoraFindParentObject").remove();
                weaveById("processFedoraGetManifestDatastream").replace().setBody().simple(testManifestXML);

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
                weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", testManifestXML);
        exchange.getIn().setHeader("pid", "test:0001");
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");

        template.send("seda:edanUpdate", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.json")));
        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
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

        template.send("seda:createEdanJsonContent", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.json")));
        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
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

        template.send("seda:createEdanJsonContent", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.json")));
        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
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

        List<IdsAsset> idsAssetList = new ArrayList<IdsAsset>();

        IdsAsset asset = new IdsAsset();
        asset.setImageid(TEST_IAMGE_ID);
        asset.setSiteId(TEST_DEPLOYMENT_ID);
        asset.setIsPublic("Yes");
        asset.setIsInternal("No");
        asset.setPid("test:001");
        idsAssetList.add(asset);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("idsAssetList", idsAssetList);

        template.send("direct:idsVelocityTest", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void idsAssetUpdateTest() throws Exception {
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
        mockResult.expectedMessageCount(6);

        context.getRouteDefinition("idsAssetImageUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("idsAssetUpdateGetFedoraOBJDatastreamContent").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/unified-test-deployment/" + out.getBody(IdsAsset.class).getImageid() + ".JPG";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, File.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
            }
        });

       context.getRouteDefinition("idsAssetXMLWriter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("getAssetXml").replace()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String resourceFilePath = KARAF_HOME + "/test_assetXML.xml";
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, File.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        });
                weaveById("saveFile").after().to("mock:result");
                weaveAddLast().to("mock:result");
            }
        });

        //Test assets are added to the asset xml and ids dir

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("SiteId", testDeployment1);
        exchange.getIn().setHeader("isPublic", "Yes");
        exchange.getIn().setHeader("isInternal", "No");

        ArrayList<IdsAsset> cttestArray = new ArrayList<IdsAsset>();
        IdsAsset testAsset1 = new IdsAsset();
        IdsAsset testAsset2 = new IdsAsset();
        IdsAsset testAsset3 = new IdsAsset();
        IdsAsset testAsset4 = new IdsAsset();
        IdsAsset testAsset5 = new IdsAsset();

        testAsset1.setImageid("testDeploymentIds1i1");
        testAsset1.setSiteId(testDeployment1);
        testAsset1.setIsInternal("No");
        testAsset1.setIsPublic("Yes");
        testAsset1.setPid("test:0001");

        testAsset2.setImageid("testDeploymentIds1i2");
        testAsset2.setSiteId(testDeployment1);
        testAsset2.setIsInternal("No");
        testAsset2.setIsPublic("Yes");
        testAsset2.setPid("test:0002");

        testAsset3.setImageid("testDeploymentIds1i3");
        testAsset3.setSiteId(testDeployment1);
        testAsset3.setIsInternal("No");
        testAsset3.setIsPublic("Yes");
        testAsset3.setPid("test:0003");

        testAsset4.setImageid("testImageMan");
        testAsset4.setSiteId(testDeployment2);
        testAsset4.setIsInternal("No");
        testAsset4.setIsPublic("Yes");
        testAsset4.setPid("test:0004");

        testAsset5.setImageid("testImageRaccoonAndMan");
        testAsset5.setSiteId(testDeployment2);
        testAsset5.setIsInternal("No");
        testAsset5.setIsPublic("Yes");
        testAsset5.setPid("test:0005");

        cttestArray.add(testAsset1);
        cttestArray.add(testAsset2);
        cttestArray.add(testAsset3);
        cttestArray.add(testAsset4);
        cttestArray.add(testAsset5);

        exchange.getIn().setHeader("idsAssetList", cttestArray);
        template.send("seda:idsAssetImageUpdate", exchange);

        assertMockEndpointsSatisfied();
        /*
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
        XMLAssert.assertXpathEvaluatesTo("4000", "string(/Assets/Asset[1]/@InternalMaxSize)", assetXml_2);*/
    }

    @Test
    public void edanIdsExceptionTest () throws Exception {
        Integer minEdanRedelivery = Integer.valueOf(context.resolvePropertyPlaceholders("{{min.edan.redeliveries}}"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);
        mockResult.expectedHeaderReceived("redeliveryCount", minEdanRedelivery);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(EdanIdsException.class);
        mockError.expectedHeaderReceived("redeliveryCount", minEdanRedelivery);

        context.getRouteDefinition("edanUpdate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                context.getRouteDefinition("edanUpdate").onException(EdanIdsException.class)
                        .useExponentialBackOff()
                        .backOffMultiplier(2)
                        .redeliveryDelay("{{si.ct.edanIds.redeliveryDelay}}")
                        .maximumRedeliveries("{{min.edan.redeliveries}}")
                        .retryAttemptedLogLevel(LoggingLevel.WARN)
                        .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                        //.logExhausted(true)
                        .log(LoggingLevel.ERROR, "************[ TEST edanHttpRequest ONEXCEPTION ]************")
                        .to("mock:error");
            }
        });

        context.getRouteDefinition("edanHttpRequest").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to test onException and retries
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

                weaveById("edanApiSendSingleRequest").replace().process(processor);
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("imageid", "testRaccoonAndFox");

        template.send("seda:edanUpdate", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void httpRegexTest() {

        String regex = "^(http|https|cxfrs?(:http|:https)|http4):.*$";

        Exchange exchange1 = new DefaultExchange(context);
        exchange1.getIn().setHeader("test1", "http://localhost:8080/fedora/objects/test:001/datastreams?format=xml");
        exchange1.getIn().setHeader("test2", "https://localhost:8080/fedora/objects/test:001/datastreams?format=xml");
        exchange1.getIn().setHeader("test3", "cxfrs:http://localhost:8080/fedora/objects/test:001/datastreams?format=xml");
        exchange1.getIn().setHeader("test4", "cxfrs:https://localhost:8080/fedora/objects/test:001/datastreams?format=xml");
        exchange1.getIn().setHeader("test5", "http4://localhost:8080/fedora/objects/test:001/datastreams?format=xml");
        exchange1.getIn().setHeader("test5", "http4://localhost:8080/fedora/objects/test:001/datastreams?format=xml");
        exchange1.getIn().setHeader("testOther1", "log://test?showAll=true&multiline=true&maxChars=1000000");
        exchange1.getIn().setHeader("testOther2", "xslt://httptest");

        for (Map.Entry<String, Object> s : exchange1.getIn().getHeaders().entrySet()) {
            if (s.getKey().contains("Other")) {
                assertPredicate(header(s.getKey()).regex(regex), exchange1, false);
            } else {
                assertPredicate(header(s.getKey()).regex(regex), exchange1, true);
            }
        }
    }

    @Test
    public void edanIdsHTTPExceptionTest () throws Exception {
        Integer minEdanHTTPRedelivery = Integer.valueOf(getExtra().getProperty("min.edan.http.redeliveries"));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);
        mockResult.expectedHeaderReceived("redeliveryCount", minEdanHTTPRedelivery);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        if ((minEdanHTTPRedelivery % 2) == 0 ) {
            mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(HttpOperationFailedException.class);
        } else {
            mockError.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SocketException.class);
        }
        mockError.expectedHeaderReceived("redeliveryCount", minEdanHTTPRedelivery); //headers are not preserved from split

        context.getRouteDefinition("EdanIdsProcessCtDeployment").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                context.getRouteDefinition("EdanIdsProcessCtDeployment")
                        .onException(Exception.class, HttpOperationFailedException.class, SocketException.class)
                        .onWhen(exchangeProperty(Exchange.TO_ENDPOINT).regex("^(http|https|cxfrs?(:http|:https)|http4):.*"))
                        .useExponentialBackOff()
                        .backOffMultiplier(2)
                        .redeliveryDelay("{{si.ct.edanIds.redeliveryDelay}}")
                        .maximumRedeliveries("{{min.edan.http.redeliveries}}")
                        .retryAttemptedLogLevel(LoggingLevel.WARN)
                        .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                        .logNewException(true)
                        .log(LoggingLevel.ERROR, "************[ TEST HTTP ONEXCEPTION ]************")
                        .to("mock:error");

                /**
                 * I have not been able to come up with a way of testing other exceptions (exception bar) being thrown
                 * during retries for original exception (exception foo)
                 * See link for a good explanation and example of the issue
                 * https://stackoverflow.com/questions/13684775/camel-retry-control-with-multiple-exceptions
                 */

                //processor used to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        log.info("Redelivery Count = {}", in.getHeader(Exchange.REDELIVERY_COUNTER));
                        exchange.setProperty(Exchange.TO_ENDPOINT, in.getHeader("CamelHttpUri"));
                        log.info("exchangeProperty(Exchange.TO_ENDPOINT) = {}", exchange.getProperty(Exchange.TO_ENDPOINT));
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));

                        if (in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class) != null) {
                            if ((in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class) % 2) != 0 && in.getHeader("redeliveryCount", Integer.class) != 0) {
                                log.info("if block Redelivery Count = {}", in.getHeader(Exchange.REDELIVERY_COUNTER));
                                log.info("if exchangeProperty(Exchange.TO_ENDPOINT) = {}", exchange.getProperty(Exchange.TO_ENDPOINT));
                                throw new SocketException("Simulated SocketException!!!");
                            } else {
                                throw new HttpOperationFailedException("http://somehost", 404, null, null, null, null);
                            }
                        } else {
                            throw new HttpOperationFailedException("http://somehost", 404, null, null, null, null);
                        }
                    }
                };

                interceptSendToEndpoint("http4:*").skipSendToOriginalEndpoint().process(processor);
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

        template.send("activemq:queue:" + JMS_CT_INGEST_TEST_QUEUE, exchange);

        assertMockEndpointsSatisfied();
    }
}
