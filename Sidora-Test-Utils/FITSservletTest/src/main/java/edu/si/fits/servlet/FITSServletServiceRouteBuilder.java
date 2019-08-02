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

package edu.si.fits.servlet;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.ContentType;

import java.io.File;

/**
 * The FITS Servlet Service Test Route Builder contains the configuration needed to test the FIT service.
 *
 * @author davisda
 * @author jbirkhimer
 */
public class FITSServletServiceRouteBuilder extends RouteBuilder {

//    @PropertyInject(value = "si.ct.id")
//    static private String CT_LOG_NAME;

    /**
     * Configure the Camel routing rules for the FITS Servlet.
     */
    @Override
    public void configure() {

        from("direct:generateFITSReport")
                .log("Calling FITS")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                        //multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                        String filePath = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                        File uploadFile = new File(filePath);
                        //File file = exchange.getIn().getBody(File.class);
                        FileBody bin = new FileBody(uploadFile);
                        //multipartEntityBuilder.addPart("datafile", new FileBody(file, ContentType.MULTIPART_FORM_DATA, filename));
                        multipartEntityBuilder.addPart("datafile", bin);
                        exchange.getOut().setBody(multipartEntityBuilder.build());
                    }
                })
                .to("http4:{{si.fits.host}}/examine?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class, "UTF-8")
                .log(LoggingLevel.INFO, "FITS RESPONSE BODY ${body}").end();

        from("direct:getFITSVersion")
                .log("Calling FITS Version")
                .to("http4:{{si.fits.host}}/version?headerFilterStrategy=#dropHeadersStrategy");
                //.log(LoggingLevel.INFO, "FITS RESPONSE BODY ${body}");

    }
}
