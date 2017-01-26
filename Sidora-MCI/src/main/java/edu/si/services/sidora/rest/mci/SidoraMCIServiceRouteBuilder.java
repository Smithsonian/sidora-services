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
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.helpers.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author jbirkhimer
 */
public class SidoraMCIServiceRouteBuilder extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SidoraMCIServiceRouteBuilder.class);

    @PropertyInject(value = "edu.si.mci")
    static private String LOG_NAME;

    @Override
    public void configure() throws Exception {

        from("cxfrs://bean://rsServer?bindingStyle=SimpleConsumer")
                .routeId("SidoraMCIService")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting Sidora MCI Service Request for: ${header.operationName} ... ")
                .log(LoggingLevel.INFO, LOG_NAME, "===============================[ START ]==================================")
                .to("log:{{edu.si.mci}}?maxChars=100000&showAll=true&level=WARN")
                .log(LoggingLevel.INFO, LOG_NAME, "================================[ END ]===================================")
                .recipientList(simple("direct:${header.operationName}"))
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished Sidora MCI Service Request for: ${header.operationName} ... ");

        /**
         * Add Project XML from Payload
         */
        from("direct:addProject")
                .routeId("AddMCIProject")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Add MCI Project Concept...")
                .log(LoggingLevel.INFO, LOG_NAME, "===============================[ START AddMCIProject ]==================================")
                .to("log:{{edu.si.mci}}?maxChars=100000&showAll=true&level=WARN")
                .toD("xslt:Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true")
                .log(LoggingLevel.INFO, LOG_NAME, "================================[ END AddMCIProject ]===================================")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Add MCI Project Concept...");

        /**
         * Add Project XML From Multipart With Parameters And Payload
         */
        from("direct:addProjectMultipart")
                .routeId("AddMCIProjectMultipart")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Starting MCI Request - Add MCI Project Concept...")

                .log(LoggingLevel.INFO, LOG_NAME, "===============================[ START AddMCIProjectMultipart ]==================================")
                .to("log:{{edu.si.mci}}?maxChars=100000&showAll=true&level=WARN")


                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        LOG.info("==========[ Attachment Names ]==========\n{}", exchange.getIn().getAttachmentNames());

                        for (Map.Entry<String, DataHandler> entry : exchange.getIn().getAttachments().entrySet()) {
                            String name = entry.getKey().toString();
                            String contentType = entry.getValue().getContentType();
                            String value = IOUtils.toString(entry.getValue().getInputStream());
                            LOG.info("==========[ Attachment Info START ]==========\nName: {}\ncontentType: {}\nValue:{}\n==========[ Attachment Info END]==========\n", name, contentType, value);

                            //exchange.getOut().setHeader(name, value);
                            //exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                            //exchange.getOut().setBody(value);
                            //exchange.getIn().addAttachment(name + "TEST", new DataHandler(value.getBytes(), contentType));
                        }
                    }
                })
                .setBody().simple("${header.mciProject}")
                .log(LoggingLevel.INFO, LOG_NAME, "================================[ Body For XSLT ]===================================")
                .log(LoggingLevel.INFO, LOG_NAME, "${body}")
                .log(LoggingLevel.INFO, LOG_NAME, "================================[ Body For XSLT STOP ]===================================")
                .toD("xslt:Input/xslt/MCIProjectToSIdoraProject.xsl?saxon=true")
                .removeHeader("mciProject")
                .log(LoggingLevel.INFO, LOG_NAME, "================================[ END AddMCIProjectMultipart ]===================================")
                .log(LoggingLevel.INFO, LOG_NAME, "${id}: Finished MCI Request - Add MCI Project Concept...");


    }
}
