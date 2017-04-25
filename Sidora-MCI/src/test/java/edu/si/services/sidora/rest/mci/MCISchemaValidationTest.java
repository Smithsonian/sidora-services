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
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.File;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author jbirkhimer
 */
public class MCISchemaValidationTest extends CamelTestSupport {

    static private String LOG_NAME = "edu.si.mci";

    private static String sampleDataDir = "src/test/resources/sample-data/MCI_Inbox";

    @Test
    public void mciSchemaValidationTest() throws Exception {
        deleteDirectory("target/output");
        File path = new File(sampleDataDir);

        File [] files = path.listFiles();

        for (int i = 0; i < files.length; i++){
            if (files[i].isFile()){ //this line weeds out other directories/folders

                String mciProjectXML = readFileToString(files[i]);

                MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
                //mockEndpoint.expectedMessageCount(1);

                Exchange exchange = new DefaultExchange(context);
                exchange.getIn().setHeader("fileName", files[i].getName());
                exchange.getIn().setHeader("mciProjectXML", mciProjectXML);
                exchange.getIn().setBody(mciProjectXML);

                template.send("direct:start", exchange);

                assertMockEndpointsSatisfied();

                MockEndpoint.resetMocks(context);
            }
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                Namespaces ns = new Namespaces("ri", "http://www.w3.org/2005/sparql-results#");

                from("direct:start")
                        .doTry()
                        .log(LoggingLevel.INFO, LOG_NAME, "Testing File: ${header.fileName}")
                        .to("validator:file:target/test-classes/Input/schemas/MCIProjectSchema.xsd")
                        .setHeader("mciFolderHolder").xpath("//Fields/Field[@Name='Folder_x0020_Holder']/substring-after(., 'i:0#.w|us\\')", String.class, ns, "mciProjectXML")
                        .choice()
                            .when().simple("${header.mciFolderHolder} == null || ${header.mciFolderHolder} == ''")
                                .log(LoggingLevel.INFO, "Folder Holder NOT Found: ${header.mciFolderHolder} ]<<<<<<<<<<<<<<<<<<<<<")
                            .endChoice()
                            .otherwise()
                                .log(LoggingLevel.INFO, "Found MCI Folder Holder: ${header.mciFolderHolder}")
                        .end()
                        .endDoTry()
                        .doCatch(org.apache.camel.ValidationException.class)
                        .log(LoggingLevel.INFO, LOG_NAME, "Validation Failed: ${header.fileName} ]<<<<<<<<<<<<<<<<<<<<<")
                        .end()
                        .to("direct:transform");

                from("direct:transform")
                        .doTry()
                        .toD("xslt:file:target/test-classes/Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true").id("xsltMCIProjectToSIdoraProject")
                        .log(LoggingLevel.INFO, "Transform Successful")
                        .to("file:target/output/valid?fileName=${header.fileName}.xml")
                        .endDoTry()
                        .doCatch(net.sf.saxon.trans.XPathException.class)
                        .log(LoggingLevel.INFO, LOG_NAME, "Transform Failed: ${header.fileName} ]<<<<<<<<<<<<<<<<<<<<<")
                        .to("file:target/output/failed?fileName=${header.fileName}.xml")
                        .end()
                        .to("mock:result");
            }
        };
    }
}
