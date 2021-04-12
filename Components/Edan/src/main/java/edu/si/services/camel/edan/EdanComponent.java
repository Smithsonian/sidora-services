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

package edu.si.services.camel.edan;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

/**
 * Represents the component that manages {@link EdanEndpoint}.
 */

/*
AbstractApiComponent is based on the deprecated UriEndpointComponent which furthermore is based on the DefaultComponent.
Depending on your needs the DefaultComponent should probably be enough to start with.
The consumer could extend i.e. a ScheduledPollConsumer if you need to regularly poll for new state or
ScheduledBatchPollConsumer if you receive data in batch which you want to split up into different exchanges.
As the producer generates output data sent to the API a DefaultProducer might be sufficient.

The DefaultComponent is for generic components.
That other ApiComponent is if you have an API-contract that you can use to code generate viaa maven plugin - this is more tricky to implement/use.
But its for example used in some of the big API component for camel-linkedin, camel-box, etc. The CiA2 book covers how to use it as well
 */
@Component("edan")
public class EdanComponent extends DefaultComponent {

    private static final Logger log = LoggerFactory.getLogger(EdanComponent.class);

    @Metadata(label = "advanced edan config")
    private EdanConfiguration edanConfiguration;

    /**
     * Create an EdanComponent independent of a CamelContext
     */
    public EdanComponent() {
        this(null);
    }

    /**
     * Given a CamelContext, create an EdanComponent
     * @param context
     */
//    public EdanComponent(CamelContext context) {
//        super(context);
//        //registerExtension(new GoogleMailStreamComponentVerifierExtension());
//        //this.edanConfiguration = new EdanConfiguration();
//        this.edanConfiguration = edanConfiguration;
//    }

    public EdanComponent(EdanConfiguration edanConfiguration) {
        this.edanConfiguration = edanConfiguration;
    }

    /**
     *
     * @param uri full uri with scheme, remaining, and the parameter values - ex) edan://metadata/v2.0/metadata/search.htm?q=type:emammal_image
     * @param remaining uri without scheme and parameters - ex) metadata/v2.0/metadata/search.htm
     * @param parameters request parameters from the uri in key/value format ex) q=type:emammal_image
     * @return EdanEndpoint for the given URI
     * @throws Exception
     */
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        log.debug("createEndpoint uri: "+ uri);
        log.debug("createEndpoint remaining: "+ remaining);
        log.debug("createEndpoint parameters: "+ parameters.toString());
        log.debug(toString());

        EdanEndpoint endpoint = new EdanEndpoint(uri, this, getEdanConfiguration().clone());
        endpoint.setEdanService(remaining);
        //The DefaultEndpoint class already contains a field named 'id' with getter so we need this workaround
        if (parameters.containsKey("id")) {
            endpoint.setEdanId(getAndRemoveParameter(parameters, "id", String.class));
            log.debug("createEndpoint parameters after removing id: "+ parameters.toString());
        }
        setProperties(endpoint, parameters);

        log.debug("Created Edan Endpoint [{}]", endpoint);

        return endpoint;
    }

    @Override
    public boolean useRawUri() {
        return true;
    }

    /**
     * Get the component's configuration.
     * @return the configuration for the component.
     */
    public EdanConfiguration getEdanConfiguration() {
        if (edanConfiguration == null) {
            edanConfiguration = new EdanConfiguration();
        }
        return edanConfiguration;
    }

    /**
     * Set the component's configuration.
     * @param edanConfiguration the configuration settings for the component
     */
    public void setEdanConfiguration(final EdanConfiguration edanConfiguration) {
        this.edanConfiguration = edanConfiguration.clone();
    }

    /**
     * set the EDAN host url value component-wide
     * @param edanBaseUrl the EDAN host url
     */
    public void setEdanBaseUrl(URL edanBaseUrl) {
        getEdanConfiguration().setEdanBaseUrl(edanBaseUrl);
    }

    /**
     * set the EDAN app_id value component-wide
     * @param app_id the EDAN app_id used for authentication
     */
    public void setApp_id(String app_id) {
        getEdanConfiguration().setApp_id(app_id);
    }

    /**
     * set the EDAN key value component-wide
     * @param edan_key the EDAN key used for authentication
     */
    public void setEdan_key(String edan_key) {
        getEdanConfiguration().setEdan_key(edan_key);
    }

    /**
     * set the Edan auth_type value component-wide
     * indicates the authentication type/level 0 for unsigned/trusted/T1
     * requests; (currently unused) 1 for signed/T2 requests; 2 for password based
     * (unused)
     * auth_type is only for future, this code is only for type 1
     * @param auth_type the EDAN auth_type used for authentication
     */
    public void setAuth_type(int auth_type) {
        getEdanConfiguration().setAuth_type(auth_type);
    }

    @Override
    public String toString() {
        return "EdanComponent{" +
                "edanBaseUrl='" + getEdanConfiguration().getEdanBaseUrl() + '\'' +
                ", app_id='" + getEdanConfiguration().getApp_id() + '\'' +
                ", edan_key='" + getEdanConfiguration().getEdan_key() + '\'' +
                ", auth_type='" + getEdanConfiguration().getAuth_type() + '\'' +
                ", edanConfiguration=" + edanConfiguration.toString() +
                '}';
    }

}
