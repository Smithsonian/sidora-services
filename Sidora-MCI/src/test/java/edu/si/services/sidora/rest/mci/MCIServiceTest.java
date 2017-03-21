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
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class MCIServiceTest extends MCI_BlueprintTestSupport {

    static private String LOG_NAME = "edu.si.mci";

    private static final boolean USE_ACTUAL_FEDORA_SERVER = false;
    private String defaultTestProperties = "src/test/resources/test.properties";

    private static final String SERVICE_ADDRESS = "/sidora/mci";
    private static final String PORT = String.valueOf(AvailablePortFinder.getNextAvailable());
    private static final String PORT_PATH = PORT + SERVICE_ADDRESS;
    private static final String BASE_URL = "http://localhost:" + PORT_PATH;

    //Default Test Params
    private static File TEST_XML = new File("src/test/resources/sample-data/MCI_Inbox/42_0.1.xml");
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

    @Test
    public void testSQLExceptionRetries() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(11);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*direct:findFolderHolderUserPID.*").after().stop();
            }
        });

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("queryFolderHolder").replace().to("mock:result").throwException(new SQLException("Test Response"));
            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(500, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        assertEquals("Error reported: Test Response - cannot process this message.", responceBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCannotGetJdbcConnectionExceptionRetries() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(11);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*direct:findFolderHolderUserPID.*").after().stop();
            }
        });

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("queryFolderHolder").replace().to("mock:result").throwException(new CannotGetJdbcConnectionException("Test Response", new SQLException("Test SQL Exception")));
            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(500, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        assertEquals("Error reported: Test Response; nested exception is java.sql.SQLException: Test SQL Exception - cannot process this message.", responceBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testBothFolderHolderAndDefaultUserRetries() throws Exception {

        MockEndpoint mockFolderHolderResult = getMockEndpoint("mock:folderHolderResult");
        mockFolderHolderResult.expectedMessageCount(11);

        MockEndpoint mockDefaultUser = getMockEndpoint("mock:defaultUserResult");
        mockDefaultUser.expectedMessageCount(11);

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                weaveById("queryFolderHolder").replace().setBody().constant(sqlResultBody).to("mock:folderHolderResult");
            }
        });
        context.getRouteDefinition("MCIFindDefaultUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                weaveById("queryDefaultUser").replace().setBody().constant(sqlResultBody).to("mock:defaultUserResult");
            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(500, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        assertEquals("Error reported: Default MCI User PID Not Found!!! - cannot process this message.", responceBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFolderHolderNotFoundRetry() throws Exception {

        MockEndpoint mockFolderHolderResult = getMockEndpoint("mock:folderHolderResult");
        mockFolderHolderResult.expectedMessageCount(11);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*direct:findFolderHolderUserPID.*").after().stop();
            }
        });

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                weaveById("queryFolderHolder").replace().setBody().constant(sqlResultBody).to("mock:folderHolderResult");
            }
        });

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("mciFolderHolder", "testUser");
        exchange.getIn().setBody(readFileToString(TEST_XML));

        template.send("direct:addProject", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDefaultUserNotFoundRetryAndResponse() throws Exception {

        MockEndpoint mockDefaultUser = getMockEndpoint("mock:defaultUserResult");
        mockDefaultUser.expectedMessageCount(11);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*direct:findFolderHolderUserPID.*").remove();
            }
        });

        context.getRouteDefinition("MCIFindDefaultUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                List<Map<String,Object>> sqlResultBody = new ArrayList<Map<String,Object>>();
                weaveById("queryDefaultUser").replace().setBody().constant(sqlResultBody).to("mock:defaultUserResult");
            }
        });

        HttpPost post = new HttpPost(BASE_URL + OWNERID + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(500, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        assertEquals("Error reported: Default MCI User PID Not Found!!! - cannot process this message.", responceBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testBadXMLPayload() throws Exception {

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(readFileToString(TEST_BAD_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(400, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        assertEquals("Error reported: Error during type conversion from type: java.lang.String to the required type: org.w3c.dom.Document with value  <?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "        - <Fields>\n" +
                "    <Field Type=\"Text\" Name=\"Title\" DisplayName=\"Project Title\">Quantification of Boron in Kogod Courtyard Planter Materials</Field>\n" +
                "    <Field Type=\"Boolean\" Name=\"Confidential\" DisplayName=\"Confidential\">False</Field>\n" +
                "    <Field Type=\"Text\" Name=\"TemporaryRequest\" DisplayName=\"Temporary Request #\" />\n" +
                "    <Field Type=\"User\" Name=\"Initiator\" DisplayName=\"Initiator\">i:0#.w|us\\littlen</Field>\n" +
                "    <Field Type=\"Text\" Name=\"Status\" DisplayName=\"Approval Status\">Waiting for MCI Admin to Complete Approval</Field>\n" +
                "    <Field Type=\"Text\" Name=\"ConservationGroup\" DisplayName=\"Conservation Group\">Technical Studies</Field>\n" +
                "    <Field Type=\"Note\" Name=\"Project_x0020_Summary\" DisplayName=\"Project Summary\">Plants from the Kogod Courtyard have consistently exhibited signs of boron poisoning (browning of the leaves leading to death of the flora). Initial analyses have found excessively high boron content in the plant leaves with negligible bor... [Body clipped after 1000 chars, total length is 9932] due org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 7; The processing instruction target matching \"[xX][mM][lL]\" is not allowed. - cannot process this message.", responceBody);
    }
}
