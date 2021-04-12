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

package edu.si.services.camel.fcrepo;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.http.common.HttpMethods;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultProducer;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.util.ObjectHelper;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.apache.camel.Exchange.HTTP_METHOD;

/**
 * FcrepoProducer will build the URL based on available information from the FcrepoEndpoint
 * and the Camel message exchange properties and invoke the Fedora Repository's RESTful APIs over HTTP.
 * Currently only supports GET HTTP method and limited Fedora REST APIs are supported.
 *
 * @author parkjohn
 */
public class FcrepoProducer extends DefaultProducer {

    private static final Logger log = LoggerFactory.getLogger(FcrepoProducer.class);

    private final FcrepoEndpoint endpoint;

    public FcrepoProducer(FcrepoEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    /**
     * Overriding the process method for the FcrepoProducer
     *
     * @param exchange camel message exchange
     */
    @Override
    public void process(Exchange exchange) {
        doRequest(exchange);
    }

    /**
     * The method to build URL and executes the HTTP requests.
     * The HTTP responds are then set back to the exchange in message header and body.
     *
     * @param exchange camel message exchange
     */
    private void doRequest(final Exchange exchange) {
        final Message in = exchange.getIn();
        final String method = getMethod(exchange);

        FcrepoEndpoint fcrepoEndpoint = ObjectHelper.cast(FcrepoEndpoint.class, getEndpoint());

        WebTarget fcrepoWebTarget = endpoint.createWebClientTarget();

        applyAuth(fcrepoEndpoint, fcrepoWebTarget);

        log.debug("Fcrepo uir request: {}", fcrepoWebTarget.getUri());

        writeResponse(exchange, fcrepoEndpoint, method, fcrepoWebTarget);
        /*try {
            Invocation.Builder builder = fcrepoWebTarget.request();

            switch (method) {
                case POST:
                    response = fcrepoWebTarget.request().post(Entity.xml(in.getBody()));
                    break;
                case GET:
                default:
                    response = fcrepoWebTarget.request().get();
            }

            in.setHeader(Exchange.HTTP_RESPONSE_CODE, response.getStatusInfo().getStatusCode());

            String mediaType = null;
            if (response.getMediaType() != null) {
                mediaType = response.getMediaType().toString();
                in.setHeader(Exchange.CONTENT_TYPE, mediaType);
            }

            //depends on the response content type, grab the content as string or object
            if (MediaType.APPLICATION_XML.equals(mediaType) || MediaType.TEXT_XML.equals(mediaType)
                    || MediaType.TEXT_HTML.equals(mediaType)) {
                String responseBodyStr = response.readEntity(String.class);
                in.setBody(responseBodyStr);
            } else {
                Object responseBodyObj = response.getEntity();
                in.setBody(responseBodyObj);
            }

        } catch (Exception ex) {
            log.error("Problem while processing Fcrepo HTTP request", ex);
        } finally {
            response.close();
        }*/

    }

    private void writeResponse(Exchange exchange, FcrepoEndpoint fcrepoEndpoint, String method, WebTarget target) {
        Response response = createResponse(exchange, fcrepoEndpoint, method, target);
        if (response == null) {
            // maybe throw exception because not method was correct
            throw new IllegalArgumentException("Method '" + method + "' is not supported method to create response");
        }
        doWriteResponse(exchange, response, fcrepoEndpoint.getHeaderFilterStrategy());
        response.close();
        return;
    }

    private Response createResponse(Exchange exchange, FcrepoEndpoint fcrepoEndpoint, String method, WebTarget target) {
        String body = exchange.getIn().getBody(String.class);
        log.debug("Body in producer: {}", body);

        String mediaType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);

        log.debug("Populate Fcrepo request from exchange body: {} using media type {}", body, mediaType);

        Invocation.Builder builder;
        if (mediaType != null) {
            builder = target.request(mediaType);
        } else {
            builder = target.request();
        }

        for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (fcrepoEndpoint.getHeaderFilterStrategy() != null
                    && !fcrepoEndpoint.getHeaderFilterStrategy().applyFilterToCamelHeaders(key, value, exchange)) {
                builder.header(key, value);
                log.debug("Populate Fcrepo request from exchange header: {} value: {}", key, value);
            }
        }

