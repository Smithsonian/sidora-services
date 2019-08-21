/*
 * Copyright (c) 2015-2019 Smithsonian Institution.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.You may obtain a copy of
 *  the License at: http://www.apache.org/licenses/
 *
 *  This software and accompanying documentation is supplied without
 *  warranty of any kind. The copyright holder and the Smithsonian Institution:
 *  (1) expressly disclaim any warranties, express or implied, including but not
 *  limited to any implied warranties of merchantability, fitness for a
 *  particular purpose, title or non-infringement; (2) do not assume any legal
 *  liability or responsibility for the accuracy, completeness, or usefulness of
 *  the software; (3) do not represent that use of the software would not
 *  infringe privately owned rights; (4) do not warrant that the software
 *  is error-free or will be maintained, supported, updated or enhanced;
 *  (5) will not be liable for any indirect, incidental, consequential special
 *  or punitive damages of any kind or nature, including but not limited to lost
 *  profits or loss of data, on any basis arising from contract, tort or
 *  otherwise, even if any of the parties has been warned of the possibility of
 *  such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 *  license terms. For a complete copy of all copyright and license terms, including
 *  those of third-party libraries, please see the product release notes.
 */

package edu.si.services.beans.fitsservlet.itest;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

/**
 * @author jbirkhimer
 * @author davisda
 */
public class FITSServlet_IT extends CamelBlueprintTestSupport {

    private static final String KARAF_HOME = System.getProperty("karaf.home");
    private static Properties extra = new Properties();
    private static String TEST_FILENAME = KARAF_HOME + "/BBB_9425.NEF";

    protected static String FITS_URI;

    private static final Logger logger = LoggerFactory.getLogger(FITSServlet_IT.class);

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/fits-servlet-test-route.xml";
    }

    @Override
    public void setUp() throws Exception {
        //System.getProperties().list(System.out);
        log.info("===================[ KARAF_HOME = {} ]===================", System.getProperty("karaf.home"));

        List<String> propFileList = loadAdditionalPropertyFiles();
        if (loadAdditionalPropertyFiles() != null) {
            for (String propFile : propFileList) {
                Properties extra = new Properties();
                try {
                    extra.load(new FileInputStream(propFile));
                    this.extra.putAll(extra);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Map.Entry<Object, Object> p : System.getProperties().entrySet()) {
            if (extra.containsKey(p.getKey())) {
                extra.setProperty(p.getKey().toString(), p.getValue().toString());
            }
        }

        FITS_URI = extra.getProperty("si.fits.host");

        super.setUp();
    }

    protected List<String> loadAdditionalPropertyFiles() {
        return Arrays.asList(KARAF_HOME + "/etc/edu.si.sidora.fits.cfg");
    }

    @Override
    protected String setConfigAdminInitialConfiguration(Properties configAdmin) {
        configAdmin.putAll(extra);
        return "edu.si.sidora.karaf";
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void fitsVersionTest() {
        logger.info("FITS_URI = {}", FITS_URI);

        Exchange exchange = new DefaultExchange(context);
        template.send("direct:getFITSVersion", exchange);
        String fitsVersion = exchange.getOut().getBody(String.class);
        logger.info("Found FITS Version:{}", fitsVersion.trim());
        String expectedVersion = "1.2.0";
        assertEquals(expectedVersion, fitsVersion.trim());
    }

    @Test
    public void fitsOutputTest() throws IOException, XpathException, SAXException {
        log.debug("FITS_URI = {}", FITS_URI);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader(Exchange.FILE_NAME, TEST_FILENAME);
        template.send("direct:getFITSReport", exchange);

        String fitsOutput = exchange.getIn().getBody(String.class);
        logger.info("FITS Response: {}", fitsOutput);

        Map nsMap = new HashMap();
        nsMap.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(nsMap));

        assertXpathEvaluatesTo("image/x-nikon-nef", "/fits:fits/fits:identification/fits:identity[1]/@mimetype", fitsOutput.trim());
    }

    /**
     * Testing AddFITSDataStream Route
     *
     * @throws Exception
     */
    @Test
    public void fitsRouteTest() throws Exception {

        // The mock endpoint we are sending to for assertions.
        MockEndpoint mockResult = getMockEndpoint("mock:mockResult");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("dsMIME", "image/x-nikon-nef");

        // AdviceWith the routes as needed for this test.
        context.getRouteDefinition("AddFITSDataStream").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("fedora:addDatastream.*").skipSendToOriginalEndpoint().log(LoggingLevel.INFO, "Skipping add datastream to Fedora").to("mock:mockResult");
            }
        });

        context.start();

        // Initialize the exchange with body and headers as needed.
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader(Exchange.FILE_NAME, TEST_FILENAME);
        exchange.getIn().setHeader("CamelFedoraPid", "test:0001");

        // The endpoint we want to start from with the exchange body and headers we want
        template.send("direct:addFITSDataStream", exchange);

        // Check the result
        Message result = mockResult.getExchanges().get(0).getIn();
        assertEquals("image/x-nikon-nef", result.getHeader("dsMIME"));
        assertMockEndpointsSatisfied();
    }
}
