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

package edu.si.services.sidora.rest.mci;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;

/**
 * @author jbirkhimer
 */
public class MCI_TransformTest extends CamelTestSupport {

    static private String LOG_NAME = "edu.si.mci";

    private static String mciXSL = "Input/xslt/MCIProjectToSIdoraProject.xsl";
    private static String sampleDataDir = "src/test/resources/sample-data/MCI_Inbox";
    private static File tmpOutputDir = new File("target/transform_results");

    @Test
    public void single_mci_transformTest() throws Exception {
        //File mciTestFile = new File(sampleDataDir + "/BAD-XML-ID-si-fedoratest-si-edu-35125-1485459442803-12-15.xml");
        File mciTestFile = new File(sampleDataDir + "/ID-si-fedoratest-si-edu-35125-1485459442803-12-19.xml");

        String mciProjectXML = FileUtils.readFileToString(mciTestFile);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);

        exchange.getIn().setHeader("mciXSL", mciXSL);
        exchange.getIn().setHeader("CamelXsltFileName", tmpOutputDir + "/" + FilenameUtils.getBaseName(mciTestFile.getName()) + "-transform.xml");
        exchange.getIn().setBody(mciProjectXML);

        template.send("direct:start", exchange);

        assertMockEndpointsSatisfied();

        MockEndpoint.resetMocks(context);

    }

    @Test
    public void multiple_mci_transformTest() throws Exception {
        File path = new File(sampleDataDir);

        File [] files = path.listFiles();

        for (int i = 0; i < files.length; i++){
            if (files[i].isFile()){ //this line weeds out other directories/folders

                String mciProjectXML = FileUtils.readFileToString(files[i]);

                MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
                mockEndpoint.expectedMessageCount(1);

                Exchange exchange = new DefaultExchange(context);

                exchange.getIn().setHeader("mciXSL", mciXSL);
                //exchange.getIn().setHeader("CamelXsltFileName", tmpOutputDir + "/" + FilenameUtils.getBaseName(files[i].getName()) + "-transform.xml");
                exchange.getIn().setBody(mciProjectXML);

                template.send("direct:start", exchange);

                assertMockEndpointsSatisfied();

                MockEndpoint.resetMocks(context);


            }
        }

    }

    @Test
    public void mciCreateConceptTest() throws Exception {

        File mciTestFile = new File(sampleDataDir + "/ID-si-fedoratest-si-edu-35125-1485459442803-12-19.xml");

        String mciProjectXML = FileUtils.readFileToString(mciTestFile);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);

        //exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setBody(mciProjectXML);

        template.send("direct:mciCreateConcept", exchange);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void getParentIdTest() throws Exception {

        File mciTestFile = new File(sampleDataDir + "/ID-si-fedoratest-si-edu-35125-1485459442803-12-19.xml");

        String mciProjectXML = FileUtils.readFileToString(mciTestFile);

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);

        //exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setBody(mciProjectXML);

        template.send("direct:getParentId", exchange);

        assertMockEndpointsSatisfied();

        MockEndpoint.resetMocks(context);

    }

    @Test
    public void fusekiQueryTest() throws Exception {

        String fusekiQuery = "ASK FROM <info:edu.si.fedora#ri>{<info:fedora/si-user:57> ?p ?o .}";

        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);

        Exchange exchange = new DefaultExchange(context);

        //exchange.getIn().setHeader("CamelHttpMethod", "GET");
        exchange.getIn().setBody(fusekiQuery);

        template.send("direct:fusekiQuery", exchange);

        assertMockEndpointsSatisfied();

        MockEndpoint.resetMocks(context);

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                /*from("direct:start")
                        .doTry()
                        .toD("xslt:${header.mciXSL}?output=file")
                        .log(LoggingLevel.INFO, "Transform Successful for ${header.CamelXsltFileName}")
                        .log(LoggingLevel.INFO, "===============[ BODY ] =================\n${body}")
                        //.to("mock:result")
                        .doCatch(net.sf.saxon.trans.XPathException.class)
                        .log(LoggingLevel.ERROR, "Exception Caught for file ${header.CamelXsltFileName}: ${property.CamelExceptionCaught} ")
                        .end()
                .to("mock:result");*/

                from("direct:start")
                        .routeId("AddMCIProject")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Add MCI Project Concept...")

                        //Stash Incoming Payload to File
                        .setHeader("fileName", simple("${id}"))
                        .toD("file:target/MCI_Inbox?fileName=${header.fileName}.xml")
                        .log(LoggingLevel.INFO, LOG_NAME, "Payload Received - Saved to File: ${header.fileName}.xml")

                        //Create the
                        .doTry()
                        .setHeader("CamelXsltFileName", simple("target/MCI_Inbox/${header.fileName}-transform.xml"))
                        .toD("xslt:Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true&output=file")
                        .log(LoggingLevel.INFO, "Transform Successful for ${header.CamelXsltFileName}.xml")
                        .doCatch(net.sf.saxon.trans.XPathException.class)
                        .log(LoggingLevel.ERROR, "Transform Failed for file ${header.fileName} :: Exception Caught: ${property.CamelExceptionCaught} ")
                        .setBody().simple("Transform Failed for file ${header.fileName} :: Exception Caught: ${property.CamelExceptionCaught} ")
                        .end()
                        .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Add MCI Project Concept...")
                .to("mock:result");

                from("direct:fusekiQuery")
                        .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                        .log(LoggingLevel.INFO, LOG_NAME, "===============[ BODY ]================\n${body}")
                        .setHeader("CamelHttpMethod", constant("GET"))
                        .toD("http://si-fedoratest.si.edu:9080/fuseki/fedora3?output=xml&${body}")
                        .log(LoggingLevel.INFO, LOG_NAME, "===============[ BODY ]================\n${body}")
                        .to("mock:result");

                from("direct:getParentId")
                        //.setBody().xpath("//Fields/Field[@Name='Folder_x0020_Holder']/substring-after(., 'i:0#.w|us\\')", String.class)
                        .setHeader("mciProjectXML", simple("${body}", String.class))
                        .setBody().xpath("//Fields/Field[@Name='Folder_x0020_Holder']/substring-after(., 'i:0#.w|us\\')", String.class)
                        .setHeader("mciOwner").simple("${body}")
                        .log(LoggingLevel.INFO, LOG_NAME, "==========[ mciOwner:${header.mciOwner} ]==========\n${body}")
                        .to("mock:result");

                from("direct:mciCreateConcept")
                        .routeId("MCICreateConcept")
                        .doTry()
                            .toD("xslt:Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true")
                            .log(LoggingLevel.INFO, "Transform Successful")
                            .setHeader("mciDESCMETA", simple("${body}", String.class))
                            .setHeader("mciLable").xpath("/SIdoraConcept/primaryTitle/titleText/text()", String.class, "mciDESCMETA")
                        .log(LoggingLevel.INFO, "===================[ mciLabel: ${header.mciLabel} ]======================")
                        .endDoTry()
                        .doCatch(net.sf.saxon.trans.XPathException.class)
                            .log(LoggingLevel.ERROR, "MCIProjectToSIdoraProject Transform Failed :: Exception Caught: ${property.CamelExceptionCaught}")
                        .end()
                .to("mock:result");

            }
        };
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if(tmpOutputDir.exists()){
            //FileUtils.deleteDirectory(tmpOutputDir);
        }

    }
}