        Response response = null;
        if (method.equals("GET")) {
            response = builder.get();
        }
        if (method.equals("POST")) {
            response = builder.post(Entity.entity(body, mediaType));
        }
        if (method.equals("PUT")) {
            response = builder.put(Entity.entity(body, mediaType));
        }
        if (method.equals("DELETE")) {
            response = builder.delete();
        }
        if (method.equals("OPTIONS")) {
            response = builder.options();
        }
        if (method.equals("TRACE")) {
            response = builder.trace();
        }
        if (method.equals("HEAD")) {
            response = builder.head();
        }
        return response;
    }

    public void doWriteResponse(Exchange exchange, Response response, HeaderFilterStrategy headerFilterStrategy) {
        // set response code
        int responseCode = response.getStatus();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_RESPONSE_CODE, responseCode);

        for (String key : response.getHeaders().keySet()) {
            Object value = response.getHeaders().get(key);
            if (headerFilterStrategy != null
                    && !headerFilterStrategy.applyFilterToExternalHeaders(key, value, exchange)) {
                headers.put(key, value);
                log.debug("Populate Camel exchange from response: {} value: {}", key, value);
            }
        }

        // set fcrepo response as header so the end user has access to it if needed
        headers.put(Exchange.HTTP_RESPONSE_TEXT, response);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_TEXT, response);

        log.debug("Headers from exchange.getIn() : {}", exchange.getIn().getHeaders());
        log.debug("Headers from exchange.getOut() before copying : {}", exchange.getMessage().getHeaders());
        log.debug("Header from response : {}", response.getHeaders());

        if (response.hasEntity()) {
            exchange.getMessage().setBody(response.readEntity(String.class));
        } else {
            exchange.getMessage().setBody(response.getStatusInfo());
        }

        // preserve headers from in by copying any non existing headers
        // to avoid overriding existing headers with old values
        MessageHelper.copyHeaders(exchange.getIn(), exchange.getMessage(), false);
    }

    private void applyAuth(FcrepoEndpoint fcrepoEndpoint, WebTarget target) {
        FcrepoConfiguration configuration = fcrepoEndpoint.getConfiguration();
        if (ObjectHelper.isNotEmpty(configuration.getUsername()) && ObjectHelper.isNotEmpty(configuration.getPassword())) {
            target.register(new BasicAuthentication(configuration.getUsername(), configuration.getPassword()));
        }
        if (log.isTraceEnabled()) {
            log.trace("Basic authentication was applied");
        }
    }

    /**
     * By looking at the Camel message exchange's header, the method will return the HTTP request method type
     *
     * @param exchange camel message exchange
     * @return FcrepoHttpMethodEnum based on information found in the exchange; default is GET
     */
    private String getMethod(final Exchange exchange) {
        String methodHeader = exchange.getIn().getHeader(HTTP_METHOD, String.class);
        String method = endpoint.getMethod();
        if (methodHeader != null && !method.equalsIgnoreCase(methodHeader)) {
            method = methodHeader;
        }
        log.debug("Inside getHttpMethod() - HTTP Method from the exchange is: " + method);

        return method;
    }

    /**
     * This method will build the endpoint URI based on the Fedora Repo host information and the URI given
     * to the FcrepoEndpoint.
     *
     * @param exchange
     * @return prepared URI endpoint
     */
//    private String getUrl(Exchange exchange) {
//        StringBuilder sb = new StringBuilder();
//
//        //TODO update properties fetch strategy
//        // replace this with fetching the si.fedora.host property from the configuration file once the property is
//        // removed from the system.properties external file with karaf config file.
//        sb.append(getEndpoint().getCamelContext().resolvePropertyPlaceholders("{{si.fedora.host}}"));
//
//        if (endpoint.getEndpointUri() != null) {
//            sb.append("/");
//            log.debug("Inside getRequestUrl() - original endPoint URI is: " + endpoint.getEndpointUri());
//
//            //removing fcrepo scheme so the endpoint uri can be appended to the base fedora host url for rest api
//            sb.append(endpoint.getEndpointUri().replaceFirst("fcrepo://", ""));
//        }
//
//        String updatedRequestURL = sb.toString();
//        log.debug("Inside getRequestUrl() - updated endPoint URI is: " + updatedRequestURL);
//
//        return updatedRequestURL;
//    }

    /**
     * Returns the base64 encoded basic authentication token.
     *
     * @return base64 encoded authorization header for the given username/password in configuration
     */
//    private HttpAuthenticationFeature getBasicAuthToken() {
//
////        final String username = System.getProperty("si.fedora.user");
////        final String password = System.getProperty("si.fedora.password");
////
////        //TODO update properties fetch strategy once the property is coming from the karaf config rather than system prop
//        final String username = endpoint.getConfiguration().getUsername();
//        final String password = endpoint.getConfiguration().getPassword();
////
////        return "Basic "
////                + org.apache.cxf.common.util.Base64Utility.encode((username + ":" + password).getBytes());
//
//        HttpAuthenticationFeature feature = HttpAuthenticationFeature.universalBuilder()
//                .credentialsForBasic(username, password).build();
//        return feature;
//
//    }
}