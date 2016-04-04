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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * FcrepoEndpoint is used with FcrepoComponent.  Currently only supports producer and the format parameter.
 *
 * @author parkjohn
 */
class FcrepoEndpoint extends DefaultEndpoint {

    private String format;
    private String query;
    private String pid;
    private String resultFormat;

    private final FcrepoConfiguration fcrepoConfiguration;

    public FcrepoEndpoint(String uri, FcrepoComponent component) {
        super(uri, component);
        if (component.getFcrepoConfiguration() != null) {
            this.fcrepoConfiguration = component.getFcrepoConfiguration();
        }
        else {
            this.fcrepoConfiguration = new FcrepoConfiguration();
        }
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
     * Fedora resultFormat parameter getter
     *
     * @return resultFormat
     */
    public String getResultFormat() {
        return resultFormat;
    }

    /**
     * Fedora resultFormat parameter setter - ex) resultFormat=xml
     *
     * @param resultFormat
     */
    public void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    /**
     * FcrepoConfiguration getter
     *
     * @return fcrepoConfiguration
     */
    public FcrepoConfiguration getFcrepoConfiguration() {
        return fcrepoConfiguration;
    }

}
