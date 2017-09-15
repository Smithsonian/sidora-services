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

package edu.si.services.fits.itest;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

/**
 * @author jbirkhimer
 */
public class UCT_FITS_IT extends CamelBlueprintTestSupport {

    private String defaultTestProperties = "src/test/resources/test.properties";
    private static final File testFile = new File("src/test/resources/BBB_9425.NEF");
    protected static final String FITS_URI = System.getProperty("si.fits.host");

    private static CloseableHttpClient httpClient;

    private static final Logger logger = LoggerFactory.getLogger(UCT_FITS_IT.class);

    @Override
    protected String getBlueprintDescriptor() {
        return "Routes/unified-camera-trap-route.xml";
    }

    @Override
    protected String[] loadConfigAdminConfigurationFile() {
        return new String[]{defaultTestProperties, "edu.si.sidora.karaf"};
    }

    @Override
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        services.put("amazonS3Client", asService(new AmazonS3ClientMock("some_key", "some_secret_key"), null));
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @BeforeClass
    public static void createHttpClient() throws IOException {
        httpClient = HttpClientBuilder.create().build();
    }

    @AfterClass
    public static void cleanUpHttpClient() throws IOException {
        httpClient.close();
    }

    @Test
    public void fitsVersionTest() throws IOException {
        log.debug("FITS_URI = {}", FITS_URI);

        HttpGet request = new HttpGet(FITS_URI + "/version");
        final String fitsVersion;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            fitsVersion = EntityUtils.toString(response.getEntity());
        }
        logger.info("Found FITS Version:{}", fitsVersion);
        String expectedVersion = "1.0.4";
        assertEquals(expectedVersion, fitsVersion.trim());
    }

    @Test
    public void fitsOutputTest() throws IOException, XpathException, SAXException {
        log.debug("FITS_URI = {}", FITS_URI);

        log.info("fits test URL = {}", FITS_URI + "/examine?file="+ testFile.getAbsolutePath());
        HttpGet request = new HttpGet(FITS_URI + "/examine?file="+ testFile.getAbsolutePath());
        final String fitsOutput;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            fitsOutput = EntityUtils.toString(response.getEntity());
        }
        logger.info("FITS Response: {}", fitsOutput);

        Map nsMap = new HashMap();
        nsMap.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(nsMap));

        assertXpathEvaluatesTo("image/x-nikon-nef", "/fits:fits/fits:identification/fits:identity[1]/@mimetype", fitsOutput.trim());
    }

    /**
     * Testing UnifiedCameraTrapAddFITSDataStream Route
     *
     * @throws Exception
     */
    @Test
    public void fitsCameraTrapRouteTest() throws Exception {

        //The mock endpoint we are sending to for assertions
        MockEndpoint mockResult = getMockEndpoint("mock:mockResult");
        mockResult.expectedMessageCount(1);

        /* Advicewith the routes as needed for this test */
        context.getRouteDefinition("UnifiedCameraTrapAddFITSDataStream").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("fedora:addDatastream.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping add datastream to Fedora").to("mock:mockResult");
            }
        });

        context.start();

        //Initialize the exchange with body and headers as needed
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelFileNameProduced", testFile.getName());
        exchange.getIn().setHeader("CamelFedoraPid", "test:0001");

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:addFITSDataStream", exchange);

        Message result = mockResult.getExchanges().get(0).getIn();

        //Project
        assertEquals("image/x-nikon-nef", result.getHeader("dsMIME"));

        assertMockEndpointsSatisfied();
    }
}
