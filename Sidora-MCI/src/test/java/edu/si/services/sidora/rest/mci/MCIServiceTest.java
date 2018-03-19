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

import edu.si.services.fedorarepo.FedoraObjectNotFoundException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
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

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class MCIServiceTest extends MCI_BlueprintTestSupport {

    static private String LOG_NAME = "edu.si.mci";

    protected static String MCI_URI;
    private static final String KARAF_HOME = System.getProperty("karaf.home");

    //Default Test Params
    private static File TEST_XML = new File(KARAF_HOME + "/sample-data/MCI_Inbox/valid-mci-payload.xml");
    private static File TEST_BAD_XML = new File(KARAF_HOME + "/sample-data/MCI_Inbox/bad-mci-payload.xml");

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

    @Before
    @Override
    public void setUp() throws Exception {
        httpClient = HttpClientBuilder.create().build();
        super.setUp();

        MCI_URI = getProps().getProperty("sidora.mci.service.address");
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

    /**
     * Test the correct return response is received
     *
     * @throws Exception
     */
    @Test
    public void testRequestOKResponse() throws Exception {

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Skip adding request to database
                weaveById("createRequest").remove();
                //Skip further processing since all we are interested in is the return response
                weaveByToString(".*seda:processProject.*").remove();
            }
        });

        HttpPost post = new HttpPost(MCI_URI + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        //post.setEntity(new StringEntity(readFileToString(new File(KARAF_HOME + "/sample-data/MCI_Inbox/small.xml"))));
        post.setEntity(new StringEntity(readFileToString(TEST_XML)));
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responseBody = EntityUtils.toString(response.getEntity());

        assertStringContains(responseBody, "OK :: Created");

        assertMockEndpointsSatisfied();
    }

    /**
     * Test the correct return response is received when bad xml payload is received. Validation errors throw exception
     * and is handled by onException sending errors to log, and body for the response and skipping recording errors to database
     * @throws Exception
     */
    @Test
    public void testValidationFailedErrorResponse() throws Exception {

        String payload = "<Fields>\n" +
                "    <Field Type=\"Text\" Name=\"Title\" DisplayName=\"Project Title\">Testing of MCI Project request - all approval go\n" +
                "        through.\n" +
                "    </Field>\n" +
                "    <Field Type=\"User\" Name=\"Folder_x0020_Holder\" DisplayName=\"Folder Holder\">i:0#.w|us\\testFolderHolder</Field>\n" +
                "    <Field Type=\"User\" Name=\"Folder_x0020_Holder\" DisplayName=\"Folder Holder\">i:0#.w|us\\testFolderHolder</Field>\n" +
                "</Fields>";

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Skip adding request to database
                weaveById("createRequest").remove();
                weaveById("onExceptionAddValidationErrorsToDB").remove();
                //Skip further processing since all we are interested in is the return response
                weaveByToString(".*seda:processProject.*").remove();
            }
        });

        //for (int i = 0; i < 25; i++) {

            HttpPost post = new HttpPost(MCI_URI + "/addProject");
            post.addHeader("Content-Type", "application/xml");
            post.addHeader("Accept", "application/xml");
            //post.setEntity(new StringEntity(readFileToString(TEST_BAD_XML)));
            post.setEntity(new StringEntity(payload));
            HttpResponse response = httpClient.execute(post);
            assertEquals(400, response.getStatusLine().getStatusCode());

            String responseBody = EntityUtils.toString(response.getEntity());
            String responseHeaders = Arrays.toString(response.getAllHeaders());

            assertStringContains(responseBody, "Error reported: Validation failed for");
        //}
    }

    /**
     * Test onException in AddMCIProject route for SQL exceptions retries and continues routing after retries are exhausted
     *
     * @throws Exception
     */
    @Test
    public void testSQLExceptionAndRetriesForAddMCIProjectRoute() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SQLException.class);
        mockResult.expectedHeaderReceived("redeliveryCount", 3);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws SQLException {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new SQLException("Testing AddMCIProject SQL exception handling");
                    }
                };

                //advice sending to database and replace with processor
                weaveById("createRequest").replace().process(processor);
                weaveById("sedaProcessProject").remove();
                weaveAddLast().to("mock:result");
            }
        });

        context.start();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("mciFolderHolder", "testUser");
        exchange.getIn().setBody(readFileToString(TEST_XML));

        template.send("direct:addProject", exchange);

        assertMockEndpointsSatisfied();
    }

    /**
     * Test onException retries when folder holder is not found in the Drupal dB and continues routing after
     * retries are exhausted setting the user pid to the default user.
     *
     * @throws Exception
     */
    @Test
    public void testFolderHolderNotFoundRetry() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("mciFolderHolder", "testUser");
        mockResult.expectedHeaderReceived("mciOwnerPID", getProps().getProperty("mci.default.owner.pid"));
        mockResult.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT).isInstanceOf(MCI_Exception.class);
        mockResult.expectedHeaderReceived("redeliveryCount", 10);

        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.expectedHeaderReceived("redeliveryCount", 10);

        context.getRouteDefinition("ProcessMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("MCI_ExceptionOnException").after().to("mock:error");
                weaveById("consumeRequest").remove();
                weaveById("folderHolderXpath").remove();
                weaveById("setOwnerPID").before().to("mock:result").stop();
            }
        });

        context.getRouteDefinition("MCIFindFolderHolderUserPID").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //processor used to replace sql query to test onException and retries
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws MCI_Exception {
                        Message in = exchange.getIn();
                        in.setHeader("redeliveryCount", in.getHeader(Exchange.REDELIVERY_COUNTER, Integer.class));
                        throw new MCI_Exception("Folder Holder User PID Not Found!!!");
                    }
                };

                weaveById("queryFolderHolder").remove();
                weaveById("throwMCIException").replace().process(processor);

            }
        });

        context.start();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("mciFolderHolder", "testUser");

        template.send("seda:processProject", exchange);

        assertMockEndpointsSatisfied(5, TimeUnit.SECONDS); // make sure the redeliveries finish
    }

    /**
     * Test the onException handling during processing without sending errors to database
     *
     * @throws Exception
     */
    @Test
    public void testAllOtherExceptionsDuringProcessMCIProjectRoute() throws Exception {
        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);
        mockError.expectedHeaderReceived(Exchange.REDELIVERY_COUNTER, 10);
        mockError.expectedHeaderReceived(Exchange.REDELIVERY_MAX_COUNTER, 10);
        mockError.expectedHeaderReceived(Exchange.REDELIVERED, true);

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(0);

        context.getRouteDefinition("ProcessMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {

                //processor used to to test onException
                final Processor processor = new Processor() {
                    public void process(Exchange exchange) throws FedoraObjectNotFoundException {
                        throw new FedoraObjectNotFoundException("The fedora object not found");
                    }
                };

                weaveById("processProjectOnExceptionSQL").replace().to("mock:error");
                interceptFrom().process(processor).to("mock:result");

            }
        });

        context.start();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Testing OnException");

        template.send("seda:processProject", exchange);

        //Give the redeliveries time to finish
        assertMockEndpointsSatisfied(15, TimeUnit.SECONDS);
    }

    /**
     * Testing the generic resource and FITS object creation
     *
     * NOTE: FITS must be installed
     *
     * @throws Exception
     */
    @Test
    public void testMCICreateResource() throws Exception {

        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        MockEndpoint mockFedora = getMockEndpoint("mock:fedora");
        mockFedora.expectedMessageCount(6);

        context.getRouteDefinition("MCICreateResource").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(".*fedora:.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, LOG_NAME, "Skip Sending to Fedora").to("mock:fedora");
                weaveAddLast().to("mock:result").stop();
            }
        });

        context.getRouteDefinition("MCIProjectAddFITSDataStream").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(".*fedora:.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, LOG_NAME, "Skip Sending to Fedora").to("mock:fedora");

                weaveById("fitsRequest").replace().setHeader("CamelHttpResponseCode", simple("200")).setBody().simple("Test Fits Output");
            }
        });

        context.start();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("mciFolderHolder", "testUser");
        exchange.getIn().setHeader("mciOwnerPID", "test-user:123");
        exchange.getIn().setHeader("projectPID", "test:0001");
        exchange.getIn().setHeader("CamelFedoraPid", "test:0002");
        exchange.getIn().setHeader("mciProjectXML", readFileToString(TEST_XML));

        template.send("direct:mciCreateResource", exchange);

        for ( Exchange fedoraExchange : mockFedora.getExchanges()) {
            log.info(fedoraExchange.getIn().getBody(String.class));
        }

        assertMockEndpointsSatisfied();

        assertStringContains(mockFedora.getExchanges().get(5).getIn().getBody(String.class), "Test Fits Output");

        deleteDirectory(KARAF_HOME + "/staging");
    }
}
