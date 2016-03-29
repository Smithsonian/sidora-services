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

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * FcrepoComponent is used with Fedora Repository 3.x and is based on CXF library to call Fedora's REST APIs.
 * This component currently does not support all of the Fedora Repo's REST APIs and there may be some overlapping
 * Fedora API coverage between the FedoraRepo component.  This component was created as a starting point to support
 * Fedora's REST APIs using an HTTP Client to expand the API support coverage in streamlined way.
 *
 * @author parkjohn
 */
public class FcrepoComponent extends UriEndpointComponent {

    private static final Logger log = LoggerFactory.getLogger(FcrepoComponent.class);

    @BeanInject
    private FcrepoConfiguration fcrepoConfiguration;

    /**
     * Default FcrepoComponent constructor
     */
    public FcrepoComponent() {
        super(FcrepoEndpoint.class);
    }

    /**
     * Creates FcrepoComponent instance with given camel context
     * @param context the camel context for the component.
     */
    public FcrepoComponent(final CamelContext context) {
        super(context, FcrepoEndpoint.class);
    }

    /**
     * Overriding the required createEndpoint method
     *
     * @param uri full uri with scheme, remaining, and the parameter values - ex) fcrepo://objects/ct:9311/datastreams?format=xml
     * @param remaining - uri without scheme and parameters - ex) objects/ct:9311/datastreams
     * @param parameters - request parameters from the uri in key/value format ex) format=xml
     *
     * @return FcrepoEndpoint for the given URI
     * @throws Exception
     */
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        log.debug("createEndpoint uri: "+ uri);
        log.debug("createEndpoint remaining: "+ remaining);
        log.debug("createEndpoint parameters: "+ parameters.toString());

        Endpoint endpoint = new FcrepoEndpoint(uri, this);
        setProperties(endpoint, parameters);

        return endpoint;
    }

    /**
     * FcrepoConfiguration getter
     *
     * @return FcrepoConfiguration
     */
    public FcrepoConfiguration getFcrepoConfiguration() {
        return fcrepoConfiguration;
    }

    /**
     * FcrepoConfiguration setter
     *
     * @param fcrepoConfiguration passed in fcrepo configuration
     */
    public void setFcrepoConfiguration(FcrepoConfiguration fcrepoConfiguration) {
        this.fcrepoConfiguration = fcrepoConfiguration;
    }
}
