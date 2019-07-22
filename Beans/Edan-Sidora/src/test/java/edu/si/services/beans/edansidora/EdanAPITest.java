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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assume.assumeTrue;

/**
 * @author jbirkhimer
 */
public class EdanAPITest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(EdanAPITest.class);
    private static final boolean DEBUG = true;

    //EDAN and IDS Solr
    protected static String SOLR_SERVER;
    protected static int SOLR_PORT;
    protected static String EDAN_TEST_URI;
    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static Properties props = new Properties();
    private static File testManifest = new File(KARAF_HOME + "/unified-test-deployment/deployment_manifest.xml");

    private static EdanApiBean edanApiBean = new EdanApiBean();

    //private static String TEST_IMAGE_PREFIX = config.getString("si.edu.idsAssetImagePrefix");
    private static String TEST_EDAN_ID = "p1b-1562599618948-1563542629068-0"; //QUOTIENTPROD
    private static String TEST_PROJECT_ID = "testProjectId";
    private static String TEST_DEPLOYMENT_ID = "testDeploymentId";
    private static String TEST_IAMGE_ID = "testRaccoonAndFox";
    private static String TEST_TITLE = "Camera Trap Image Northern Raccoon, Red Fox";
    private static String TEST_SIDORA_PID = "test:001";
    private static String TEST_TYPE = "emammal_image";
    private static String TEST_APP_ID = "QUOTIENTPROD";


    /**
     * Start up a test EDAN Server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void loadConfig() throws Exception {

        List<String> propFileList = loadAdditionalPropertyFiles();
        if (loadAdditionalPropertyFiles() != null) {
            for (String propFile : propFileList) {
                Properties extra = new Properties();
                try {
                    extra.load(new FileInputStream(propFile));
                    props.putAll(extra);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        props.putAll(System.getProperties());

        edanApiBean.initIt();

        EDAN_TEST_URI = props.getProperty("si.ct.uscbi.server");
        LOG.info("===========[ EDAN_TEST_URI = {} ]============", EDAN_TEST_URI);
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        if (DEBUG) {
            System.getProperties().list(System.out);
            LOG.debug("===========[ Props ]============");
            props.list(System.out);
        }

        SOLR_SERVER = props.getProperty("edan.ids.solr.host");

        edanApiBean.setServer(props.getProperty("si.ct.uscbi.server"));
        edanApiBean.setApp_id(props.getProperty("si.ct.uscbi.appId"));
        edanApiBean.setEdan_key(props.getProperty("si.ct.uscbi.edanKey"));
        edanApiBean.setAuth_type(props.getProperty("si.ct.uscbi.authType"));
    }

    protected static List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/system.properties", KARAF_HOME + "/etc/edu.si.sidora.karaf.cfg", KARAF_HOME + "/etc/edu.si.sidora.emammal.cfg", KARAF_HOME + "/test.properties");
    }

    /**
     * Stop the test EDAN Server
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        edanApiBean.cleanUp();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {

        JndiRegistry jndiRegistry = super.createRegistry();
        //jndiRegistry.bind("edanApiBean", edu.si.services.beans.edansidora.EdanApiBean.class);
        jndiRegistry.bind("edanApiBean", edanApiBean);
        return jndiRegistry;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return props;
    }

    @Test
    public void edanGetContentIdParamTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/content/getContent.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "id=" + TEST_EDAN_ID);

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getString("id");

        LOG.info("Expected ID = {}, Returned ID = {}", TEST_EDAN_ID, result);
        assertEquals("The returned ID does not match", TEST_EDAN_ID, result);
    }

    @Test
    public void edanGetAdminContentTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));
        assumeTrue(props.getProperty("si.ct.uscbi.appId").equals(TEST_APP_ID));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/admincontent/getContent.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "id=" + TEST_EDAN_ID); //QUOTIENTPROD

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getString("id");

        LOG.info("Expected ID = {}, Returned ID = {}", TEST_EDAN_ID, result);
        assertEquals("The returned ID does not match", TEST_EDAN_ID, result);
    }

    @Test
    @Ignore
    public void edanAdminContentEditContentTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));
        assumeTrue(props.getProperty("si.ct.uscbi.appId").equals(TEST_APP_ID));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(2);

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("manifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("pid", "test:001");
        exchange.getIn().setHeader("imageid", TEST_IAMGE_ID);
        exchange.getIn().setHeader("edanId", TEST_EDAN_ID);

        template.send("direct:createEdanJsonContent", exchange);

        //Now make editContent EDAN request
        String edanJson = mockResult.getExchanges().get(0).getIn().getHeader("edanJson", String.class);

        exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/admincontent/editContent.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "id=" + TEST_EDAN_ID + "&content="+ edanJson); //QUOTIENTPROD

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getString("id");

        LOG.info("Expected ID = {}, Returned ID = {}", TEST_EDAN_ID, result);
        assertEquals("The returned ID does not match", TEST_EDAN_ID, result);
    }

    @Test
    @Ignore
    public void edanAdminContentCreateContentTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));
        assumeTrue(props.getProperty("si.ct.uscbi.appId").equals(TEST_APP_ID));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(2);

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("manifestXML", readFileToString(testManifest));
        exchange.getIn().setHeader("pid", "test:001");
        exchange.getIn().setHeader("imageid", TEST_IAMGE_ID);
        //exchange.getIn().setHeader("edanId", TEST_EDAN_ID);

        template.send("direct:createEdanJsonContent", exchange);

        //Now make createContent EDAN request
        String edanJson = mockResult.getExchanges().get(0).getIn().getHeader("edanJson", String.class);

        exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/admincontent/createContent.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "content="+ edanJson); //QUOTIENTPROD

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getString("id");

        LOG.info("Expected ID = {}, Returned ID = {}", TEST_EDAN_ID, result);
        assertEquals("The returned ID does not match", TEST_EDAN_ID, result);
    }

    @Test
    public void edanMetadataSearchTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "q=" + URLEncoder.encode("app_id:" + TEST_APP_ID + " AND type:" + TEST_TYPE, "UTF-8"));

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getJSONArray("rows").getJSONObject(0).getString("type");

        LOG.info("Expected Type = {}, Returned Type = {}", TEST_TYPE, result);
        assertEquals("The returned type does not match", TEST_TYPE, result);
    }

    @Test
    public void edanMetadataSearchFilterQueryTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"type:" + TEST_TYPE + "\"]", "UTF-8"));

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result = json.getJSONArray("rows").getJSONObject(0).getString("type");

        LOG.info("Expected Type = {}, Returned Type = {}", TEST_TYPE , result);
        assertEquals("The returned type does not match", TEST_TYPE, result);
    }

    @Test
    public void edanMetadataSearchFilterQueryCtProjectIdTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.project_id:"+ TEST_PROJECT_ID+ "\"]", "UTF-8"));

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result_projectId = json.getJSONArray("rows").getJSONObject(0).getJSONObject("content").getString("project_id");

        LOG.info("Expected ProjectId = {}, Returned ProjectId = {}", TEST_PROJECT_ID , result_projectId);
        assertEquals("The returned ProjectId does not match", TEST_PROJECT_ID, result_projectId);
    }

    @Test
    public void edanMetadataSearchFilterQueryCtDeploymentIdTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.deployment_id:" + TEST_DEPLOYMENT_ID + "\"]", "UTF-8") + "&rows=20&start=0");

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        //Get the record with the correct test EDAN Id we may receive more than one EDAN record.
        String result_deploymentId = null;
        for (int i = 0; i < json.getJSONArray("rows").length(); i++) {
            if (json.getJSONArray("rows").getJSONObject(i).getString("id").equalsIgnoreCase(TEST_EDAN_ID)) {
                result_deploymentId = json.getJSONArray("rows").getJSONObject(i).getJSONObject("content").getString("deployment_id");
            }
        }
        assumeTrue("The EDAN response does not contain our test EDAN Id:" + TEST_EDAN_ID, StringUtils.isNotEmpty(result_deploymentId));

        LOG.info("Expected DeploymentId = {}, Returned DeploymentId = {}", TEST_DEPLOYMENT_ID, result_deploymentId);
        assertEquals("The returned DeploymentId does not match", TEST_DEPLOYMENT_ID, result_deploymentId);

        for (int i = 0; i < json.getJSONArray("rows").length(); i++) {
            if (json.getJSONArray("rows").getJSONObject(i).has("image")) {
                LOG.debug("image_sequence_id = {}", json.getJSONArray("rows").getJSONObject(i).getJSONObject("content").getJSONObject("image").getString("id"));
            }
        }
    }

    @Test
    public void edanMetadataSearchFilterQueryCtProjectAndDeploymentIdTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.project_id:" + TEST_PROJECT_ID + "\", \"p.emammal_image.deployment_id:" + TEST_DEPLOYMENT_ID + "\"]", "UTF-8") + "&rows=20&start=0");

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        //Get the record with the correct test EDAN Id we may receive more than one EDAN record.
        String result_deploymentId = null;
        String result_projectId = null;
        for (int i = 0; i < json.getJSONArray("rows").length(); i++) {
            if (json.getJSONArray("rows").getJSONObject(i).getString("id").equalsIgnoreCase(TEST_EDAN_ID)) {
                result_deploymentId = json.getJSONArray("rows").getJSONObject(i).getJSONObject("content").getString("deployment_id");
                result_projectId = json.getJSONArray("rows").getJSONObject(0).getJSONObject("content").getString("project_id");
            }
        }

        assumeTrue("The EDAN response does not contain our test EDAN Id:" + TEST_EDAN_ID, StringUtils.isNotEmpty(result_deploymentId) && StringUtils.isNotEmpty(result_projectId));

        LOG.info("Expected DeploymentId = {}, Returned DeploymentId = {}", TEST_DEPLOYMENT_ID, result_deploymentId);
        assertEquals("The returned DeploymentId does not match", TEST_DEPLOYMENT_ID, result_deploymentId);

        LOG.info("Expected ProjectId = {}, Returned ProjectId = {}", TEST_PROJECT_ID, result_projectId);
        assertEquals("The returned ProjectId does not match", TEST_PROJECT_ID, result_projectId);

        for (int i = 0; i < json.getJSONArray("rows").length(); i++) {
            if (json.getJSONArray("rows").getJSONObject(i).has("image")) {
                LOG.debug("image_sequence_id = {}", json.getJSONArray("rows").getJSONObject(i).getJSONObject("content").getJSONObject("image").getString("id"));
            }
        }
    }

    @Test
    public void edanMetadataSearchFilterQueryImageIdTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.id:" + TEST_IAMGE_ID + "\"]", "UTF-8"));

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));
        LOG.info("Result Count = {}", json.getJSONArray("rows").length());
        assertEquals("Result Count does not match.", 1, json.getJSONArray("rows").length());

        String result_deploymentId = json.getJSONArray("rows").getJSONObject(0).getJSONObject("content").getString("deployment_id");
        LOG.info("Expected DeploymentId = {}, Returned DeploymentId = {}", TEST_DEPLOYMENT_ID , result_deploymentId);
        assertEquals("The returned DeploymentId does not match", TEST_DEPLOYMENT_ID, result_deploymentId);

        int result_count = json.getInt("rowCount");
        LOG.info("Expected Row Count = {}, Returned Row Count = {}", 1 , result_count);
        assertEquals("The returned Row Count does not match", 1, result_count);

        String result_imageId = json.getJSONArray("rows").getJSONObject(0).getJSONObject("content").getJSONObject("image").getString("id");
        LOG.info("Expected ImageId = {}, Returned ImageId = {}", TEST_IAMGE_ID , result_imageId);
        assertEquals("The returned ImageId does not match", TEST_IAMGE_ID, result_imageId);

    }

    @Test
    public void edanMetadataSearchFilterQueryIdsIdTest() throws Exception {
        assumeTrue("Edan Server Cannot be reached!", hostAvailabilityCheck(EDAN_TEST_URI, 80));

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        MockEndpoint mockImageIdResult = getMockEndpoint("mock:imageIdResult");
        mockImageIdResult.expectedMessageCount(1);
        mockImageIdResult.expectedHeaderReceived("edanId", TEST_EDAN_ID);
        mockImageIdResult.expectedHeaderReceived("edanTitle", TEST_TITLE);
        mockImageIdResult.expectedHeaderReceived("edanType", TEST_TYPE);
        mockImageIdResult.expectedHeaderReceived("idsId", props.getProperty("si.edu.idsAssetImagePrefix") + TEST_IAMGE_ID);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.id:" + TEST_IAMGE_ID + "\"]", "UTF-8") + "&rows=20&start=0");

        template.send("direct:edanTest", exchange);

        String edanResult = mockResult.getExchanges().get(0).getIn().getBody(String.class);

        template.sendBody("direct:unmarshalJSON", edanResult);

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        LOG.info("Result Count = {}", json.getJSONArray("rows").length());
        assertEquals("Result Count does not match.", 1, json.getJSONArray("rows").length());

        String result_imageId = json.getJSONArray("rows").getJSONObject(0).getJSONObject("content").getJSONObject("image").getString("id");
        LOG.info("Expected ImageId = {}, Returned ImageId = {}", TEST_IAMGE_ID , result_imageId);
        assertEquals("The returned ImageId does not match", TEST_IAMGE_ID, result_imageId);

        String result_idsId = json.getJSONArray("rows").getJSONObject(0).getJSONObject("content").getJSONObject("image").getJSONArray("online_media").getJSONObject(0).getString("idsId");
        LOG.info("Expected IdsId = {}, Returned IdsId = {}", props.getProperty("si.edu.idsAssetImagePrefix") + TEST_IAMGE_ID , result_idsId);
        assertEquals("The returned IdsId does not match", props.getProperty("si.edu.idsAssetImagePrefix") + TEST_IAMGE_ID, result_idsId);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void edanMetadataSearchFilterQuerySidoraPidTest() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(2);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "q=id:" + TEST_EDAN_ID);
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "q=" + URLEncoder.encode("app_id:" + TEST_APP_ID + " AND type:" + TEST_TYPE + " AND id:" + TEST_EDAN_ID, "UTF-8"));

        template.send("direct:edanTest", exchange);

        /*Not Working*/
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:" + TEST_SIDORA_PID + "\"]", "UTF-8"));
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:test\\:0001\"]", "UTF-8"));
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:test%5C%3A0001\"]", "UTF-8"));
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:test", "UTF-8") + "\\:" + URLEncoder.encode("0001\"]", "UTF-8"));
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:\"test:0001\"\"]", "UTF-8"));
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:*\"]", "UTF-8"));

        /*Working*/
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:test*0001\"]", "UTF-8")); //works with *
        //exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:test?0001\"]", "UTF-8"));  //works with ?

        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.online_media.sidorapid:" + TEST_SIDORA_PID.replace(":", "?") + "\"]", "UTF-8"));

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    @Ignore
    public void edanDeleteTest() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        List<String> delIdList = Arrays.asList("p2b-1527589878996-1527781471561-0", "p2b-1527589878996-1527718044046-0", "p2b-1527589878996-1527781554376-0", "p2b-1527589878996-1527781635832-0");

        for (String id : delIdList) {
            Exchange exchange = new DefaultExchange(context);
            exchange.getIn().setHeader("edanServiceEndpoint", "/content/v1.1/admincontent/releaseContent.htm");
            exchange.getIn().setHeader(Exchange.HTTP_QUERY, "id=" + id + "&type=" + TEST_TYPE);

            template.send("direct:edanTest", exchange);
        }

        assertMockEndpointsSatisfied();
    }

    /*@Test
    @Ignore
    public void edanMetadataSearchFilterQueryUnmarshalJSONTest() throws Exception {
        assumeFalse(LOCAL_TEST);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        MockEndpoint mockImageIdResult = getMockEndpoint("mock:imageIdResult");
        mockImageIdResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "fqs=" + URLEncoder.encode("[\"p.emammal_image.image.id:" + TEST_IAMGE_ID + "\"]", "UTF-8") + "&rows=20&start=0");

        template.send("direct:edanTest", exchange);

        String edanResult = mockResult.getExchanges().get(0).getIn().getBody(String.class);

        template.sendBody("direct:unmarshalJSON", edanResult);

        EdanSearch edanSearch = mockImageIdResult.getExchanges().get(0).getIn().getBody(EdanSearch.class);

        assertEquals("d33997s6i1", edanSearch.getRows().get(0).getContent().getImage().getId());

        assertMockEndpointsSatisfied();
    }*/

    @Test
    public void solrEdan9010Test() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");

        //Test EDAN Solr
        SOLR_PORT = 9010;
        String uri = SOLR_SERVER + ":" + SOLR_PORT + "/solr/content_objects/select";
        String solrQuery = "app_id:" + TEST_APP_ID + " AND url:" + TEST_IAMGE_ID.toLowerCase();
        String httpQuery = "?q=" + URLEncoder.encode(solrQuery, "UTF-8") + "&wt=json&indent=true";

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setHeader("CamelHttpQuery", httpQuery);
        exchange.getIn().setHeader("uri", uri);

        assumeTrue("The Solr Server could not be found! Make sure you are connected to SI VPN if you want this test to run!", hostAvailabilityCheck(SOLR_SERVER, SOLR_PORT));
        template.send("direct:solrTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result_appId = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("app_id");
        LOG.info("Expected app_id = {}, Returned app_id = {}", TEST_APP_ID , result_appId);
        assertEquals("The returned app_id does not match", TEST_APP_ID, result_appId);

        String result_url = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("url");
        LOG.info("Expected url = {}, Returned url = {}", TEST_IAMGE_ID , result_url);
        assertTrue("The returned url does not match", result_url.equalsIgnoreCase(TEST_IAMGE_ID));

        String result_id = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("id");
        LOG.info("Expected id = {}, Returned id = {}", TEST_EDAN_ID , result_id);
        assertEquals("The returned id does not match", TEST_EDAN_ID, result_id);
    }

    @Test
    public void solrEdan9020Test() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");

        //Test EDAN Solr Search index
        SOLR_PORT = 9020;
        String uri = SOLR_SERVER + ":" + SOLR_PORT + "/solr/search/select";
        String solrQuery = "app_id:" + TEST_APP_ID + " AND url:" + TEST_IAMGE_ID.toLowerCase();
        String httpQuery = "?q=" + URLEncoder.encode(solrQuery, "UTF-8") + "&wt=json&indent=true";

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setHeader("CamelHttpQuery", httpQuery);
        exchange.getIn().setHeader("uri", uri);

        assumeTrue("The Solr Server could not be found! Make sure you are connected to SI VPN if you want this test to run!", hostAvailabilityCheck(SOLR_SERVER, SOLR_PORT));

        template.send("direct:solrTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String result_appId = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("app_id");
        LOG.info("Expected app_id = {}, Returned app_id = {}", TEST_APP_ID , result_appId);
        assertEquals("The returned app_id does not match", TEST_APP_ID, result_appId);

        String result_url = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("url");
        LOG.info("Expected url = {}, Returned url = {}", TEST_IAMGE_ID , result_url);
        assertTrue("The returned url does not match", result_url.equalsIgnoreCase(TEST_IAMGE_ID));

        String result_id = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("id");
        LOG.info("Expected id = {}, Returned id = {}", TEST_EDAN_ID , result_id);
        assertEquals("The returned id does not match", TEST_EDAN_ID, result_id);

    }

    /**
     * Solr 3.2 instances were deprecated and retired
     * @throws Exception
     */
    @Test
    @Ignore
    public void solrIDSTest() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");

        //Test IDS solr
        SOLR_PORT = 8089;
        String uri = SOLR_SERVER + ":" + SOLR_PORT + "/solr/ids/select";
        String solrQuery = "archive:EMAMMAL AND uan:emammal_image_d33997s6i1";
        String httpQuery = "?q=" + URLEncoder.encode(solrQuery, "UTF-8") + "&wt=json&indent=true";

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setHeader("CamelHttpQuery", httpQuery);
        exchange.getIn().setHeader("uri", uri);

        assumeTrue("The Solr Server could not be found! Make sure you are connected to SI VPN if you want this test to run!", hostAvailabilityCheck(SOLR_SERVER, SOLR_PORT));

        template.send("direct:solrTest", exchange);

        assertMockEndpointsSatisfied();

        JSONObject json = new JSONObject(mockResult.getExchanges().get(0).getIn().getBody(String.class));

        String expected_archive = StringUtils.substringBetween(solrQuery, "archive:", " ");
        String result_archive = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("archive");
        LOG.info("Expected archive = {}, Returned archive = {}", expected_archive , result_archive);
        assertEquals("The returned archive does not match", expected_archive, result_archive);

        String expected_uan = StringUtils.substringAfter(solrQuery, "uan:");
        String result_uan = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("uan");
        LOG.info("Expected uan = {}, Returned uan = {}", expected_uan , result_uan);
        assertEquals("The returned uan does not match", expected_uan, result_uan);

    }


    /*@Test
    public void edanMetadataSearch2Test() throws Exception {
        assumeFalse(LOCAL_TEST);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);
        //exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v1.1/metadata/search.htm");
        exchange.getIn().setHeader("edanServiceEndpoint", "/metadata/v2.0/metadata/search.htm");
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "q=" + URLEncoder.encode("id:edanmdm-NMAI*", "UTF-8"));

        template.send("direct:edanTest", exchange);

        assertMockEndpointsSatisfied();
    }*/



    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                getContext().getComponent("properties", PropertiesComponent.class).setOverrideProperties(props);

                from("direct:edanTest").routeId("edanTest")
                        //.to("bean:edanApiBean?method=sendRequest(*, ${header.edanServiceEndpoint}, ${header.edanQueryParams})")
                        .to("bean:edanApiBean?method=sendRequest")
                        .to("log:test?showAll=true&multiline=true&maxChars=100000")
                        .convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "${id} EdanIds: EDAN Request Status: ${header.CamelHttpResponseCode}, Response: ${body}")
                        .to("mock:result");

                from("direct:createEdanJsonContent").routeId("createEdanJsonContent")
                        .log(LoggingLevel.INFO, "${id} EdanIds: Starting Edan JSON Content creation...")

                        // create JSON content for EDAN
                        .setHeader("CamelFedoraPid", simple("${header.pid}"))
                        .setHeader("extraJson").simple("{{si.ct.uscbi.extra_property}}")
                        .setBody(simple("${header.manifestXML}"))
                        //.to("xslt:file:{{karaf.home}}/Input/xslt/edan_Transform.xsl?saxon=true").id("transform2json")
                        .to("xslt:file:{{karaf.home}}/Input/xslt/edan_Transform_2_xml.xsl?saxon=true").id("transform2xml")

                        // convert xslt xml output to json and convert array elements to json array
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String xmlBody = out.getBody(String.class);
                                try {
                                    //JSONObject xmlJSONObj = XML.toJSONObject(xmlBody, true);
                                    JSONObject xmlJSONObj = XML.toJSONObject(xmlBody);
                                    log.debug("xml 2 json Output:\n{}", xmlJSONObj);

                                    JSONObject edan_content = xmlJSONObj.getJSONObject("xml").getJSONObject("content");
                                    log.debug("json edan_content Output:\n{}", edan_content.toString(4));

                                    //convert online_media to json array
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

                                    //convert image_identifications to json array
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

                                    log.debug("json final edan_content :\n{}", xmlJSONObj.getJSONObject("xml").toString(4));
                                    out.setBody(xmlJSONObj.getJSONObject("xml").toString());
                                } catch (JSONException je) {
                                    throw new EdanIdsException("Error creating edan json", je);
                                }
                            }
                        }).id("edanConvertXml2Json")

                        .log(LoggingLevel.INFO, "${id} EdanIds: EDAN JSON content before encoding: ${body}")
                        .setHeader("edanJson").groovy("URLEncoder.encode(request.body, 'UTF-8')")
                        .log(LoggingLevel.INFO, "${id} EdanIds: EDAN JSON content encoded: ${header.edanJson}")
                        .log(LoggingLevel.INFO, "${id} EdanIds: Finished Edan JSON Content creation...")
                        .to("mock:result");

                from("direct:solrTest").routeId("solrTest")
                        .log(LoggingLevel.INFO, "Solr uri = ${header.uri}")
                        .toD("${header.uri}")
                        .convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "Solr Response Body:\n${body}")
                        .to("mock:result");

                from("direct:unmarshalJSON").routeId("unmarshalJSON")
                        .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                        //.unmarshal().json(JsonLibrary.Jackson, EdanSearch.class) //converts json to EdanSearch java object
                        .log(LoggingLevel.INFO, "Unmarshal Body Type: ${body.class.name}")
                        .log(LoggingLevel.INFO, "Body After Unmarshal from JSON= ${body}")

                        .setHeader("edanId").simple("${body[rows].get(0)[id]}")
                        .setHeader("edanTitle").simple("${body[rows].get(0)[title]}")
                        .setHeader("edanType").simple("${body[rows].get(0)[type]}")
                        .setHeader("idsId").simple("${body[rows].get(0)[content][image][online_media].get(0)[idsId]}")
                        .to("mock:imageIdResult");

                /*from("direct:solrTest").routeId("solrTest")
                        .log(LoggingLevel.INFO, "Solr uri = ${header.uri}")
                        .toD("${header.uri}")
                        .convertBodyTo(String.class)
                        .log(LoggingLevel.INFO, "Solr Http Response Body:\n${body}")
                        .log(LoggingLevel.INFO, "Body Type Before Unmarshal to JSON= ${body.class.name}")
                        .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                        .log(LoggingLevel.INFO, "Body Type After Unmarshal to JSON= ${body.class.name}")
                        .log(LoggingLevel.INFO, "Body After Unmarshal = ${body}")
                        .setBody().simple("${body[response][docs]}")
                        .log(LoggingLevel.INFO, "Solr Docs returned:\n${body}")
                        .log(LoggingLevel.INFO, "Body Type After for Docs = ${body.class.name}")
                        .to("mock:result");*/

            }
        };
    }

    public static boolean hostAvailabilityCheck(String host, int port) {
        LOG.info("Checking Host Availability: {}", host+":"+port);
        try (Socket socket = new Socket(new URL(host).getHost(), port)) {
            return true;
        } catch (IOException ex) {
        /* ignore */
        }


        return false;
    }
}
