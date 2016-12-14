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
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/** Integration Tests from association and metadata to DC
 * @author jbirkhimer
 */
public class Metadata2DCTest extends CamelTestSupport {

    //Camel Headers Map
    private Map<String, Object> headers;

    //Temp directories created for testing
    private static File tempInputDirectory;

    /**
     * Testing metadata to dc transforms for audio
     * @throws Exception
     */
    @Test
    public void audioDCTest() throws Exception {

        String metadata_to_dc_XSL = "xslt/mods_to_dc.xsl";

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/audio/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/audio/association.xml"));

        String expectedDC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<oai_dc:dc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "           xmlns:srw_dc=\"info:srw/schema/1/dc-schema\"\n" +
                "           xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                "   <dc:title>batch-audio-test(1)</dc:title>\n" +
                "   <dc:description>batch ingest</dc:description>\n" +
                "</oai_dc:dc>\n";

        runDCTest(metadataXML, associationXML, metadata_to_dc_XSL, expectedDC);
    }

    /**
     * Testing metadata to dc transforms for codebook
     * @throws Exception
     */
    @Test
    public void codebookDCTest() throws Exception {

        String metadata_to_dc_XSL = "xslt/SIdoraConcept2DC.xsl";

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/codebook/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/codebook/association.xml"));

        String expectedDC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\"><dc:title>batch-codebook-test(1)</dc:title><dc:type>Tabular Data Object</dc:type></oai_dc:dc>";

        runDCTest(metadataXML, associationXML, metadata_to_dc_XSL, expectedDC);
    }

    /**
     * Testing metadata to dc transforms for image
     * @throws Exception
     */
    @Test
    public void imageDCTest() throws Exception {

        String metadata_to_dc_XSL = "xslt/mods_to_dc.xsl";

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/image/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/image/association.xml"));

        String expectedDC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<oai_dc:dc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "           xmlns:srw_dc=\"info:srw/schema/1/dc-schema\"\n" +
                "           xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                "   <dc:title>batch-image-test(1)</dc:title>\n" +
                "   <dc:description>jjjjj</dc:description>\n" +
                "   <dc:subject>ttt1--tt2</dc:subject>\n" +
                "</oai_dc:dc>\n";

        runDCTest(metadataXML, associationXML, metadata_to_dc_XSL, expectedDC);
    }

    /**
     * Testing metadata to dc transforms for pdf
     * @throws Exception
     */
    @Test
    public void pdfDCTest() throws Exception {

        String metadata_to_dc_XSL = "xslt/mods_to_dc.xsl";

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/pdf/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/pdf/association.xml"));

        String expectedDC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<oai_dc:dc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "           xmlns:srw_dc=\"info:srw/schema/1/dc-schema\"\n" +
                "           xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                "   <dc:title>batch-pdf-test(1)</dc:title>\n" +
                "   <dc:contributor>corporate (artist)</dc:contributor>\n" +
                "   <dc:creator>personal</dc:creator>\n" +
                "   <dc:type>Text</dc:type>\n" +
                "   <dc:format>text</dc:format>\n" +
                "   <dc:subject/>\n" +
                "   <dc:coverage>North America</dc:coverage>\n" +
                "</oai_dc:dc>\n";

        runDCTest(metadataXML, associationXML, metadata_to_dc_XSL, expectedDC);
    }

    /**
     * Testing metadata to dc transforms for video
     * @throws Exception
     */
    @Test
    public void videoDCTest() throws Exception {

        String metadata_to_dc_XSL = "xslt/mods_to_dc.xsl";

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/video/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/video/association.xml"));

        String expectedDC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<oai_dc:dc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "           xmlns:srw_dc=\"info:srw/schema/1/dc-schema\"\n" +
                "           xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                "           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                "   <dc:title>batch-video-test(1): 1 videos</dc:title>\n" +
                "   <dc:type>MovingImage</dc:type>\n" +
                "   <dc:format>videorecording</dc:format>\n" +
                "   <dc:subject/>\n" +
                "   <dc:coverage>North America</dc:coverage>\n" +
                "</oai_dc:dc>\n";

        runDCTest(metadataXML, associationXML, metadata_to_dc_XSL, expectedDC);
    }

    /**
     * Metadata to dc transform test
     * @throws Exception
     */
    public void runDCTest(String metadataXML, String associationXML, String metadata_to_dc_XSL, String expectedDC) throws Exception {

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("ds_metadataXML", metadataXML);
        exchange.getIn().setHeader("metadata_to_dc_XSL", metadata_to_dc_XSL);
        exchange.setProperty("CamelSplitIndex", 0);
        exchange.getIn().setBody(associationXML);

        template.send("direct:Start", exchange);

        //template.sendBodyAndHeaders("direct:metadata_to_dc", metadataXML, headers);

        assertEquals(expectedDC, mockEndpoint.getExchanges().get(0).getIn().getBody(String.class));

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:start")
                        .to("xslt:file:Input/xslt/BatchAssociationTitlePath.xsl?saxon=true")
                        .setHeader("titlePath", simple("${body}"))
                        .setBody(simple("${header.ds_metadataXML}", String.class))
                        .to("xslt:file:Input/xslt/BatchProcess_ManifestResource.xsl?saxon=true")
                        .to("bean:batchRequestControllerBean?method=setTitleLabel")
                        //.log(LoggingLevel.INFO, "XSLT Title Transform:\n${body}")
                        .toD("xslt:{{extract.mods.from.collection.xsl}}")
                        .log(LoggingLevel.INFO, "Extract Transform:\n${body}")
                        .toD("xslt:${header.metadata_to_dc_XSL}?saxon=true")
                        .log(LoggingLevel.INFO, "Mods to DC Transform:\n${body}")
                        .log(LoggingLevel.INFO, "TitlePath = ${header.titlePath}")
                        .log(LoggingLevel.INFO, "TitleLabel = ${header.titleLabel}")
                        .to("mock:result");
            }
        };
    }

    /**
     * Registering the batchRequestControllerBean
     *
     * @return JndiRegistry
     * @throws Exception
     */
    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("batchRequestControllerBean", edu.si.services.sidora.rest.batch.beans.BatchRequestControllerBean.class);

        return jndi;
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

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties extra = new Properties();
        extra.put("extract.mods.from.collection.xsl", "xslt/extract_mods_from_collection.xsl");
        return extra;
    }
}
