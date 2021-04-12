/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.sidora.tabular.metadata.rest;

import edu.si.sidora.tabular.metadata.excel2tabular.ExcelToTabular;
import edu.si.sidora.tabular.metadata.codebook.Codebook;
import edu.si.sidora.tabular.metadata.generator.TabularMetadataGenerator;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;

import static edu.si.sidora.tabular.metadata.codebook.Codebook.codebook;
import static org.apache.camel.Exchange.*;
import static org.apache.camel.LoggingLevel.INFO;

/**
 * @author jbirkhimer
 */
@Component
public class SidoraTabulatorMetadataRestRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.tablular.metadata")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.tablular.metadata");

    //The properties bean autoconfigured by application properties
    @Autowired
    ServerProperties serverProperties;

    /**
     * <b>Called on initialization to build the routes using the fluent builder syntax.</b>
     * <p/>
     * This is a central method for RouteBuilder implementations to implement the routes using the Java fluent builder
     * syntax.
     *
     * @throws Exception can be thrown during configuration
     */
    @Override
    public void configure() throws Exception {

        restConfiguration().apiContextRouteId("SidoraTabulatorRest")
                .component("servlet")
                .host("0.0.0.0")
                .port("{{server.port}}")
                .enableCORS(true)
//                .bindingMode(RestBindingMode.auto)
                .dataFormatProperty("prettyPrint", "true")
                // turn on swagger api-doc
                .apiContextPath("/api-doc")
                .apiContextRouteId("api-doc")
                .contextPath("/api")
                .apiProperty("api.title", "Sidora Tablulator API")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("cors", "true");

        rest("/api").description("Sidora Tablulator")
                .get("/codebook")
                .produces("text/xml")
                .description("add Tablulator project to sidora")

                .param().name("url").description("url of the csv file")
                    .required(true)
                    .type(RestParamType.query)
                    .endParam()
                .param().name("headers").description("are csv headers present")
                    .required(true)
                    .type(RestParamType.query)
                    .endParam()
                .param().name("scanLimit").description("row scan limit")
                    .required(false)
                    .defaultValue("100")
                    .dataType("integer")
                    .type(RestParamType.query)
                    .endParam()
                .responseMessage().code(200).message("OK").responseModel(Codebook.class).endResponseMessage()
                .route().routeId("getCodebook")
                    .onException(Exception.class)
                        .process(exchange -> {
                            Exception cause = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
                            String errorMsg = "Error while processing codebook: FailedBecause: " + cause.getMessage();
                            exchange.getIn().setBody(errorMsg);
                            exchange.getIn().setHeader(HTTP_SERVLET_RESPONSE, errorMsg);
                            exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 400);
                            exchange.getIn().setHeader(CONTENT_TYPE, "text/plain");
                        })
                        .handled(true)
                        .end()

                    .log(INFO, LOG_NAME, "${id} :: ${routeId} :: Start add MCI Project")

                    .removeHeaders("CamelHttp*")

                    .process(exchange -> {
                        Message out = exchange.getIn();
                        URL url = out.getHeader("url", URL.class);
                        Boolean hasHeaders = out.getHeader("headers", Boolean.class);
                        Integer scanLimit = out.getHeader("scanLimit", Integer.class);

                        Codebook answer = getCodebook(url, hasHeaders, scanLimit);
                        out.setBody(answer, String.class);
                    })
                    .log(INFO, LOG_NAME, "${id} :: ${routeId} :: MCI Project Submitted\n${body}")
                    .removeHeaders("*")
                .endRest();

    }

    private Codebook codebook;

    public Codebook getCodebook(URL url, boolean hasHeaders, int scanLimit) throws Exception {
        log.info("Parsing Started...");
        log.debug("QueryParams :: url: {}, hasHeaders: {}", url, hasHeaders);

        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        URL urlDecoded = new URL(uri.toASCIIString());
        String fileExt = urlDecoded.getFile().toLowerCase();

        TabularMetadataGenerator generator = new TabularMetadataGenerator();
        generator.setScanLimit(scanLimit);

        ExcelToTabular translator = new ExcelToTabular();

        if (!fileExt.endsWith(".csv") && !fileExt.endsWith(".xls") && !fileExt.endsWith(".xlsx")) {

            log.error("Parsing Failed! Invalid File Type '{}'!", fileExt);

            log.error("Parsing Finished with an Error...");

            throw new Exception("File '" + urlDecoded.getFile() + "' Not A Valid File Type!" );

        } else if (fileExt.endsWith(".xls") || fileExt.endsWith(".xlsx")) {

            log.info("Parsing Excel file {}", urlDecoded.getFile());

            URL xlsUrl = translator.process(urlDecoded).get(0).toURI().toURL();

            codebook = codebook(generator.getMetadata(xlsUrl, hasHeaders));

            log.info("Parsing Finished...");

            return codebook;
        }

        log.info("Parsing CSV file {}", urlDecoded.getFile());

        codebook = codebook(generator.getMetadata(urlDecoded, hasHeaders));

        log.info("Parsing Finished...");

        return codebook;
    }
}
