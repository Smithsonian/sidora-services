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

package edu.si.services.sidora.rest.batch;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Testing Different Variations for incrementing resource titles
 *
 *
 * @author jbirkhimer
 */
public class BatchTitleTest extends CamelTestSupport {

    //Temp directories created for testing
    private static File tempInputDirectory;

    /**
     * Testing title field with increment
     * @throws Exception
     */
    @Test
    public void titleNumberingTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/audio-mods.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/association.xml"));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(4);

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("mods", metadataXML);
        exchange.getIn().setHeader("associationXML", associationXML);
        exchange.getIn().setBody("0,1,2,3");

        template.send("direct:start", exchange);

        assertEquals("batch-audio-test(1)", mockEndpoint.getExchanges().get(0).getIn().getBody());
        assertEquals("batch-audio-test(2)", mockEndpoint.getExchanges().get(1).getIn().getBody());
        assertEquals("batch-audio-test(3)", mockEndpoint.getExchanges().get(2).getIn().getBody());
        assertEquals("batch-audio-test(4)", mockEndpoint.getExchanges().get(3).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing title field with increment on 2nd resource
     * @throws Exception
     */
    @Test
    public void titleNumbering2ndTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/audio-mods.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/association.xml"));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(4);

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("mods", metadataXML);
        exchange.getIn().setHeader("associationXML", associationXML);
        exchange.getIn().setBody("0,1,2,3");

        template.send("direct:start2", exchange);

        assertEquals("batch-audio-test", mockEndpoint.getExchanges().get(0).getIn().getBody());
        assertEquals("batch-audio-test(1)", mockEndpoint.getExchanges().get(1).getIn().getBody());
        assertEquals("batch-audio-test(2)", mockEndpoint.getExchanges().get(2).getIn().getBody());
        assertEquals("batch-audio-test(3)", mockEndpoint.getExchanges().get(3).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:start")
                        .setHeader("titlePath").xpath("//association/title_field/*[last()]", String.class, "associationXML")
                        .split().tokenize(",")
                        .setBody().simple("${header.mods}")
                        .setBody()
                        .xpath("concat(//*[local-name()=in:header('titlePath')]/text(), '(', function:simple('${property.CamelSplitIndex}++'), ')')", String.class)
                        .log(LoggingLevel.INFO, "Title = ${body}")
                        .to("mock:result");

                from("direct:start2")
                        .setHeader("titlePath").xpath("//association/title_field/*[last()]", String.class, "associationXML")
                        .split().tokenize(",")
                        .setBody().simple("${header.mods}")
                        .choice()
                            .when(simple("${property.CamelSplitIndex} == 0"))
                                .setBody()
                                .xpath("//*[local-name()=in:header('titlePath')]/text()", String.class)
                            .endChoice()
                            .otherwise()
                                .setBody()
                                .xpath("concat(//*[local-name()=in:header('titlePath')]/text(), function:simple('(${property.CamelSplitIndex})'))", String.class)
                        .end()
                        .log(LoggingLevel.INFO, "Title = ${body}")
                        .to("mock:result");
            }
        };
    }

    /**
     * Sets up the Temp directories used by the
     * route.
     * @throws IOException
     */
    @BeforeClass
    public static void setupSysPropsTempResourceDir() throws IOException {
        //Create and Copy the Input dir xslt, etc. files used in the route
        tempInputDirectory = new File("Input");
        if(!tempInputDirectory.exists()){
            tempInputDirectory.mkdir();
        }

        //The Location of the Input dir in the project
        File inputSrcDirLoc = new File("../Routes/Sidora-Batch/Karaf-config/Input");

        //Copy the Input src files so the camel route can find them
        FileUtils.copyDirectory(inputSrcDirLoc, tempInputDirectory);
    }

    /**
     * Clean up the temp directories after tests are finished
     * @throws IOException
     */
    @AfterClass
    public static void teardown() throws IOException {
        if(tempInputDirectory.exists()){
            FileUtils.deleteDirectory(tempInputDirectory);
        }
    }

}
