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

package edu.si.services.beans.cameratrap;

import edu.si.services.fedorarepo.FedoraComponent;
import edu.si.services.fedorarepo.FedoraSettings;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.test.junit4.CamelTestSupport;
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
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author jbirkhimer
 */
public class CT_AltIdTest extends CamelBlueprintTestSupport {

    static private String LOG_NAME = "edu.si.mci";

    private static String BASE_URL = null;
    private static String FUSEKI_PORT = null;
    private static String FEDORA_PORT = null;
    private static final String FUSEKI_BASE_URL = BASE_URL + ":9080/fuseki/fedora3?output=xml&query=";
    private static final String FEDORA_BASE_URL = BASE_URL + ":8080/fedora/objects/";

    //Default Test Params
    private static String PAYLOAD;
    //private static File TEST_XML = new File("src/test/resources/sample-data.xml");
    private static String RESPONCE_PAYLOAD;
    private static File TEST_RESPONCE_XML = new File("src/test/resources/sample-data.xml");

    private static File tmpOutputDir = new File("target/CT-Tests");
    private static Configuration config = null;
    private CloseableHttpClient httpClient;

    static {
        try {
            //PAYLOAD = FileUtils.readFileToString(TEST_XML);
            RESPONCE_PAYLOAD = FileUtils.readFileToString(TEST_RESPONCE_XML);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void ctProcessProjectAltIdTest() throws Exception {

        String ctTestManifest = FileUtils.readFileToString(new File("src/test/resources/AltIdSampleData/p117d22157/deployment_manifest.xml"));

        FedoraSettings fedoraSettings = new FedoraSettings(
                String.valueOf(config.getString("si.fedora.host")),
                String.valueOf(config.getString("si.fedora.user")),
                String.valueOf(config.getString("si.fedora.password"))
        );

        FedoraComponent fedora = new FedoraComponent();
        fedora.setSettings(fedoraSettings);

        //Adding the Fedora Component to the the context using the setting above
        context.addComponent("fedora", fedora);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        context.getRouteDefinition("UnifiedCameraTrapProcessProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        context.getRouteDefinition("UnifiedCameraTrapProcessProject").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToString(".*fedora:create.*").before().to("mock:result").stop();
                //weaveAddLast().to("mock:result");
            }
        });

        Exchange exchange = new DefaultExchange(context);

        //exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setHeader("CamelFedoraPid", config.getString("si.ct.root"));
        exchange.getIn().setHeader("CamelFileParent", "someCamelFileParent");
        exchange.getIn().setHeader("ManifestXML", ctTestManifest);

        template.send("direct:processProject", exchange);

        assertMockEndpointsSatisfied();

    }

    @Test
    public void fusekiQuery_Test() throws Exception {

        //String fusekiQuery = "ASK FROM <info:edu.si.fedora#ri>{<info:fedora/si-user:57> ?p ?o .}";

        String fusekiQuery = "PREFIX ct: <info:fedora/ct:>\n" +
                "PREFIX fedora-model: <info:fedora/fedora-system:def/model#>\n" +
                "\n" +
                "SELECT ?subject\n" +
                "FROM <info:edu.si.fedora#ri>\n" +
                "WHERE {\n" +
                "  ?subject fedora-model:hasModel <info:fedora/si:cameraTrapCModel> .\n" +
                "  FILTER CONTAINS (str(?subject), \"ct\")\n" +
                "}";


        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);

        //exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setBody(fusekiQuery);

        template.send("direct:fusekiQuery", exchange);

        assertMockEndpointsSatisfied();

        MockEndpoint.resetMocks(context);

    }

    @Test
    public void getManifestFromFedora_Test() throws Exception {

        FedoraSettings fedoraSettings = new FedoraSettings(
                String.valueOf(config.getString("si.fedora.host")),
                String.valueOf(config.getString("si.fedora.user")),
                String.valueOf(config.getString("si.fedora.password"))
        );

        FedoraComponent fedora = new FedoraComponent();
        fedora.setSettings(fedoraSettings);

        //Adding the Fedora Component to the the context using the setting above
        context.addComponent("fedora", fedora);
        //context.getComponent("sql", SqlComponent.class).setDataSource(DERBY_DB);

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

        HttpPost post = new HttpPost(BASE_URL + "/addProject");
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.setEntity(new StringEntity(PAYLOAD));
        HttpResponse response = httpClient.execute(post);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String responceBody = EntityUtils.toString(response.getEntity());

        //assertEquals(RESPONCE_PAYLOAD, responceBody);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{"src/test/resources/test.properties", "edu.si.sidora.karaf"};
    }


    @Override
    protected JndiRegistry createRegistry() throws Exception {

        JndiRegistry reg = super.createRegistry();

        return reg;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }


    @Before
    @Override
    public void setUp() throws Exception {
        System.setProperty("karaf.home", "target/test-classes");

        List<String> propFileList = Arrays.asList("target/test-classes/etc/edu.si.sidora.karaf.cfg", "target/test-classes/etc/system.properties");

        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(new File("src/test/resources/test.properties")));
        config = builder.getConfiguration();

        for (String propFile : propFileList) {

            FileBasedConfigurationBuilder<FileBasedConfiguration> builder2 =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.fileBased().setFile(new File(propFile)));

            for (Iterator<String> i = builder2.getConfiguration().getKeys(); i.hasNext();) {
                String key = i.next();
                Object value = builder2.getConfiguration().getProperty(key);
                if (!config.containsKey(key)) {
                    config.setProperty(key, value);
                }
            }
        }

        builder.save();
        super.setUp();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {

        Properties extra = new Properties();

        for (Iterator<String> i = config.getKeys(); i.hasNext();) {
            String key = i.next();
            Object value = config.getProperty(key);
            extra.setProperty(key, String.valueOf(value));
        }

        return extra;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if(tmpOutputDir.exists()){
            //FileUtils.deleteDirectory(tmpOutputDir);
        }
    }
}
