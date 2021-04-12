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

package edu.si.services.sidora.cameratrap.itest;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.LogDefinition;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
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
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.auth.AuthScope.ANY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing Project and Subproject findObject logic using alternate id's to check if an object exists
 * and falling back to checking using string name. The test assumes neither Project or SubProject exist
 * and will continue to create the object, However actual object creation is intercepted or sent to mock endpoints.
 *
 * @author jbirkhimer
 */
@Disabled("Ignore this test b/c it needs fedora and fuseki running and have the correct object in the repo")
@CamelSpringBootTest
@SpringBootTest(properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false",
        "camel.springboot.java-routes-exclude-pattern=UnifiedCameraTrapInFlightConceptStatusPolling,UnifiedCameraTrapStartProcessing"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UCT_AltId_IT {

    private static final Logger log = LoggerFactory.getLogger(UCT_AltId_IT.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    private final File testManifest = new File("src/test/resources/AltIdSampleData/Unified/deployment_manifest.xml");
    private final File projectRELS_EXT = new File("src/test/resources/AltIdSampleData/Unified/projectRELS-EXT.rdf");
    private final File subProjectRELS_EXT = new File("src/test/resources/AltIdSampleData/Unified/subProjectRELS-EXT.rdf");
    private final File objectNotFoundFusekiResponse = new File("src/test/resources/AltIdSampleData/objectNotFoundFusekiResponse.xml");

    @Value(value = "si.fedora.host")
    protected String FEDORA_URI;

    @Value(value = "si.fedora.user")
    protected String FEDORA_USER;
    @Value(value = "si.fedora.password")
    protected String FEDORA_PASSWORD;

    @Value(value = "si.fuseki.endpoint")
    protected String FUSEKI_URI;

    @Value(value = "fits.host")
    protected String FITS_URI;

    @Value(value = "si.ct.root")
    protected String CT_ROOT_PID;

    private final String FOXML = "foxml";

    private CloseableHttpClient httpClient;

    @BeforeEach
    public void loadObjectsIntoFedora() throws IOException {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(ANY, new UsernamePasswordCredentials(FEDORA_USER, FEDORA_PASSWORD));
        httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        ingest("test:123456", Paths.get(FOXML, "eMammal.xml"));
    }

    private void ingest(String pid, Path payload) throws IOException {
        if (!checkObjecsExist(pid)) {
            HttpPost ingest = new HttpPost(FEDORA_URI + "/objects/" + pid + "?format=info:fedora/fedora-system:FOXML-1.1");
            ingest.setEntity(new ByteArrayEntity(Files.readAllBytes(payload)));
            ingest.setHeader("Content-type", MediaType.TEXT_XML);
            try (CloseableHttpResponse pidRes = httpClient.execute(ingest)) {
                assertEquals(SC_CREATED, pidRes.getStatusLine().getStatusCode(), "Failed to ingest " + pid + "!");
                log.info("Ingested test object {}", EntityUtils.toString(pidRes.getEntity()));
            }
        }
    }

    private boolean checkObjecsExist(String pid) throws IOException {
        String query = URLEncoder.encode("ASK FROM <info:edu.si.fedora#ri> WHERE { <info:fedora/" + pid + "> ?p ?o}", "UTF-8");

        HttpGet request = new HttpGet(FUSEKI_URI + "?output=xml&query=" + query);
        final boolean exists;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String pidExists = EntityUtils.toString(response.getEntity()).split("<boolean>")[1].split("</boolean>")[0];
            exists = Boolean.parseBoolean(pidExists);
            log.info("PID = {}, Exists = {}", pid, exists);
        }

        return exists;
    }

    @AfterEach
    public void cleanUpHttpClient() throws IOException {
        httpClient.close();
    }

    @Test
//    @Disabled
    public void testFedora() throws IOException {
        HttpGet request = new HttpGet(FEDORA_URI + "/objects/si:121909/datastreams/DC/content?format=xml");
        final String xml;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            xml = EntityUtils.toString(response.getEntity());
        }
        log.info("Found DC datastream profile:\n{}", xml);
        String expectedProfile = "\n<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                "  <dc:title>eMammal</dc:title>\n" +
                "  <dc:title>eMammal</dc:title>\n" +
                "  <dc:description></dc:description>\n" +
                "  <dc:identifier>test:123456</dc:identifier>\n" +
                "  <dc:relation></dc:relation>\n" +
                "</oai_dc:dc>\n";
        assertEquals(expectedProfile, xml);
    }

    @Test
//    @Disabled
    public void testFuseki() throws IOException {
        String parentPid = "test:123456";

        String testQuery = URLEncoder.encode("ASK FROM <info:edu.si.fedora#ri> { <info:fedora/" + parentPid + "> ?p ?o .}", "UTF-8");

        log.info("testQuery={}", testQuery);

        HttpGet request = new HttpGet(FUSEKI_URI + "?output=xml&query=" + testQuery);
        final String xml;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            xml = EntityUtils.toString(response.getEntity());
        }
        log.info("Found datastream profile:\n{}", xml);
        String expectedProfile = "<?xml version=\"1.0\"?>\n" +
                "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <boolean>true</boolean>\n" +
                "</sparql>\n";
        assertEquals(expectedProfile, xml);
    }

    /*@Test
    public void testFits() throws IOException {
        HttpGet request = new HttpGet(FITS_URI + "/version");
        final String xml;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            xml = EntityUtils.toString(response.getEntity());
        }
        log.info("Found FITS Version:{}", xml);
        String expectedVersion = "1.0.4";
        Assertions.assertEquals(expectedVersion, xml);
    }*/

    /**
     * Testing Project and Subproject datastreams are created and the DC has the alternate ID. The test assumes neither Project or SubProject exist
     * and will continue to create the object after findObject retries for both altId and name have been exhausted.
     *
     * Project and Subproject creation basic workflow:
     *    - findObject using AltId if not found and retries are exhausted continue
     *    - findObject using String Name if not found and retries are exhausted continue
     *    - create fedora object RELS-EXT, EAC-CPF, parent/child relationship, and add the altId to the DC datastream
     *
     * @throws Exception
     */
    @Test
    public void processParentsAltIDTest() throws Exception {

        //The mock endpoint we are sending to for assertions
        MockEndpoint mockResult = context.getEndpoint("mock:mockResult", MockEndpoint.class);
        mockResult.expectedMessageCount(4);

        MockEndpoint mockAltIDResult = context.getEndpoint("mock:altIdResult", MockEndpoint.class);
        mockAltIDResult.expectedMessageCount(16);

        MockEndpoint mockNameResult = context.getEndpoint("mock:nameResult", MockEndpoint.class);
        mockNameResult.expectedMessageCount(16);

        /* Advicewith the routes as needed for this test */
        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessParents", false, a ->{
                a.interceptSendToEndpoint("direct:processPlot").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping processPlot");

                //Grab the Datastreams from fedora and send then to the mock endpoint for assertion
                a.weaveByType(LogDefinition.class).selectLast().after()
                        //Project Datastreams
                        .toD("fcrepo:objects/${headers.ProjectPID}/datastreams?format=xml").id("mockResult[0]")
                        .log("Project Datastreams=\n${body}")
                        .convertBodyTo(String.class)
                        .to("mock:mockResult")
                        .toD("fcrepo:objects/${headers.ProjectPID}/datastreams/DC/content?format=xml").id("mockResult[1]")
                        .convertBodyTo(String.class)
                        .log("Project DC=\n${body}")
                        .to("mock:mockResult")

                        //SubProject datastreams
                        .toD("fcrepo:objects/${headers.SubProjectPID}/datastreams?format=xml").id("mockResult[2]")
                        .log("Subproject Datastreams=\n${body}")
                        .convertBodyTo(String.class)
                        .to("mock:mockResult")
                        .toD("fcrepo:objects/${headers.SubProjectPID}/datastreams/DC/content?format=xml").id("mockResult[3]")
                        .convertBodyTo(String.class)
                        .log("Subproject DC=\n${body}")
                        .to("mock:mockResult");
        });

        AdviceWith.adviceWith(context, "UnifiedCameraTrapFindObject", false, a ->{
                a.weaveById("whenFindByAltId").after().to("mock:altIdResult");
                a.weaveById("whenFindByName").after().to("mock:nameResult");
        });

        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessProject", false, a ->
                a.interceptSendToEndpoint("direct:workbenchReloadPid").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping Workbench HTTP Call"));

        AdviceWith.adviceWith(context, "UnifiedCameraTrapProcessSubproject", false, a ->
                a.interceptSendToEndpoint("direct:workbenchReloadPid").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping Workbench HTTP Call"));

        context.start();

        //Initialize the exchange with body and headers as needed
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("ManifestXML", readFileToString(testManifest, "utf-8"));
        exchange.getIn().setHeader("deploymentId", "somedeploymentId");
        exchange.getIn().setHeader("CamelFedoraPid", CT_ROOT_PID);

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:processParents", exchange);

        //Delay test so we can open fedora admin to look at the objects
        //Thread.sleep(300000);

        mockResult.assertIsSatisfied();
        mockAltIDResult.assertIsSatisfied();
        mockNameResult.assertIsSatisfied();

        List<String> resultList = new ArrayList<String>();
        for (Exchange resultExchange : mockResult.getExchanges()) {
            resultList.add(resultExchange.getIn().getBody(String.class));
        }

        //Assert Project Datastreams
        assertNotNull("The Project Datastreams does not exist!", resultList.get(0));
        assertTrue(resultList.get(0).contains("dsid=\"DC\""), "The Project DC does not exist!");
        assertNotNull(resultList.get(0).contains("dsid=\"RELS-EXT\""), "The Project RELS-EXT does not exist!");
        assertTrue(resultList.get(0).contains("dsid=\"EAC-CPF\""), "The Project EAC-CPF does not exist!");

        //Assert Subproject Datastreams
        assertNotNull("The Subproject Datastreams does not exist!", resultList.get(2));
        assertTrue(resultList.get(2).contains("dsid=\"DC\""), "The Subproject DC does not exist!");
        assertNotNull(resultList.get(3).contains("dsid=\"RELS-EXT\""), "The Subproject RELS-EXT does not exist!");
        assertTrue(resultList.get(2).contains("dsid=\"EAC-CPF\""), "The Subproject EAC-CPF does not exist!");


        //Assert Project DC contains the PID and altId
        assertNotNull(resultList.get(1).split("<dc:identifier>")[1].split("</dc:identifier>")[0]);
        assertEquals("testProjectId0001", resultList.get(1).split("<dc:identifier>")[2].split("</dc:identifier>")[0]);

        //Assert Subproject DC contains the PID and altId
        assertNotNull(resultList.get(3).split("<dc:identifier>")[1].split("</dc:identifier>")[0]);
        assertEquals("testSubProjectId0001", resultList.get(3).split("<dc:identifier>")[2].split("</dc:identifier>")[0]);
    }
}
