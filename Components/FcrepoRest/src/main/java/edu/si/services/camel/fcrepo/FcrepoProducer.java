/*
 * Copyright 2015 Smithsonian Institution.
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
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * FcrepoProducer will build the URL based on available information from the FcrepoEndpoint
 * and the Camel message exchange properties and invoke the Fedora Repository's RESTful APIs over HTTP.
 * Currently only supports GET HTTP method and limited Fedora REST APIs are supported.
 *
 * @author parkjohn
 */
public class FcrepoProducer extends DefaultProducer {

    private static final Logger log = LoggerFactory.getLogger(FcrepoProducer.class);

    private final FcrepoEndpoint fcrepoEndpoint;

    public FcrepoProducer(FcrepoEndpoint endpoint) {
        super(endpoint);
        this.fcrepoEndpoint = endpoint;
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
    private void doRequest(final Exchange exchange){
        final Message in = exchange.getIn();
        final FcrepoHttpMethodEnum method = getHttpMethodEnum(exchange);
        final String requestURL = getRequestURL();

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(requestURL);
        Response response;

        try{
            Invocation.Builder builder = target.request();
            builder.header("Authorization", getBasicAuthToken());

            switch (method) {
//            case POST:
//                response = builder.post(Entity.xml(in.getBody()));
//                break;
                case GET:
                default:
                    response = builder.get();
            }

            in.setHeader(Exchange.HTTP_RESPONSE_CODE, response.getStatusInfo().getStatusCode());

            String mediaType = null;
            if (response.getMediaType()!=null) {
                mediaType = response.getMediaType().toString();
                in.setHeader(Exchange.CONTENT_TYPE, mediaType);
            }

            //depends on the response content type, grab the content as string or object
            if (MediaType.APPLICATION_XML.equals(mediaType) || MediaType.TEXT_XML.equals(mediaType)
                    || MediaType.TEXT_HTML.equals(mediaType)){
                String responseBodyStr = response.readEntity(String.class);
                in.setBody(responseBodyStr);
            }
            else{
                Object responseBodyObj = response.getEntity();
                in.setBody(responseBodyObj);
            }

        } catch(Exception ex){
            log.error("Problem while processing Fcrepo HTTP request", ex);
        }
        finally {
            client.close();
        }

    }

    /**
     * This method will build the endpoint URI based on the Fedora Repo host information and the URI given
     * to the FcrepoEndpoint.
     *
     * @return prepared URI endpoint
     */
    private String getRequestURL() {
        StringBuilder sb = new StringBuilder();

        //TODO update properties fetch strategy
        // replace this with fetching the si.fedora.host property from the configuration file once the property is
        // removed from the system.properties external file with karaf config file.
        sb.append(System.getProperty("si.fedora.host"));

        if (fcrepoEndpoint.getEndpointUri() != null) {
            sb.append("/");
            log.debug("Inside getRequestUrl() - original endPoint URI is: " + fcrepoEndpoint.getEndpointUri());

            //removing fcrepo scheme so the endpoint uri can be appended to the base fedora host url for rest api
            sb.append(fcrepoEndpoint.getEndpointUri().replaceFirst("fcrepo://", ""));
        }

        String updatedRequestURL = sb.toString();
        log.debug("Inside getRequestUrl() - updated endPoint URI is: " + updatedRequestURL);

        return updatedRequestURL;
    }

    /**
     * By looking at the Camel message exchange's header, the method will return the HTTP request method type
     *
     * @param exchange camel message exchange
     * @return FcrepoHttpMethodEnum based on information found in the exchange; default is GET
     */
    private FcrepoHttpMethodEnum getHttpMethodEnum(final Exchange exchange) {
        FcrepoHttpMethodEnum method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, FcrepoHttpMethodEnum.class);

        log.debug("Inside getHttpMethod() - HTTP Method from the exchange is: "+ method.toString());

        if (method == null) {
            return FcrepoHttpMethodEnum.GET;
        } else {
            return method;
        }
    }

    /**
     * Returns the base64 encoded basic authentication token.
     * @return base64 encoded authorization header for the given username/password in configuration
     */
    private String getBasicAuthToken() {

        final String username = System.getProperty("si.fedora.user");
        final String password = System.getProperty("si.fedora.password");

        //TODO update properties fetch strategy once the property is coming from the karaf config rather than system prop
        //final String username = fcrepoEndpoint.getFcrepoConfiguration().getAuthUsername();
        //final String password = fcrepoEndpoint.getFcrepoConfiguration().getAuthPassword();

        return "Basic "
                + org.apache.cxf.common.util.Base64Utility.encode((username+":"+password).getBytes());

    }
}