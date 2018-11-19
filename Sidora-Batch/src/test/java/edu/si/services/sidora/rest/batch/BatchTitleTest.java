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
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
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

/**
 * Testing incrementing resource titles
 *
 *
 * @author jbirkhimer
 */
public class BatchTitleTest extends CamelTestSupport {

    //Temp directories created for testing
    private static File tempInputDirectory;

    /**
     * Testing title field with increment for audio
     * @throws Exception
     */
    @Test
    public void audioTitleTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/audio/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/audio/association.xml"));

        runTitleTest(metadataXML, associationXML, "batch-audio-test");
    }

    /**
     * Testing title field with increment for codebook
     * @throws Exception
     */
    @Test
    public void codebookTitleTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/codebook/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/codebook/association.xml"));
        
        runTitleTest(metadataXML, associationXML, "batch-codebook-test");
    }

    /**
     * Testing title field with increment for image
     * @throws Exception
     */
    @Test
    public void imageTitleTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/image/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/image/association.xml"));

        runTitleTest(metadataXML, associationXML, "batch-image-test");
    }

    /**
     * Testing title field with increment for pdf
     * @throws Exception
     */
    @Test
    public void pdfTitleTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/pdf/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/pdf/association.xml"));

        runTitleTest(metadataXML, associationXML, "batch-pdf-test");
    }

    /**
     * Testing title field with increment for video
     * @throws Exception
     */
    @Test
    public void videoTitleTest() throws Exception {

        String metadataXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/video/metadata.xml"));
        String associationXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/video/association.xml"));

        runTitleTest(metadataXML, associationXML, "batch-video-test");
    }
    

    /**
     * Testing title field with increment
     * @throws Exception
     */
    public void runTitleTest(String metadataXML, String associationXML, String title) throws Exception {

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(4);

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("ds_metadataXML", metadataXML);
        exchange.getIn().setBody(associationXML);

        template.send("direct:start", exchange);

        assertEquals(title + "(1)", mockEndpoint.getExchanges().get(0).getIn().getHeader("titleLabel"));
        assertEquals(title + "(2)", mockEndpoint.getExchanges().get(1).getIn().getHeader("titleLabel"));
        assertEquals(title + "(3)", mockEndpoint.getExchanges().get(2).getIn().getHeader("titleLabel"));
        assertEquals(title + "(4)", mockEndpoint.getExchanges().get(3).getIn().getHeader("titleLabel"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testObjDsLabel() throws Exception {
        String resourceFileXML = FileUtils.readFileToString(new File("src/test/resources/test-data/batch-test-files/image/imageFiles.xml"));

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder("resourceFile", "image1.jpg","image2.jpg","image3.jpg");
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder("objDsLabel", "image1.jpg","image2.jpg","image3.jpg");
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder("primaryTitleLabel", "image1","image2","image3");

        template.sendBody("direct:objLabel", resourceFileXML);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                Namespaces ns = new Namespaces("ns1", "urn:shop");

                from("direct:start")
                        .to("xslt:file:Input/xslt/BatchAssociationTitlePath.xsl?saxon=true")
                        .setHeader("titlePath", simple("${body}"))
                        .setBody(simple("1,2,3,4"))
                        .split().tokenize(",")
                        .setBody(simple("${header.ds_metadataXML}", String.class))
                        .to("xslt:file:Input/xslt/BatchProcess_ManifestResource.xsl?saxon=true")
                        .to("bean:batchRequestControllerBean?method=setTitleLabel")
                        .log(LoggingLevel.INFO, "Metadata = ${body}")
                        .log(LoggingLevel.INFO, "TitlePath = ${header.titlePath}")
                        .log(LoggingLevel.INFO, "TitleLabel = ${header.titleLabel}")
                        .to("mock:result");

                from("direct:objLabel")
                        .log(LoggingLevel.INFO, "Start Body: ${body}")
                        .split().xtokenize("//file", 'w', ns)
                            .log(LoggingLevel.INFO, "Split Body: ${body}")
                            .setHeader("resourceFile").xpath("//file/text()", String.class)
                            .setHeader("objDsLabel").xpath("string(//file/@originalname)", String.class)
                            .to("bean:batchRequestControllerBean?method=setPrimaryTitleLabel")
                            .log(LoggingLevel.INFO, "resourceFile: ${header.resourceFile}, objDsLabel: ${header.objDsLabel}, primaryTitleLabel: ${header.primaryTitleLabel}")
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    Message out = exchange.getIn();
                                    log.info("stop");

                                }
                            })
                            .to("mock:result")
                        .end().id("splitEnd");
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

}
