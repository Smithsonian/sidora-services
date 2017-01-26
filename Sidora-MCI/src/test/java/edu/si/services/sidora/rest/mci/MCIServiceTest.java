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
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

/**
 * @author jbirkhimer
 */
public class MCIServiceTest extends CamelBlueprintTestSupport {

    private static final String SERVICE_ADDRESS = "/sidora/rest";
    private static final String PORT_PATH = 8282 + SERVICE_ADDRESS;
    private static final String BASE_URL = "http://localhost:" + PORT_PATH;

    //Default Test Params
    private static final String CORRELATIONID = UUID.randomUUID().toString();
    private static final String OWNERID = "test:123456";
    private static String OPTION = "MCITest";
    private static String PAYLOAD;
    private static File TEST_XML = new File("src/test/resources/sample-data/42_0.1.xml");
    private static String RESPONCE_PAYLOAD;
    private static File TEST_RESPONCE_XML = new File("src/test/resources/sample-data/MCIProjectResult.xml");

    static {
        try {
            PAYLOAD = FileUtils.readFileToString(TEST_XML);
            RESPONCE_PAYLOAD = FileUtils.readFileToString(TEST_RESPONCE_XML);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private JAXBContext jaxb;
    private CloseableHttpClient httpClient;

    private static final Properties prop = new Properties();

    @Test
    public void testPostWithParameterAndPayload() throws Exception {

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("ownerId", OWNERID);
        mockEndpoint.expectedHeaderReceived("option", OPTION);
        mockEndpoint.expectedBodyReceived().simple(PAYLOAD);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/mci/" + OWNERID + "/addProject?option=" + OPTION);
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(PAYLOAD));
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        assertEquals(RESPONCE_PAYLOAD, responceBody);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMultipartPostWithParametersAndPayload() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("ownerId", OWNERID);
        mockEndpoint.expectedHeaderReceived("option", OPTION);
        /*mockEndpoint.expectedHeaderReceived("mciProjectString", PAYLOAD);
        mockEndpoint.expectedHeaderReceived("mciProjectDataHandler", PAYLOAD);
        mockEndpoint.expectedHeaderReceived("mciProjectAttachment", PAYLOAD);*/
        //mockEndpoint.expectedBodyReceived().simple(PAYLOAD);

        context.getRouteDefinition("AddMCIProjectMultipart").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        HttpPost post = new HttpPost(BASE_URL + "/mci/" + OWNERID + "/addProject?option=" + OPTION);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);
        builder.addBinaryBody("mciProject", new File(this.getClass().getClassLoader().getResource("sample-data/42_0.1.xml").toURI()), ContentType.create("application/xml"), "42_0.1.xml");
        //builder.addBinaryBody("mciProjectDataHandler", new File(this.getClass().getClassLoader().getResource("sample-data/42_0.1.xml").toURI()), ContentType.create("application/xml"), "42_0.1.xml");
        //builder.addBinaryBody("mciProjectAttachment", new File(this.getClass().getClassLoader().getResource("sample-data/42_0.1.xml").toURI()), ContentType.create("application/xml"), "42_0.1.xml");

        //builder.addTextBody("body", FileUtils.readFileToString(new File(this.getClass().getClassLoader().getResource("sample-data/42_0.1.xml").toURI())), ContentType.create("application/xml"));

        post.setEntity(builder.build());
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        //Map<String, org.apache.camel.Attachment> camelAttachments = exchange.getOut().getAttachmentObjects();

        assertEquals(RESPONCE_PAYLOAD, responceBody);

        assertMockEndpointsSatisfied();
    }

    /**
     * Sets up the system properties and Temp directories used by the route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        FileInputStream propFile = new FileInputStream( "src/test/resources/test.properties");
        System.setProperty("karaf.home", "target/test-classes");
        prop.load(propFile);
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {

        /*File dir = new File("target/etc");
        dir.mkdirs();

        FileWriter writer = null;
        File cfg = null;
        try {
            cfg = File.createTempFile("test-properties-", ".cfg", dir);
            writer = new FileWriter(cfg);
            prop.store(writer, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new String[]{cfg.getAbsolutePath(), "edu.si.sidora.batch"};*/

        return new String[]{"src/test/resources/test.properties", "edu.si.sidora.mci"};
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return prop;
    }


    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();

        reg.bind("jsonProvider", org.apache.cxf.jaxrs.provider.json.JSONProvider.class);
        reg.bind("jaxbProvider", org.apache.cxf.jaxrs.provider.JAXBElementProvider.class);

        return reg;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Before
    @Override
    public void setUp() throws Exception {
        httpClient = HttpClientBuilder.create().build();
        super.setUp();
        //jaxb = JAXBContext.newInstance(CustomerList.class, Customer.class, Order.class, Product.class);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        httpClient.close();
    }
}
