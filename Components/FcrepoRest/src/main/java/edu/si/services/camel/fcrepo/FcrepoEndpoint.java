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

import org.apache.camel.*;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.spi.*;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.util.ObjectHelper;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

/**
 * FcrepoEndpoint is used with FcrepoComponent.  Currently only supports producer and the format parameter.
 *
 * @author parkjohn
 */
@ManagedResource(description = "Managed FcrepoEndpoint")
@UriEndpoint(firstVersion = "1.0", scheme = "fcrepo", title = "Fedora's REST API", syntax = "fcrepo:objects", apiSyntax = "objects/objectPID", producerOnly = true, category = {Category.WEBSERVICE, Category.REST, Category.API})
class FcrepoEndpoint extends DefaultEndpoint {

    @UriPath(description = "Fedora path")
    @Metadata(required = true)
    private String path;

    @UriParam(defaultValue = "GET")
    private String method = "GET";

    @UriParam(name = "pid", description = "object pid")
    private String pid;

    @UriParam(name = "query", description = "Query for the fedora ri")
    private String query;

    @UriParam(name = "format", defaultValue = "xml")
    private String format = "xml";

    @UriParam(name = "resultFormat", defaultValue = "xml")
    private String resultFormat = "xml";

    @UriParam(label = "advanced")
    private HeaderFilterStrategy headerFilterStrategy;

    private FcrepoConfiguration configuration;
    private String queryString;

    /**
     * Create a FcrepoEndpoint with a uri, path, component, and configuration
     * @param uri
     * @param component
     */
    public FcrepoEndpoint(String uri, FcrepoComponent component) {
        super(uri, component);
    }

    /**
     * Currently this endpoint does not support Consumer and therefore it cannot be used to start a route
     *
     * @param processor passed Camel processor
     * @return The method will throw exception and does not return Camel consumer
     * @throws RuntimeCamelException
     */
    @Override
    public Consumer createConsumer(Processor processor) throws RuntimeCamelException {
        throw new RuntimeCamelException("Fcrepo Component cannot start a route");
    }

    /**
     *
     * @return FcrepoProducer
     */
    @Override
    public Producer createProducer() {
        return new FcrepoProducer(this);
    }

    /**
     * Defining the given endpoint is singleton
     *
     * @return boolean true
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * FcrepoConfiguration getter
     *
     * @return fcrepoConfiguration
     */
    public FcrepoConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * configuration setter
     *
     * @param config The FcrepoConfiguration
     */
    public void setConfiguration(final FcrepoConfiguration config) {
        configuration = config;
    }

    public String getPath() {
        return path;
    }

    /**
     * Fedora path ex) objects, /objects/{pid}/datastreams/{dsID}
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    /**
     * Sets the method to process the request
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Fedora pid parameter getter
     *
     * @return pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * Fedora pid parameter setter - ex) pid=true for findObjects REST API
     *
     * @param pid
     */
    public void setPid(String pid) {
        this.pid = pid;
    }

    /**
     * Fedora query parameter getter
     *
     * @return query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Fedora query parameter setter - ex) query=pid%7Esi:121909
     *
     * @param query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Fedora format parameter getter - ex) HTML, XML
     * @return format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Fedora format parameter setter - ex) HTML, XML
     *
     * @param format passed in format parameter
     */
    public void setFormat(String format) {
        this.format = format;
    }

    public String getResultFormat() {
        return resultFormat;
    }

    /**
     * Fedora format parameter setter - ex) HTML, XML
     * @param resultFormat
     */
    public void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    /**
     * Fedora HeaderFilterStrategy
     * @return HeaderFilterStrategy
     */
    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return headerFilterStrategy;
    }

    /**
     * Fedora headerFilterStrategy
     * @param headerFilterStrategy
     */
    public void setHeaderFilterStrategy(HeaderFilterStrategy headerFilterStrategy) {
        this.headerFilterStrategy = headerFilterStrategy;
    }

    public String getQueryString() {
        return queryString;
    }

    /**
     * Sets the uriPattern
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        // Validate mandatory user password
        ObjectHelper.notNull(configuration.getUsername(), "username");
        ObjectHelper.notNull(configuration.getPassword(), "password");
    }

    public WebTarget createWebClientTarget() {

        final Client client = ClientBuilder.newBuilder().build();

        URI uri = null;
        if (ObjectHelper.isNotEmpty(configuration.getHost()) && ObjectHelper.isNotEmpty(getPath()) && ObjectHelper.isNotEmpty(getQueryString())) {
            uri = UriBuilder.fromUri(configuration.getHost()).path(getPath()).replaceQuery(getQueryString()).build();
        }

        return client.target(uri);
    }

    /**
     * Returns the base64 encoded basic authentication token.
     *
     * @return base64 encoded authorization header for the given username/password in configuration
     * @param username
     * @param password
     */
    private BasicAuthentication getBasicAuthToken(String username, String password) {
        return new BasicAuthentication(username, password);
    }
}
