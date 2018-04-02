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

package edu.si.sidora.taularmetadata.smx.blueprint;

import edu.si.codebook.Codebook;
import edu.si.sidora.tabularmetadata.TabularMetadataGenerator;
import edu.si.sidora.tabularmetadata.smx.blueprint.TabularMetadataGeneratorEndpoint;
import edu.si.sidora.tabularmetadata.smx.blueprint.TabularMetadataGeneratorEndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.IgnoreTextAndAttributeValuesDifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.*;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.bind.*;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static edu.si.codebook.Codebook.codebook;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author jbirkhimer
 */
public class TabularMetadataGeneratorEndpointTest {
    private static final Logger log = getLogger(TabularMetadataGeneratorEndpointTest.class);
    private static final String TABULARMETADATAGENERATORENDPOINT_TESTURL = "http://localhost:8282/codebook";
    private static Server server;
    private static String testDataShort, testDataLong;

    @BeforeClass
    public static void startServer() {
        TabularMetadataGeneratorEndpoint tabularMetadataGeneratorEndpoint = new TabularMetadataGeneratorEndpointImpl();

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress(TABULARMETADATAGENERATORENDPOINT_TESTURL);
        factory.setServiceBean(tabularMetadataGeneratorEndpoint);
        factory.setResourceClasses(TabularMetadataGeneratorEndpointImpl.class);
        factory.setResourceProvider(new SingletonResourceProvider(new TabularMetadataGeneratorEndpointImpl()));
        factory.setProvider(new JAXBElementProvider());

        server = factory.create();
        server.start();
    }

    @Before
    public void setUp() throws URISyntaxException {
        URL testDataShortResource = getClass().getResource("/testdata/Structural_Equation_Model_datashareSHORT.csv");
        URL testDataLongResource = getClass().getResource("/testdata/Structural_Equation_Model_datashareSHORT.csv");
        testDataShort = Paths.get(testDataShortResource.toURI()).toString();
        testDataLong = Paths.get(testDataLongResource.toURI()).toString();

        //Tell XML Unit to ignore whitespace between elements and within elements
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
    }

    @Test
    public void getRequestForCodebookShortCSVTest() throws Exception {
        File testDataFile = new File(testDataShort);
        URL url = new URL(TABULARMETADATAGENERATORENDPOINT_TESTURL + "/?url=file:" + testDataShort + "&headers=true&scanLimit=100");
        InputStream in = url.openStream();
        String codebookResult = getStringFromInputStream(in);
        System.out.println("Recieved HTTP GET request for codebook info:\n" + codebookResult);
        assertCorrectCodebook(codebookResult, testDataFile);
    }

    @Test
    public void getRequestForCodebookLongCSVTest() throws Exception {
        File testDataFile = new File(testDataShort);
        URL url = new URL(TABULARMETADATAGENERATORENDPOINT_TESTURL + "/?url=file:" + testDataLong + "&headers=true&scanLimit=100");
        InputStream in = url.openStream();
        String codebookResult = getStringFromInputStream(in);
        System.out.println("Recieved HTTP GET request for codebook info:\n" + codebookResult);
        assertCorrectCodebook(codebookResult, testDataFile);
    }

    private static String getStringFromInputStream(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c = 0;
        while ((c = in.read()) != -1) {
            bos.write(c);
        }
        in.close();
        bos.close();
        return bos.toString();
    }

