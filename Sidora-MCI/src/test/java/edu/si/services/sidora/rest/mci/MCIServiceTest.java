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

import edu.si.services.fedorarepo.FedoraComponent;
import edu.si.services.fedorarepo.FedoraSettings;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;

/**
 * @author jbirkhimer
 */
public class MCIServiceTest extends CamelBlueprintTestSupport {

    private static final String SERVICE_ADDRESS = "/sidora/mci";
    private static final String PORT = String.valueOf(AvailablePortFinder.getNextAvailable());
    private static final String PORT_PATH = PORT + SERVICE_ADDRESS;
    private static final String BASE_URL = "http://localhost:" + PORT_PATH;

    //Default Test Params
    private static final String CORRELATIONID = UUID.randomUUID().toString();
    private static String OWNERID = "/si-user:57";
    private static String OPTION = "MCITest";
    private static String PAYLOAD;
    private static File TEST_XML = new File("src/test/resources/sample-data/MCI_Inbox/42_0.1.xml");
    private static String RESPONCE_PAYLOAD;
    private static File TEST_RESPONCE_XML = new File("src/test/resources/sample-data/42_0.1-Transform-Result.xml");

    private JAXBContext jaxb;
    private CloseableHttpClient httpClient;
    private static Configuration config = null;
    private static File tmpOutputDir = new File("MCI_Inbox");

    static {
        try {
            PAYLOAD = FileUtils.readFileToString(TEST_XML);
            RESPONCE_PAYLOAD = FileUtils.readFileToString(TEST_RESPONCE_XML);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    @Override
    public void setUp() throws Exception {
        System.setProperty("karaf.home", "target/test-classes");

        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(new File("src/test/resources/test.properties"))
                        );

        try {
            config = builder.getConfiguration();
            config.setProperty("sidora.mci.service.address", BASE_URL);
            builder.save();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        httpClient = HttpClientBuilder.create().build();

        //jaxb = JAXBContext.newInstance(CustomerList.class, Customer.class, Order.class, Product.class);

        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        httpClient.close();
        if(tmpOutputDir.exists()){
            FileUtils.deleteDirectory(tmpOutputDir);
        }
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{"src/test/resources/test.properties", "edu.si.sidora.mci"};
    }


    @Override
    protected JndiRegistry createRegistry() throws Exception {

        JndiRegistry reg = super.createRegistry();

        return reg;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Test
    public void testPostWithPayload() throws Exception {

        FedoraSettings fedoraSettings = new FedoraSettings(
                String.valueOf(config.getProperty("si.fedora.host")),
                String.valueOf(config.getProperty("si.fedora.user")),
                String.valueOf(config.getProperty("si.fedora.password"))
        );

        FedoraComponent fedora = new FedoraComponent();
        fedora.setSettings(fedoraSettings);

        log.info("\n=========================\n" +
                "si.fedora.host: {}\n" +
                "si.fedora.user: {}\n" +
                "si.fedora.password: {}\n" +
                "===========================",
                fedora.getSettings().getHost(),
                fedora.getSettings().getUsername(),
                fedora.getSettings().getPassword()
        );

        //Adding the Fedora Component to the the context using the setting above
        context.addComponent("fedora", fedora);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        context.getRouteDefinition("AddMCIProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        context.getRouteDefinition("MCICreateConcept").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("xsltMCIProjectToSIdoraProject").replace().toD("xslt:Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true");
                weaveById("velocityMCIResourceTemplate").replace().toD("velocity:Input/templates/MCIResourceTemplate.vsl");
                weaveById("velocityMCISidoraTemplate").replace().toD("velocity:Input/templates/MCISidoraTemplate.vsl");
                weaveById("xsltSIdoraConcept2DC").replace().toD("xslt:Input/xslt/SIdoraConcept2DC.xsl?saxon=true");
            }
        });

        HttpPost post = new HttpPost(BASE_URL + OWNERID + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(PAYLOAD));
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        //assertEquals(RESPONCE_PAYLOAD, responceBody);

        assertMockEndpointsSatisfied();
    }
}
