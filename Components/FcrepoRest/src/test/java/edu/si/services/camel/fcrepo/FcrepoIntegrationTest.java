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

package edu.si.services.camel.fcrepo;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.auth.AuthScope.ANY;

/**
 * Integration testing for the FcrepoComponent.
 *
 * @author parkjohn
 */
public class FcrepoIntegrationTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    private MockEndpoint mockEndpoint;
    private final FcrepoConfiguration fcrepoConfiguration = new FcrepoConfiguration();

    protected static final String FEDORA_URI = System.getProperty("si.fedora.host");
    protected static final String FUSEKI_URI = System.getProperty("si.fuseki.host");
    private static final String BUILD_DIR = System.getProperty("buildDirectory");
    private static final String PID = "test:deploymentObject";
    private static final String TEST_FOXML = "/test-classes/test_deploymentObject.xml";

    private static CloseableHttpClient httpClient;

    private static final Logger logger = LoggerFactory.getLogger(FcrepoIntegrationTest.class);

    @BeforeClass
    public static void loadObjectsIntoFedora() throws IOException, InterruptedException {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(ANY, new UsernamePasswordCredentials(System.getProperty("si.fedora.user"), System.getProperty("si.fedora.password")));
        httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        ingest(PID, Paths.get(BUILD_DIR, TEST_FOXML));
    }

    private static void ingest(String pid, Path payload) throws IOException {
        if (!checkObjectExist(pid)) {
            String ingestURI = FEDORA_URI + "/objects/" + pid + "?format=info:fedora/fedora-system:FOXML-1.1&ignoreMime=true";

            logger.debug("INGEST URI = {}", ingestURI);

            HttpPost ingest = new HttpPost(ingestURI);
            ingest.setEntity(new ByteArrayEntity(Files.readAllBytes(payload)));
            ingest.setHeader("Content-type", MediaType.TEXT_XML);
            try (CloseableHttpResponse pidRes = httpClient.execute(ingest)) {
                assertEquals("Failed to ingest " + pid + "!", SC_CREATED, pidRes.getStatusLine().getStatusCode());
                logger.info("Ingested test object {}", EntityUtils.toString(pidRes.getEntity()));
            }
        }
    }

    private static boolean checkObjectExist(String pid) throws IOException {
        String query = URLEncoder.encode("ASK FROM <info:edu.si.fedora#ri> WHERE { <info:fedora/" + pid + "> ?p ?o}", "UTF-8");

        HttpGet request = new HttpGet(FUSEKI_URI + "/fedora3?output=xml&query=" + query);
        final boolean exists;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String pidExists = EntityUtils.toString(response.getEntity()).split("<boolean>")[1].split("</boolean>")[0];
            exists = Boolean.parseBoolean(pidExists);
            logger.info("PID = {}, Exists = {}", pid, exists);
        }

        return exists;
    }

    @AfterClass
    public static void cleanUpHttpClient() throws IOException {
        httpClient.close();
    }

    /**
     * Test calling datastreams api using the Fcrepo component.
     * This test currently assumes Fedora repository is running with test:deploymentObject object exists.
     *
     * Use the test_deploymentObject.xml FOXML 1.1 export found in the test/resources directory to create one if needed.
     *
     */
    @Test
    public void testGetDatastreamsFound()
    {
        String expectedStatus = "200";
        String expectedContentType = "text/xml";
        String deploymentPID = "test:deploymentObject";

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, FcrepoHttpMethodEnum.GET);
        headers.put("SitePID", deploymentPID);
        template.sendBodyAndHeaders("direct:testFcrepoComponent", "foo", headers);

        //getting the response message
        Message message = this.mockEndpoint.getExchanges().get(0).getIn();

        String responseHttpCode = message.getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        String responseContentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);

        assertEquals(expectedStatus, responseHttpCode);
        assertEquals(expectedContentType, responseContentType);

        String body = message.getBody(String.class);
        assertNotNull(body);
    }


    /**
     * Test calling datastreams api and expecting it to return 404
     * This test currently assumes Fedora service is running.
     */
    @Test
    public void testGetDatastreamsNotFound()
    {
        String expectedStatus = "404";
        String expectedContentType = "text/plain";
        String deploymentPID = "test:notfound";

        //setting up expected headers before sending message to test route
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, FcrepoHttpMethodEnum.GET);
        headers.put("SitePID", deploymentPID);
        template.sendBodyAndHeaders("direct:testFcrepoComponent", "bar", headers);

        //getting the response message
        Message message = this.mockEndpoint.getExchanges().get(0).getIn();

        String responseHttpCode = message.getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        String responseContentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);

        assertEquals(expectedStatus, responseHttpCode);
        assertEquals(expectedContentType, responseContentType);
    }

    /**
     * Setting up the camel context with the fcrepo configuration
     *
     * @return CamelContext
     * @throws Exception
     */
    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();
        PropertiesComponent prop = context.getPropertiesComponent();
        prop.setLocation("classpath:fcrepotest.properties");

        try {
            System.setProperty("si.fedora.host", context.resolvePropertyPlaceholders("{{si.fedora.host}}"));
            fcrepoConfiguration.setHost(context.resolvePropertyPlaceholders("{{si.fedora.host}}"));
            fcrepoConfiguration.setUsername(context.resolvePropertyPlaceholders("{{si.fedora.user}}"));
            fcrepoConfiguration.setPassword(context.resolvePropertyPlaceholders("{{si.fedora.password}}"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return context;
    }

    /**
     * Registering the fcrepoConfiguration bean
     *
     * @return JndiRegistry
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.getRegistry().bind("fcrepoConfiguration", fcrepoConfiguration);
    }

    /**
     * Creating route builder for the test run
     *
     * @return RouteBuilder
     */
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:testFcrepoComponent")
                        .routeId("testFcrepoComponentRoute")
                        .toD("fcrepo:objects/${header.SitePID}/datastreams?format=xml")
                        .to("mock:result")
                        .end();
            }
        };
    }
}
