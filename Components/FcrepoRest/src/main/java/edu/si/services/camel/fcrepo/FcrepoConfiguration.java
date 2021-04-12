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

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * FcrepoConfiguration will hold the basic fedora configuration properties such as the username/password and Fedora hostname
 *
 * @author parkjohn
 */
@UriParams
public class FcrepoConfiguration implements Cloneable {

//    @UriParam(label = "security", secret = true)
//    @Metadata(required = true)
    private String username;

//    @UriParam(label = "security", secret = true)
//    @Metadata(required = true)
    private String password;

//    @UriParam(label = "common", defaultValue = "http://localhost")
//    @Metadata(required = true)
    private String host = "http://localhost";

//    @UriParam(label = "common", defaultValue = "8080")
//    @Metadata(required = true)
//    private int port = 8080;

//    @UriParam(label = "common", defaultValue = "fedora")
//    @Metadata(required = true)
//    private String contextPath = "fedora";

    /**
     * Create a new FcrepoConfiguration object
     */
    public FcrepoConfiguration() {
    }

    /**
     * Create a new FcrepoConfiguration object
     */
    public FcrepoConfiguration(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Copy an FcrepoConfiguration object.
     *
     * @return a copy of the component-wide configuration
     */
    @Override
    public FcrepoConfiguration clone() {
        try {
            return (FcrepoConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeCamelException(e);
        }
    }

    /**
     * authUsername getter
     *
     * @return the username used for repository authentication
     */
    public String getUsername() {
        return username;
    }

    /**
     * authUsername setter
     *
     * @param username used for repository authentication
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * authPassword getter
     *
     * @return the password used for repository authentication
     */
    public String getPassword() {
        return password;
    }

    /**
     * authPassword setter
     *
     * @param password used for repository authentication
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * fedoraHost getter
     *
     * @return the fedoraHost realm used for repository authentication
     */
    public String getHost() {
        return host;
    }

    /**
     * fedoraHost setter
     *
     * @param host used for authentication
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /*public int getPort() {
        return port;
    }

    *//**
     * fedora port
     * @param port
     *//*
    public void setPort(int port) {
        this.port = port;
    }

    public String getContextPath() {
        return contextPath;
    }

    *//**
     * fedora contextPath
     * @param contextPath
     *//*
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }*/
}