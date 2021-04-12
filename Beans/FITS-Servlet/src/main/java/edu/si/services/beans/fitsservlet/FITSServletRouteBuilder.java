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

package edu.si.services.beans.fitsservlet;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.springframework.stereotype.Component;

import java.io.File;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.FILE_NAME_PRODUCED;

/**
 * The FITS Servlet Route Builder contains the configuration needed to execute FITS as a web service.
 *
 * @author davisda
 * @author jbirkhimer
 */
@Component
public class FITSServletRouteBuilder extends RouteBuilder {

    static private String LOG_NAME = "edu.si.fits";

    /**
     * Configure the Camel routing rules for the FITS Servlet.
     */
    @Override
    public void configure() {

        from("direct:getFITSReport").routeId("getFITSReport")
                .onException(Exception.class)
                        .logExhaustedMessageBody(true)
                    .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} FITS Report: Starting processing ...").id("FITSReportLogStart")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                        String filePath = null;
                        if (exchange.getIn().getHeader(Exchange.FILE_NAME, String.class) != null) {
                            filePath = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                        } else if (exchange.getIn().getHeader(Exchange.FILE_NAME_PRODUCED, String.class) != null) {
                            filePath = exchange.getIn().getHeader(Exchange.FILE_NAME_PRODUCED, String.class);
                        } else {
                            throw new FITSServletException(FILE_NAME + " or " + FILE_NAME_PRODUCED + " is null!");
                        }
                        File uploadFile = new File(filePath);
                        FileBody bin = new FileBody(uploadFile);
                        multipartEntityBuilder.addPart("datafile", bin);
                        exchange.getIn().setBody(multipartEntityBuilder.build());
                    }
                })
                .toD("log:" + LOG_NAME + "?level=DEBUG&showHeaders=true")
                .setHeader(Exchange.HTTP_URI).simple("{{fits.host}}/examine")
                .to("http://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class, "UTF-8")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} FITS Report: Finished processing.")
                .end();

        from("direct:getFITSVersion").routeId("getFITSVersion")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} FITS Version: Starting processing ...").id("FITSVersionLogStart")
                .setHeader(Exchange.HTTP_URI).simple("{{fits.host}}/version")
                .to("http://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy");

    }
}
