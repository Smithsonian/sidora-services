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

import edu.si.services.beans.fitsservlet.FITSServletRouteBuilder;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author jbirkhimer
 * @author davisda
 */
@CamelSpringBootTest
@SpringBootTest(classes = CamelAutoConfiguration.class, properties = {"fits.host=http://localhost:9180/fits"})
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@DisableJmx
@ActiveProfiles("test")
public class FITSServlet_IT {

    private static final Logger log = LoggerFactory.getLogger(FITSServlet_IT.class);

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    private static String TEST_FILENAME = "src/test/resources/BBB_9425.NEF";

    @Value(value = "${fits.host}")
    private String FITS_URI;

    @BeforeEach
    public void dropHeadersStrategy() throws Exception {
        DefaultHeaderFilterStrategy dropHeadersStrategy = new DefaultHeaderFilterStrategy();
        dropHeadersStrategy.setOutFilterPattern(".*");
        context.getRegistry().bind("dropHeadersStrategy", dropHeadersStrategy);
        context.addRoutes(new FITSServletRouteBuilder());
    }

    @Test
    public void fitsVersionTest() {
        log.info("FITS_URI = {}", FITS_URI);

        Exchange exchange = new DefaultExchange(context);
        template.send("direct:getFITSVersion", exchange);
        String fitsVersion = exchange.getOut().getBody(String.class);
        log.info("Found FITS Version:{}", fitsVersion.trim());
        String expectedVersion = "1.5.0";
        assertEquals(expectedVersion, fitsVersion.trim());
    }

    @Test
    public void fitsOutputTest() throws IOException, XpathException, SAXException {
        log.debug("FITS_URI = {}", FITS_URI);

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader(Exchange.FILE_NAME, TEST_FILENAME);
        template.send("direct:getFITSReport", exchange);

        String fitsOutput = exchange.getIn().getBody(String.class);
        log.info("FITS Response: {}", fitsOutput);

        Map nsMap = new HashMap();
        nsMap.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(nsMap));

        assertXpathEvaluatesTo("image/x-nikon-nef", "/fits:fits/fits:identification/fits:identity[1]/@mimetype", fitsOutput.trim());
    }
}
