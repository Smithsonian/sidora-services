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
 * @author jbirkhimer
 */
public class associationXSLTTest extends CamelTestSupport {

    //Camel Headers Map
    private Map<String, Object> headers;

    //Temp directories created for testing
    private static File tempInputDirectory;

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

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties extra = new Properties();
        extra.put("extract.mods.from.collection.xsl", "xslt/extract_mods_from_collection.xsl");
        return extra;
    }

    /**
     * Testing getting the path from association xml and updating mods title field with increment
     * @throws Exception
     */
    @Test
    public void modsTitleTransformTest() throws Exception {

        String modsXML = FileUtils.readFileToString(new File("src/test/resources/test-data/audio-mods.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/association.xml"));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        Exchange exchange = new DefaultExchange(context);

        exchange.setProperty("CamelSplitIndex", 0);
        exchange.getIn().setHeader("associationXML", associationXML);
        exchange.getIn().setBody(modsXML, String.class);

        template.send("direct:modsTitleTransformTest", exchange);

        assertEquals("batch-audio-test(0)", mockEndpoint.getExchanges().get(0).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing metadata to dc transforms
     * @throws Exception
     */
    @Test
    public void mods_to_dcTest() throws Exception {

        String modsXML = FileUtils.readFileToString(new File("src/test/resources/test-data/audio-mods.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/association.xml"));
        String mods_to_dc_XSL = "xslt/mods_to_dc.xsl";

        String expectedResultBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<oai_dc:dc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "           xmlns:srw_dc=\"info:srw/schema/1/dc-schema\"\n" +
                "           xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                "   <dc:title>batch-audio-test(0)</dc:title>\n" +
                "   <dc:description>batch ingest</dc:description>\n" +
                "</oai_dc:dc>\n";

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        Exchange exchange = new DefaultExchange(context);

        exchange.setProperty("CamelSplitIndex", 0);
        exchange.getIn().setHeader("mods_to_dc_XSL", mods_to_dc_XSL);
        exchange.getIn().setHeader("associationXML", associationXML);
        exchange.getIn().setBody(modsXML, String.class);

        template.send("direct:mods_to_dc", exchange);

        //template.sendBodyAndHeaders("direct:mods_to_dc", modsXML, headers);

        assertEquals(expectedResultBody, mockEndpoint.getExchanges().get(0).getIn().getBody(String.class));

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:modsTitleTransformTest")
                        .setHeader("titlePath").xpath("//association/title_field/*[last()]", String.class, "associationXML")
                        .setHeader("titleLabel").xpath("concat(//*[local-name()=in:header('titlePath')]/text(), function:simple('(${property.CamelSplitIndex})'))", String.class)
                        .to("xslt:file:Input/xslt/BatchProcess_ManifestResource.xsl?saxon=true")
                        .log(LoggingLevel.INFO, "Result:\n${body}")
                        .setBody(xpath("//*[local-name()=in:header('titlePath')]/text()", String.class))
                        .to("mock:result");

                from("direct:mods_to_dc")
                        .setHeader("titlePath").xpath("//association/title_field/*[last()]", String.class, "associationXML")
                        .setHeader("titleLabel").xpath("concat(//*[local-name()=in:header('titlePath')]/text(), function:simple('(${property.CamelSplitIndex})'))", String.class)
                        .to("xslt:file:Input/xslt/BatchProcess_ManifestResource.xsl?saxon=true")
                        .log(LoggingLevel.INFO, "XSLT Title Transform:\n${body}")
                        .toD("xslt:{{extract.mods.from.collection.xsl}}")
                        .log(LoggingLevel.INFO, "Extract Transform:\n${body}")
                        .toD("xslt:${header.mods_to_dc_XSL}?saxon=true")
                        .log(LoggingLevel.INFO, "Mods to DC Transform:\n${body}")
                        .to("mock:result");
            }
        };
    }
}
