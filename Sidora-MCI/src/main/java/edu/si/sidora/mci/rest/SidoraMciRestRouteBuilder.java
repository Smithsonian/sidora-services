package edu.si.sidora.mci.rest;

import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;

import static org.apache.camel.LoggingLevel.INFO;

/**
 * @author jbirkhimer
 */
@Component
public class SidoraMciRestRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.mci")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.mci");

    //The properties bean autoconfigured by application properties
    @Autowired
    ServerProperties serverProperties;

    @Override
    public void configure() throws Exception {

        restConfiguration().apiContextRouteId("SidoraMciRest")
                .component("servlet")
                .host("0.0.0.0")
                .port("{{server.port}}")
                .enableCORS(true)
                // turn on swagger api-doc
                .apiContextPath("/api-doc")
                .apiContextRouteId("api-doc")
                .contextPath("/api")
                .apiProperty("api.title", "Sidora MCI API")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("cors", "true");

        rest().description("Sidora MCI")
                .post("/addProject").description("Testing WI object by deployment")
                    .consumes("application/x-www-form-urlencoded")
                    .produces("text/plain")
                    .param().name("mciProjectXML")
                        .type(RestParamType.formData)
                        .required(true)
                        .dataType("file")
                        .description("mci project xml file to upload")
                    .endParam()
                    .responseMessage().code(200).message("OK :: Created :: CorrelationId: {correlationId}").endResponseMessage()
                    .responseMessage().code(400).message("ERROR").endResponseMessage()
                    .route().routeId("addProjectFile")
                        .log(INFO, LOG_NAME, "${id} :: ${routeId} :: Start add MCI Project file")
                        .unmarshal().mimeMultipart()
                        .convertBodyTo(String.class)
                        .setHeader("mciProjectXML", body())
                        .to("direct:addProject")
                        .log(INFO, LOG_NAME, "${id} :: ${routeId} :: MCI Project file Submitted\n${body}")
                        .removeHeaders("*")
                .endRest();
    }
}
