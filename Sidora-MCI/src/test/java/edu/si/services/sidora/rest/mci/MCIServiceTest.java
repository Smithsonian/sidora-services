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

package edu.si.services.sidora.rest.mci;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.commons.collections.map.HashedMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class MCIServiceTest extends MCI_BlueprintTestSupport {

    static private String LOG_NAME = "edu.si.mci";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = true;
    private String defaultTestProperties = "src/test/resources/test.properties";

    private static final String SERVICE_ADDRESS = "/sidora/mci";
    private static final String PORT = String.valueOf(AvailablePortFinder.getNextAvailable());
    private static final String PORT_PATH = PORT + SERVICE_ADDRESS;
    private static final String BASE_URL = "http://localhost:" + PORT_PATH;

    //Default Test Params
    private static File TEST_XML = new File("src/test/resources/sample-data/MCI_Inbox/valid-mci-payload.xml");
    private static File TEST_BAD_XML = new File("src/test/resources/sample-data/MCI_Inbox/bad-mci-payload.xml");

    private CloseableHttpClient httpClient;

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();
        reg.bind("jsonProvider", org.apache.cxf.jaxrs.provider.json.JSONProvider.class);
        reg.bind("jaxbProvider", org.apache.cxf.jaxrs.provider.JAXBElementProvider.class);
        return reg;
    }

    @Override
    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList("target/test-classes/etc/edu.si.sidora.mci.cfg",
                "target/test-classes/sql/mci.sql.properties");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setUseActualFedoraServer(USE_ACTUAL_FEDORA_SERVER);
        setDefaultTestProperties(defaultTestProperties);
        httpClient = HttpClientBuilder.create().build();
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        httpClient.close();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void testFolderHolderNotFoundRetry() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("mciFolderHolder", "testUser");
        mockResult.expectedHeaderReceived("mciOwnerPID", "si-user:57");

        MockEndpoint mockFolderHolderResult = getMockEndpoint("mock:folderHolderResult");
        mockFolderHolderResult.expectedMessageCount(11);

        context.getRouteDefinition("ProcessMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("consumeRequest").remove();
                weaveById("folderHolderXpath").remove();
                weaveById("setOwnerPID").before().to("mock:result").stop();
            }
        });

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                weaveById("queryFolderHolder").replace().setBody().constant(sqlResultBody).to("mock:folderHolderResult");
            }
        });

        context.start();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("mciFolderHolder", "testUser");

        template.send("seda:processProject", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testBadXMLPayload() throws Exception {

        String payload = "<Fields>\n" +
                "    <Field Type=\"Text\" Name=\"Title\" DisplayName=\"Project Title\">Testing of MCI Project request - all approval go\n" +
                "        through.\n" +
                "    </Field>\n" +
                "    <Field Type=\"User\" Name=\"Folder_x0020_Holder\" DisplayName=\"Folder Holder\">i:0#.w|us\\testFolderHolder</Field>\n" +
                "    <Field Type=\"User\" Name=\"Folder_x0020_Holder\" DisplayName=\"Folder Holder\">i:0#.w|us\\testFolderHolder</Field>\n" +
                "</Fields>";

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        //post.setEntity(new StringEntity(readFileToString(TEST_BAD_XML)));
        post.setEntity(new StringEntity(payload));
        HttpResponse response = httpClient.execute(post);
        assertEquals(400, response.getStatusLine().getStatusCode());

        String responseBody = EntityUtils.toString(response.getEntity());

        assertStringContains(responseBody, "Error reported: Validation failed for");
    }

    @Test
    public void testRequest() throws Exception {

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("createRequest").remove();
                weaveByToString(".*seda:processProject.*").remove();
            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        //post.setEntity(new StringEntity(readFileToString(new File("src/test/resources/sample-data/MCI_Inbox/small.xml"))));
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responseBody = EntityUtils.toString(response.getEntity());

        assertStringContains(responseBody, "OK :: Created");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRequestNoDrupalDB() throws Exception {

        MockEndpoint mockFolderHolderResult = getMockEndpoint("mock:folderHolderResult");
        mockFolderHolderResult.expectedMessageCount(11);

        MockEndpoint mockProcessMCIProjectResult = getMockEndpoint("mock:processMCIProjectResult");
        mockProcessMCIProjectResult.expectedMessageCount(1);

        MockEndpoint mockMciFindObjectByPIDPredicate= getMockEndpoint("mock:mciFindObjectByPIDPredicate");
        mockMciFindObjectByPIDPredicate.expectedMessageCount(1);
        mockMciFindObjectByPIDPredicate.expectedBodiesReceived("true");

        context.getRouteDefinition("ProcessMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Skip Object Creation
                interceptSendToEndpoint("direct:mciCreateConcept").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, LOG_NAME, "Skip Creating Fedora Objects");
                weaveAddLast().to("mock:processMCIProjectResult");
            }
        });

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                weaveById("queryFolderHolder").replace().setBody().constant(sqlResultBody).to("mock:folderHolderResult");
            }
        });

        /*context.getRouteDefinition("MCIFindDefaultUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                Map<String, Object> sqlmap = new HashedMap();
                sqlmap.put("name", "sternb");
                sqlmap.put("user_pid", "si-user:57");
                sqlResultBody.add(sqlmap);
                weaveById("queryDefaultUser").replace().setBody().constant(sqlResultBody);
            }
        });*/

        context.getRouteDefinition("MCIFindObjectByPIDPredicate").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:mciFindObjectByPIDPredicate");
            }
        });

        HttpPost post = new HttpPost(BASE_URL + OWNERID + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        //post.setEntity(new StringEntity(readFileToString(new File("src/test/resources/sample-data/MCI_Inbox/small.xml"))));
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responseBody = EntityUtils.toString(response.getEntity());

        assertStringContains(responseBody, "OK :: Created");

        assertMockEndpointsSatisfied(15, TimeUnit.SECONDS);
        //assertMockEndpointsSatisfied();
    }
}