    @Test
    public void webClientShortCSV_2_Test() throws IOException, JAXBException, SAXException {

        File testDataFile = new File(testDataShort);
        URL testDataURL = testDataFile.toURI().toURL();

        boolean hasHeaders = true;
        int scanLimit = 100;

        log.debug("CSV testdada URL Param:\n{}", testDataFile);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(TABULARMETADATAGENERATORENDPOINT_TESTURL)
                .queryParam("url", testDataURL)
                .queryParam("headers", hasHeaders)
                .queryParam("scanLimit", scanLimit);
        Invocation.Builder builder = target.request("text/xml");
        Response response = builder.get();

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatus());
        }

        String codebookResult = response.readEntity(String.class);

        log.info("WebClientTest Codebook Request Result:\n{}", codebookResult);

        assertCorrectCodebook(codebookResult, testDataFile);
    }

    @Test
    public void webClientShortCSVTest() throws IOException, JAXBException, SAXException {
        File testDataFile = new File(testDataShort);
        URL testDataURL = testDataFile.toURI().toURL();

        boolean hasHeaders = true;
        int scanLimit = 100;

        log.debug("CSV testdada URL Param:\n{}", testDataFile);

        WebClient client = WebClient.create(TABULARMETADATAGENERATORENDPOINT_TESTURL);
        client.accept("text/xml")
                .query("url", testDataURL)
                .query("headers", hasHeaders)
                .query("scanLimit", scanLimit);

        String codebookResult = client.get(String.class);

        log.info("WebClientTest Codebook Request Result:\n{}", codebookResult);

        assertCorrectCodebook(codebookResult, testDataFile);
    }

    @Test
    public void webClientLongCSVTest() throws IOException, JAXBException, SAXException {
        File testDataFile = new File(testDataLong);
        URL testDataURL = testDataFile.toURI().toURL();

        boolean hasHeaders = true;
        int scanLimit = 100;

        WebClient client = WebClient.create(TABULARMETADATAGENERATORENDPOINT_TESTURL);
        client.query("url", testDataURL).query("headers", hasHeaders).query("scanLimit", scanLimit);
        client.accept("text/xml");

        String codebookResult = client.get(String.class);

        log.info("TestWebClient Codebook Request Result:\n{}", codebookResult);
        assertCorrectCodebook(codebookResult, testDataFile);
    }

    //@Test
    public void webclientShortCSVTest() throws MalformedURLException {
        File testDataFile = new File(testDataShort);
        URL testDataURL = testDataFile.toURI().toURL();

        boolean hasHeaders = true;
        int scanLimit = 100;

        WebClient client = WebClient.create(TABULARMETADATAGENERATORENDPOINT_TESTURL);
        Codebook cb = client.accept("text/xml").query("url", testDataURL).query("headers", hasHeaders).query("scanLimit", scanLimit).get(Codebook.class);
    }

    //@Test
    public void clientFactoryShortCSVTest() throws Exception {
        File testDataFile = new File(testDataShort);
        URL testDataURL = testDataFile.toURI().toURL();

        boolean hasHeaders = true;
        int scanLimit = 100;

        List<Object> providers = new ArrayList<Object>();
        // add custom providers if any
        providers.add(new JAXBElementProvider<>());

        TabularMetadataGeneratorEndpoint tabularMetadataGeneratorEndpoint = JAXRSClientFactory.create(TABULARMETADATAGENERATORENDPOINT_TESTURL, TabularMetadataGeneratorEndpoint.class, providers);

        Codebook Response = tabularMetadataGeneratorEndpoint.getCodebook(testDataURL, hasHeaders, scanLimit);

    }

    //@Test
    public void testJAXBMarshallerUnmarshaller() throws JAXBException, IOException, URISyntaxException, ParserConfigurationException, SAXException, XMLStreamException {
        File testDataFile = new File(testDataShort);
        URL testDataURL = testDataFile.toURI().toURL();

        boolean hasHeaders = true;
        int scanLimit = 100;

        TabularMetadataGeneratorEndpoint tabularMetadataGeneratorEndpoint = new TabularMetadataGeneratorEndpointImpl();

        JAXBContext jaxbContext = JAXBContext.newInstance(Codebook.class);

        /**
         * Marshalling
         *
         */
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter stringWriter = new StringWriter();

        jaxbMarshaller.marshal(tabularMetadataGeneratorEndpoint.getCodebook(testDataURL, hasHeaders, scanLimit), stringWriter);
        log.info("XML Result:\n{}", stringWriter.toString());


        /**
         * Unmarshalling
         *
         */
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        jaxbUnmarshaller.setEventHandler(new DefaultValidationEventHandler());

        //Unmarshalling from a StAX XMLStreamReader
        XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(stringWriter.toString()));
        //Codebook xmlSRcodebook = (Codebook) jaxbUnmarshaller.unmarshal(xmlStreamReader);

        //Unmarshalling from a StringBuffer using a javax.xml.transform.stream.StreamSource
        //Codebook codebookResponse = (Codebook) jaxbUnmarshaller.unmarshal(new StringReader(stringWriter.toString()));

        //Unmarshalling from a File
        URL codebookesource = getClass().getResource("/testdata/expectedCodebook.xml");
        String codebookesourceFile = Paths.get(codebookesource.toURI()).toString();
        log.info("Codebook URL: {}", codebookesourceFile);
        //Codebook codebookResponse = (Codebook) jaxbUnmarshaller.unmarshal(new File(codebookesourceFile));

        //Unmarshalling from a org.w3c.dom.Node:
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        //Document doc = db.parse(new File(codebookesourceFile));
        //Codebook cbr = (Codebook) jaxbUnmarshaller.unmarshal(doc);

        UnmarshallerHandler unmarshallerHandler = jaxbUnmarshaller.getUnmarshallerHandler();

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware( true );

        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        xmlReader.setContentHandler(unmarshallerHandler);
        xmlReader.parse(new InputSource(new FileInputStream(codebookesourceFile)));
        Codebook myCodeBookObject= (Codebook)unmarshallerHandler.getResult();
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }


    private void assertCorrectCodebook(String codebookResult, File testDataFile) throws IOException, JAXBException, SAXException {
        Assert.assertNotNull(codebookResult);

        //Create expected codebook
        TabularMetadataGenerator generator = new TabularMetadataGenerator();
        generator.setScanLimit(100);

        URL testDataURL = testDataFile.toURI().toURL();

        //Codebook expectedCodebook = codebook(generator.getMetadata(testDataURL, true));

        StringWriter expectedCodebook = new StringWriter();

        JAXBContext jaxbContext = JAXBContext.newInstance(Codebook.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        //jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(codebook(generator.getMetadata(testDataURL, true)), expectedCodebook);

        log.info("Expected Codebook:\n{}", expectedCodebook.toString());

        //assertEquals(codebookResult, expectedCodebook.toString());

        Diff myDiff = new Diff(codebookResult, expectedCodebook.toString());
        myDiff.overrideDifferenceListener(new IgnoreTextAndAttributeValuesDifferenceListener());


        DetailedDiff detmyDiff = new DetailedDiff(myDiff);
        detmyDiff.overrideDifferenceListener(new IgnoreTextAndAttributeValuesDifferenceListener());

        List allDifferences = detmyDiff.getAllDifferences();
        //assertEquals("Difference", 2, allDifferences.size());

        assertTrue("XML similar " + detmyDiff.toString(), detmyDiff.similar());
        assertTrue("XML identical " + myDiff.toString(), myDiff.identical());
        

    }
}
