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

package edu.si.services.sidora.rest.batch.rest;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

import static org.apache.camel.Exchange.*;
import static org.apache.camel.LoggingLevel.INFO;

/**
 * @author jbirkhimer
 */
@Component
public class SidoraBatchRestRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.batch")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.batch");

    @PropertyInject(value = "swagger.test.endpoints.enabled", defaultValue = "false")
    private String swagger_test_endpoints_enabled;

    //The properties bean autoconfigured by application properties
    @Autowired
    ServerProperties serverProperties;

    /**
     * <b>Called on initialization to build the routes using the fluent builder
     * syntax.</b>
     * <p/>
     * This is a central method for RouteBuilder implementations to implement
     * the routes using the Java fluent builder syntax.
     *
     * @throws Exception can be thrown during configuration
     */
    @Override
    public void configure() throws Exception {
        getContext().setStreamCaching(true);

        restConfiguration().apiContextRouteId("SidoraBatchRest")
                .component("servlet")
//                .componentProperty("attachmentMultipartBinding", "true")
//                .componentProperty("disableStreamCache", "true")
                .host("0.0.0.0")
                .port("{{server.port}}")
                .enableCORS(true)
                //.bindingMode(RestBindingMode.auto)
                .dataFormatProperty("prettyPrint", "true")
                // turn on swagger api-doc
                .apiContextPath("/api-doc")
                .apiContextRouteId("api-doc")
                .contextPath("/api")
                .apiProperty("api.title", "Sidora Batch API")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("cors", "true");

        rest("/sidora/batch").description("Sidora Batch")
                .post("/addResourceObjects/{parentId}")
                    //.consumes(MediaType.APPLICATION_FORM_URLENCODED)
                    .consumes("application/x-www-form-urlencoded")
                    .produces("application/xml")
                    .description("Batch Add Resources")
                    .param().name("parentId")
                        .description("The parent object pid for the resources")
                        .required(true)
                        .type(RestParamType.path)
                        .endParam()
                    .param().name("codebookPID")
                        .description("pid of codebook")
                        .required(false)
                        .type(RestParamType.query)
                        .endParam()
                    .param().name("resourceFileList")
                        .description("url of xml file containing the list of resources")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .endParam()
                    .param().name("ds_metadata")
                        .description("url of the MODS datastream")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .endParam()
                    .param().name("ds_sidora")
                        .description("url of the SIDORA datastream")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .endParam()
                    .param().name("association")
                        .description("url of the association xml file")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .endParam()
                    .param().name("resourceOwner")
                        .description("Resource Owner")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .endParam()
                .route().id("addResourceObjectsRest").noStreamCaching()
                    .onException(Exception.class)
                        .process(exchange -> {
                            Exception cause = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
                            String errorMsg = "Error while processing... FailedBecause: " + cause.getMessage();
                            exchange.getIn().setBody(errorMsg);
                            exchange.getIn().setHeader(HTTP_SERVLET_RESPONSE, errorMsg);
                            exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 500);
                            exchange.getIn().setHeader(CONTENT_TYPE, "text/plain");
                        })
                        .logExhaustedMessageHistory(true)
                        .logStackTrace(true)
                        .logExhaustedMessageBody(true)
                        .handled(true)
                    .end()
                    .to("direct:addResourceObjects")
                .endRest()

                .post("/addResourceObjects/test/{parentId}").apiDocs(Boolean.parseBoolean(swagger_test_endpoints_enabled))
                    //.consumes(MediaType.APPLICATION_FORM_URLENCODED)
                    .consumes("application/x-www-form-urlencoded")
                    .produces("application/xml")
                    .description("Testing Batch Add Resources")
                    .param().name("parentId")
                        .description("The parent object pid for the resources")
                        .required(true)
                        .type(RestParamType.path)
                        .defaultValue("si-user:196")
                        .endParam()
                    .param().name("codebookPID")
                        .description("pid of codebook")
                        .required(false)
                        .type(RestParamType.query)
                        .defaultValue("ct:0002")
                        .endParam()
                    .param().name("resourceFileList")
                        .description("url of xml file containing the list of resources")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .example("http://localhost:{{server.port}}/test-data/audio/audioFiles.xml")
                        .defaultValue("http://localhost:{{server.port}}/test-data/audio/audioFiles.xml")
                        .endParam()
                    .param().name("ds_metadata")
                        .description("url of the MODS datastream")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .example("http://localhost:{{server.port}}/test-data/audio/metadata.xml")
                        .defaultValue("http://localhost:{{server.port}}/test-data/audio/metadata.xml")
                        .endParam()
                    .param().name("ds_sidora")
                        .description("url of the SIDORA datastream")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .example("http://localhost:{{server.port}}/test-data/audio/sidora.xml")
                        .defaultValue("http://localhost:{{server.port}}/test-data/audio/sidora.xml")
                        .endParam()
                    .param().name("association")
                        .description("url of the association xml file")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .example("http://localhost:{{server.port}}/test-data/audio/association.xml")
                        .defaultValue("http://localhost:{{server.port}}/test-data/audio/association.xml")
                        .endParam()
                    .param().name("resourceOwner")
                        .description("Resource Owner")
                        .type(RestParamType.formData)
                        .dataType("string")
                        .required(true)
                        .defaultValue("camelWBUser")
                        .endParam()
                .route().id("addResourceObjectsTestRest").noStreamCaching()
                    .onException(Exception.class)
                        .process(exchange -> {
                            Exception cause = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
                            String errorMsg = "Error while processing... FailedBecause: " + cause.getMessage();
                            exchange.getIn().setBody(errorMsg);
                            exchange.getIn().setHeader(HTTP_SERVLET_RESPONSE, errorMsg);
                            exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 500);
                            exchange.getIn().setHeader(CONTENT_TYPE, "text/plain");
                        })
                        .logExhaustedMessageHistory(true)
                        .logStackTrace(true)
                        .logExhaustedMessageBody(true)
                        .handled(true)
                    .end()
                    .to("direct:addResourceObjects")
                .endRest()

                .post("/requestStatus/{parentId}/{correlationId}").description("Get the status of a batch job")
                    .produces("application/xml")
                    .param().name("parentId").description("The parent object pid for the resources")
                        .required(true)
                        .type(RestParamType.path)
                        .endParam()
                    .param().name("correlationId").description("")
                        .required(true)
                        .type(RestParamType.path)
                        .endParam()
                    .route().id("requestStatusRest").noStreamCaching()
                        .onException(Exception.class)
                            .process(exchange -> {
                                Exception cause = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
                                String parentId = exchange.getIn().getHeader("parentId", String.class);
                                String errorMsg = "Error while processing parentId: " + parentId + ", FailedBecause: " + cause.getMessage();
                                exchange.getIn().setBody(errorMsg);
                                exchange.getIn().setHeader(HTTP_SERVLET_RESPONSE, errorMsg);
                                exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 500);
                                exchange.getIn().setHeader(CONTENT_TYPE, "text/plain");
                            })
                            .handled(true)
                        .end()
                        .log(INFO, LOG_NAME, "${id}: Starting SidoraRESTService Request for: ${routeId} ... ")
                        .to("direct:requestStatus")
                        .log(INFO, LOG_NAME, "${id}: Finished SidoraRESTService Request for: ${routeId} ... ")
                .endRest();


    }
}
