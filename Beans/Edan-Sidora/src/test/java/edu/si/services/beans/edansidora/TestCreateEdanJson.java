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
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * @author jbirkhimer
 */
public class TestCreateEdanJson extends EDAN_CT_BlueprintTestSupport {

    private static final Logger log = LoggerFactory.getLogger(TestCreateEdanJson.class);
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static File testManifest = new File(KARAF_HOME + "/unified-test-deployment/deployment_manifest.xml");
    private static String extraJson = "\"source\": \"dev\",";

    protected static final boolean LOCAL_TEST = false;
    //private static String TEST_EDAN_ID = "p2b-1515252134647-1515436502565-0"; //QUOTIENTPROD
    //private static String TEST_IMAGE_PREFIX = config.getString("si.edu.idsAssetImagePrefix");
    private static String TEST_EDAN_ID = "p2b-1515252134647-1516215519247-0"; //QUOTIENTPROD
    private static String TEST_PROJECT_ID = "testProjectId";
    private static String TEST_DEPLOYMENT_ID = "testDeploymentId";
    private static String TEST_IAMGE_ID = "testRaccoonAndFox";
    private static String TEST_TITLE = "Camera Trap Image Northern Raccoon, Red Fox";
    private static String TEST_SIDORA_PID = "test:0001";
    private static String TEST_TYPE = "emammal_image";
    private static String TEST_APP_ID = "QUOTIENTPROD";

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/edan-ids-sidora-route.xml";
    }

    public String newJsonProcessor(String body, String extraJson) {
        String result = null;
        // convert xslt xml output to json and convert array elements to json array
        try {
            //JSONObject xmlJSONObj = XML.toJSONObject(body, true);
            JSONObject xmlJSONObj = XML.toJSONObject(body);
            log.debug("xml 2 json Output:\n{}", xmlJSONObj);

            JSONObject edan_content = xmlJSONObj.getJSONObject("xml").getJSONObject("content");
            log.debug("json edan_content Output:\n{}", edan_content.toString(4));

            JSONObject image = edan_content.getJSONObject("image");
            JSONObject online_media = image.getJSONObject("online_media");
            Object online_media_array_element = online_media.get("online_media_array_element");
            if (online_media_array_element instanceof JSONObject) {
                JSONArray online_media_replace = new JSONArray();
                online_media_replace.put(online_media_array_element);
                edan_content.getJSONObject("image").put("online_media", online_media_replace);
            } else if (online_media_array_element instanceof JSONArray) {
                edan_content.getJSONObject("image").put("online_media", online_media_array_element);
            }

            log.debug("json edan_content after online_media fix:\n{}", edan_content.getJSONObject("image").getJSONArray("online_media").toString(4));

            JSONObject image_identifications = edan_content.getJSONObject("image_identifications");
            Object image_identifications_array_element = image_identifications.get("image_identifications_array_element");
            if (image_identifications_array_element instanceof JSONObject) {
                JSONArray image_identifications_replace = new JSONArray();
                image_identifications_replace.put(image_identifications_array_element);
                edan_content.getJSONObject("image").put("image_identifications", image_identifications_replace);
            } else if (image_identifications_array_element instanceof JSONArray) {
                edan_content.put("image_identifications", image_identifications_array_element);
            }

            log.debug("json edan_content after image_identifications fix:\n{}", edan_content.getJSONArray("image_identifications").toString(4));

            if (extraJson != null || !extraJson.isEmpty()) {
                JSONObject extra = new JSONObject("{" + extraJson + "}");
                Map<String, Object> extraMap = new HashMap<>();
                extraMap.putAll(extra.toMap());

                for (Map.Entry entry : extraMap.entrySet()) {
                    edan_content.accumulate(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            log.info("json final edan_content :\n{}", xmlJSONObj.getJSONObject("xml").toString(4));
            result = xmlJSONObj.getJSONObject("xml").toString(4);
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return result;
    }

    @Test
    public void testTransform2XML_create() throws Exception {
        String deployment = readFileToString(testManifest); //has multiple researcher identifications !!!
        String expected = URLDecoder.decode(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.json")), "UTF-8");

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(KARAF_HOME + "/Input/xslt/edan_Transform_2_xml.xsl"));
        Transformer transformer = factory.newTransformer(xslt);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setParameter("imageid", TEST_IAMGE_ID);
        transformer.setParameter("CamelFedoraPid", TEST_SIDORA_PID);
        transformer.setParameter("extraJson", extraJson);

        Source xmlInSource = new StreamSource(new StringReader(deployment));
        StringWriter xmlOutWriter = new StringWriter();
        transformer.transform(xmlInSource, new StreamResult(xmlOutWriter));
        log.info("edan_transform_2_xml xslt Output:\n{}", xmlOutWriter.toString());

        String actual = newJsonProcessor(xmlOutWriter.toString(), extraJson);

        log.info("expected json:\n{}", expected);

        JSONAssert.assertEquals(expected, actual, false);

    }

    @Test
    public void testTransform2XML_update() throws Exception {
        String deployment = readFileToString(testManifest); //has multiple researcher identifications !!!
        String expected = URLDecoder.decode(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.json")), "UTF-8");

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(KARAF_HOME + "/Input/xslt/edan_Transform_2_xml.xsl"));
        Transformer transformer = factory.newTransformer(xslt);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setParameter("imageid", TEST_IAMGE_ID);
        transformer.setParameter("CamelFedoraPid", TEST_SIDORA_PID);
        transformer.setParameter("extraJson", extraJson);
        transformer.setParameter("edanId", TEST_EDAN_ID);

        Source xmlInSource = new StreamSource(new StringReader(deployment));
        StringWriter xmlOutWriter = new StringWriter();
        transformer.transform(xmlInSource, new StreamResult(xmlOutWriter));
        log.info("edan_transform_2_xml xslt Output:\n{}", xmlOutWriter.toString());

        String actual = newJsonProcessor(xmlOutWriter.toString(), extraJson);

        log.info("expected json:\n{}", expected);

        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void testTransform2JSON_create() throws Exception {
        String deployment = readFileToString(testManifest); //has multiple researcher identifications !!!
        JSONObject expected = new JSONObject(URLDecoder.decode(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_NoEdanId.json")), "UTF-8"));

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(KARAF_HOME + "/Input/xslt/edan_Transform.xsl"));
        Transformer transformer = factory.newTransformer(xslt);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setParameter("imageid", TEST_IAMGE_ID);
        transformer.setParameter("CamelFedoraPid", TEST_SIDORA_PID);
        transformer.setParameter("extraJson", extraJson);
        Source xmlInSource = new StreamSource(new StringReader(deployment));
        StringWriter xmlOutWriter = new StringWriter();
        transformer.transform(xmlInSource, new StreamResult(xmlOutWriter));
        log.info("edan_transform_2_json xslt Output:\n{}", xmlOutWriter.toString());

        JSONObject resultJson = XML.toJSONObject(xmlOutWriter.toString(), true);

        log.info("expected json:\n{}", expected);

        JSONAssert.assertNotEquals(expected, resultJson, JSONCompareMode.LENIENT);
        JSONAssert.assertNotEquals(expected, resultJson, JSONCompareMode.STRICT);
    }

    @Test
    public void testTransform2JSON_update() throws Exception {
        String deployment = readFileToString(testManifest); //has multiple researcher identifications !!!
        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.json")));

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(KARAF_HOME + "/Input/xslt/edan_Transform.xsl"));
        Transformer transformer = factory.newTransformer(xslt);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setParameter("imageid", TEST_IAMGE_ID);
        transformer.setParameter("CamelFedoraPid", TEST_SIDORA_PID);
        transformer.setParameter("edanId", TEST_EDAN_ID);
        transformer.setParameter("extraJson", extraJson);
        Source xmlInSource = new StreamSource(new StringReader(deployment));
        StringWriter xmlOutWriter = new StringWriter();
        transformer.transform(xmlInSource, new StreamResult(xmlOutWriter));
        log.info("edan_transform_2_json xslt Output:\n{}", xmlOutWriter.toString());

        JSONObject resultJson = XML.toJSONObject(xmlOutWriter.toString(), true);

        log.info("expected json:\n{}", expected);

        JSONAssert.assertNotEquals(expected, resultJson, JSONCompareMode.LENIENT);
        JSONAssert.assertNotEquals(expected, resultJson, JSONCompareMode.STRICT);
    }

    @Test
    public void testCreateEdanJsonContentRoute() throws Exception {
        String testManifestXML = readFileToString(testManifest);
        JSONObject expected = new JSONObject(readFileToString(new File(KARAF_HOME + "/test-json-data/testEdanJsonContentEncoded_withEdanId.json")));

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
        exchange.getIn().setHeader("pid", TEST_SIDORA_PID);
        exchange.getIn().setHeader("edanId", TEST_EDAN_ID);
        exchange.getIn().setHeader("imageid", TEST_IAMGE_ID);
        exchange.getIn().setHeader("testImage", TEST_IAMGE_ID +".JPG");

        template.send("seda:createEdanJsonContent", exchange);

        assertMockEndpointsSatisfied();

        String resultJson = URLDecoder.decode(mockResult.getReceivedExchanges().get(0).getIn().getHeader("edanJson", String.class), "UTF-8");
        JSONObject result = new JSONObject(resultJson);

        JSONAssert.assertEquals(expected, result, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expected, result, JSONCompareMode.STRICT);
    }

    @Test
    @Ignore
    public void edanAdminContentCreateContentTest() throws Exception {
        assumeFalse(LOCAL_TEST);
        assumeTrue(context.resolvePropertyPlaceholders("{{si.ct.uscbi.appId}}").equals(TEST_APP_ID));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(2);

        context.getRouteDefinition("createEdanJsonContent").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("encodeJson").after().to("mock:result");
            }
        });

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:edanTest").routeId("edanTest")
                        //.to("bean:edanApiBean?method=sendRequest(*, ${header.edanServiceEndpoint}, ${header.edanQueryParams})")
                        .to("bean:edanApiBean?method=sendRequest")
                        .to("log:test?showAll=true&multiline=true&maxChars=100000")
                        .convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "${id} EdanIds: EDAN Request Status: ${header.CamelHttpResponseCode}, Response: ${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("pid", TEST_SIDORA_PID);
        exchange.getIn().setHeader("imageid", TEST_IAMGE_ID);

        template.requestBodyAndHeaders("seda:createEdanJsonContent", null, exchange.getIn().getHeaders());

        //Now make createContent EDAN request
        String edanJson = mockResult.getExchanges().get(0).getIn().getHeader("edanJson", String.class);

        exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/admincontent/createContent.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "content="+ edanJson); //QUOTIENTPROD

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        com.amazonaws.util.json.JSONObject json = new com.amazonaws.util.json.JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getString("id");

        log.info("Expected ID = {}, Returned ID = {}", TEST_EDAN_ID, result);
        assertEquals("The returned ID does not match", TEST_EDAN_ID, result);
    }

    @Test
    @Ignore
    public void edanAdminContentEditContentTest() throws Exception {
        assumeFalse(LOCAL_TEST);
        assumeTrue(context.resolvePropertyPlaceholders("{{si.ct.uscbi.appId}}").equals(TEST_APP_ID));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(2);

        context.getRouteDefinition("createEdanJsonContent").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("encodeJson").after().to("mock:result");
            }
        });

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:edanTest").routeId("edanTest")
                        //.to("bean:edanApiBean?method=sendRequest(*, ${header.edanServiceEndpoint}, ${header.edanQueryParams})")
                        .to("bean:edanApiBean?method=sendRequest")
                        .to("log:test?showAll=true&multiline=true&maxChars=100000")
                        .convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "${id} EdanIds: EDAN Request Status: ${header.CamelHttpResponseCode}, Response: ${body}")
                        .to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("pid", TEST_SIDORA_PID);
        exchange.getIn().setHeader("imageid", TEST_IAMGE_ID);
        exchange.getIn().setHeader("edanId", TEST_EDAN_ID);

        template.requestBodyAndHeaders("seda:createEdanJsonContent", null, exchange.getIn().getHeaders());

        //Now make editContent EDAN request
        String edanJson = mockResult.getExchanges().get(0).getIn().getHeader("edanJson", String.class);

        exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/admincontent/editContent.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "id=" + TEST_EDAN_ID + "&content="+ edanJson); //QUOTIENTPROD

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        com.amazonaws.util.json.JSONObject json = new com.amazonaws.util.json.JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getString("id");

        log.info("Expected ID = {}, Returned ID = {}", TEST_EDAN_ID, result);
        assertEquals("The returned ID does not match", TEST_EDAN_ID, result);
    }
}
