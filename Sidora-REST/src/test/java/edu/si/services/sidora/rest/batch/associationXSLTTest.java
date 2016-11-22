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

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jbirkhimer
 */
public class associationXSLTTest extends CamelTestSupport {

    //Camel Headers Map
    private Map<String, Object> headers;

    //Temp directories created for testing
    private static File tempInputDirectory;

    //private Namespaces ns = new Namespaces("mods", "http://www.loc.gov/mods/v3");

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
        File inputSrcDirLoc = new File("../Routes/Camera Trap/Input");

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

    /**
     * Testing getting title field path from association xml, update the mods title, and create dc from mods
     * @throws Exception
     */
    @Test
    public void associationTitleFieldPathTest() throws Exception {

        String associationXML = FileUtils.readFileToString(new File("src/test/resources/association.xml"));
        String associationXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/batch_association_title_field.xsl"));
        String mods_to_dcXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/mods_to_dc.xsl"));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        template.sendBody("direct:titleFieldPath", associationXML);

        assertEquals("/mods/titleInfo/title",mockEndpoint.getExchanges().get(0).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing updating title field from path created from association xml
     * @throws Exception
     */
    @Test
    public void updateModsTitleTest() throws Exception {

        String associationXML = FileUtils.readFileToString(new File("src/test/resources/association.xml"));
        String modsXML = FileUtils.readFileToString(new File("src/test/resources/mods.xml"));
        String associationXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/batch_association_title_field.xsl"));
        String mods_to_dcXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/mods_to_dc.xsl"));

        headers = new HashMap<>();
        headers.put("titlePath", "/mods/titleInfo/title");
        //headers.put("titleLabel", "test(123)");
        headers.put("camelSplitIndex", 0);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        template.sendBodyAndHeaders("direct:updateModsTitleField", modsXML, headers);

        assertEquals("pdf_batch_111416(0)", mockEndpoint.getExchanges().get(0).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing updating title field from path created from association xml
     * @throws Exception
     */
    @Test
    public void mods_to_dcTest() throws Exception {

        String modsXML = FileUtils.readFileToString(new File("src/test/resources/mods2.xml"));
        //String mods_to_dcXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/mods_to_dc.xsl"));

        String mods_to_dc_XSL = "http://sidora0c.myquotient.net/~ramlani/sidora/sidora0.4/sites/all/modules/islandora_xml_forms-7.x-1.7/builder/transforms/mods_to_dc.xsl";

        headers = new HashMap<>();
        headers.put("transform", mods_to_dc_XSL);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        template.sendBodyAndHeaders("direct:mods_to_dc", modsXML, headers);

        //assertEquals("test(123)", mockEndpoint.getExchanges().get(0).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing updating title field from path created from association xml
     * @throws Exception
     */
    @Test
    public void mods_to_dc_StrippedTest() throws Exception {

        String modsXML = FileUtils.readFileToString(new File("src/test/resources/mods2.xml"));
        //String mods_to_dcXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/mods_to_dc.xsl"));

        String mods_to_dc_XSL = "http://sidora0c.myquotient.net/~ramlani/sidora/sidora0.4/sites/all/modules/islandora_xml_forms-7.x-1.7/builder/transforms/mods_to_dc.xsl";
        String stripModsXSL = "http://sidora0c.myquotient.net/~ramlani/sidora/sidora0.4/sites/all/modules/islandora_xml_forms-7.x-1.7/builder/xml/extract_mods_from_collection.xsl";

        headers = new HashMap<>();
        headers.put("transform", mods_to_dc_XSL);
        headers.put("stripMods", stripModsXSL);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        template.sendBodyAndHeaders("direct:mods_to_dc_stripped", modsXML, headers);

        //assertEquals("test(123)", mockEndpoint.getExchanges().get(0).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    /**
     * Testing updating title field from path created from association xml
     * @throws Exception
     */
    @Test
    public void mods_to_dc_remove_identifier_Test() throws Exception {

        String modsXML = FileUtils.readFileToString(new File("src/test/resources/mods2.xml"));
        //String mods_to_dcXSLT = FileUtils.readFileToString(new File("src/test/resources/xslt/mods_to_dc.xsl"));

        String mods_to_dc_XSL = "http://sidora0c.myquotient.net/~ramlani/sidora/sidora0.4/sites/all/modules/islandora_xml_forms-7.x-1.7/builder/transforms/mods_to_dc.xsl";

        headers = new HashMap<>();
        headers.put("transform", mods_to_dc_XSL);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        template.sendBodyAndHeaders("direct:mods_to_dc_remove_identifier", modsXML, headers);

        //assertEquals("test(123)", mockEndpoint.getExchanges().get(0).getIn().getBody());

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:titleFieldPath")
                        .to("xslt:file:src/test/resources/xslt/batch_association_title_field.xsl?saxon=true")
                        .to("mock:result");

                from("direct:updateModsTitleField")
                        .to("xslt:file:Input/xslt/BatchProcess_ManifestResource.xsl?saxon=true")
                        .setBody(xpath("//*[local-name()='title']/text()", String.class))
                        .to("mock:result");

                from("direct:mods_to_dc")
                        /*.toD("xslt:${header.transform}?saxon=true")*/
                        .toD("xslt:${header.transform}?allowStAX=true")
                        .log(LoggingLevel.INFO, "Result:\n${body}")
                        .to("mock:result");

                from("direct:mods_to_dc_stripped")
                        /*.toD("xslt:${header.transform}?saxon=true")*/
                        .toD("xslt:${header.stripMods}?allowStAX=true")
                        .toD("xslt:${header.transform}?allowStAX=true")
                        .log(LoggingLevel.INFO, "Result:\n${body}")
                        .to("mock:result");

                from("direct:mods_to_dc_remove_identifier")
                        .toD("xslt:${header.transform}?saxon=true")
                        /*.setBody(xpath("not(self::*[local-name() = 'identifier'])"))*/
                        .log(LoggingLevel.INFO, "Result:\n${body}")
                        .to("mock:result");
            }
        };
    }
}
