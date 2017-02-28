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

import com.amazonaws.services.dynamodbv2.xspec.L;
import edu.si.services.fedorarepo.FedoraComponent;
import edu.si.services.fedorarepo.FedoraSettings;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

/**
 * @author jbirkhimer
 */
public class CT_AltIdTest extends CamelTestSupport {

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
                String.valueOf(config.getProperty("si.fedora.host")),
                String.valueOf(config.getProperty("si.fedora.user")),
                String.valueOf(config.getProperty("si.fedora.password"))
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
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:fusekiQuery")
                        .setBody().groovy("URLEncoder.encode(request.getBody(String.class));")
                        .setHeader("CamelHttpMethod", constant("GET"))
                        .toD(FUSEKI_BASE_URL + "${body}")
                        .convertBodyTo(String.class, "UTF-8")
                        //.log(LoggingLevel.INFO, LOG_NAME, "===============[ Fuseki BODY ]================\n${body}")
                        .to("direct:getManifest")
                        .to("mock:result");


                from("direct:getManifest")
                        .split().tokenizeXML("uri")
                            .setHeader("pid").xpath("//uri/substring-after(., 'info:fedora/')", String.class)
                            .setHeader(Exchange.FILE_NAME).simple("${header.pid.replaceAll(\":\", \"-\")}.xml")
                            .toD(FEDORA_BASE_URL + "${header.pid}" + "/datastreams/MANIFEST/content")
                            .convertBodyTo(String.class, "UTF-8")
                            .to("file:target/Inbox")
                            .to("direct:getIDs")
                        .log(LoggingLevel.INFO, LOG_NAME, "=================\n${body}\n===================")
                        .end();


                from("direct:getIDs")
                        //ProcessParents
                        .setHeader("ProjectName").xpath("//ProjectName", String.class)
                        .setHeader("ProjectId").xpath("//ProjectId", String.class)

                        .filter().xpath("boolean(//SubProjectName/text()[1])")
                            .setHeader("SubProjectName").xpath("//SubProjectName", String.class)
                            .setHeader("SubProjectId").xpath("//SubProjectId", String.class)
                        .end()

                        .filter().xpath(" boolean(//PlotName/text()[1])")
                            .setHeader("PlotName").xpath("concat(//SubProjectName/text(), ':', //PlotName/text())", String.class)
                        .end()

                        //ProcessSite
                        .setHeader("CameraSiteName").xpath("//CameraSiteName", String.class)
                        .setHeader("CameraDeploymentID").xpath("//CameraDeploymentID", String.class)

                        .setBody()
                        .simple("==============================================\n"
                                + "ProjectName:${header.ProjectName}\n"
                                + "ProjectId:${header.ProjectId}\n"
                                + "SubProjectName:${header.SubProjectName}\n"
                                + "SubProjectId:${header.SubProjectId}\n"
                                + "PlotName:${header.PlotName}\n"
                                + "CameraSiteName:${header.CameraSiteName}\n"
                                + "CameraDeploymentID:${header.CameraDeploymentID}\n"
                                + "==============================================\n");
                        //.log(LoggingLevel.INFO, LOG_NAME, "=================\n${headers}\n===================");


            }
        };
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {

        JndiRegistry reg = super.createRegistry();

        return reg;
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

        /*FileBasedConfigurationBuilder<FileBasedConfiguration> builder2 =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(new File("src/test/resources/other.properties"))
                        );*/

        try {
            config = builder.getConfiguration();
            //config.setProperty("server.address", BASE_URL);
            BASE_URL = String.valueOf(config.getProperty("server.address"));
            FUSEKI_PORT = String.valueOf(config.getProperty("fuseki.port"));
            FEDORA_PORT = String.valueOf(config.getProperty("fedora.port"));

            /*for (Iterator<String> i = builder2.getConfiguration().getKeys(); i.hasNext();) {
                String key = i.next();
                Object value = builder2.getConfiguration().getProperty(key);
                config.setProperty(key, value);
            }*/

            builder.save();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
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
