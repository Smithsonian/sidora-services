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

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URL;

/**
 * @author jbirkhimer
 */
@UriParams
public class EdanConfiguration implements Cloneable {

    @UriParam
    private URL edanBaseUrl = null;
    @UriParam
    private String app_id = null;
    @UriParam
    private String edan_key = null;

    /**
     * int indicates the authentication type/level 0 for unsigned/trusted/T1
     * requests; (currently unused) 1 for signed/T2 requests; 2 for password based
     * (unused)
     * auth_type is only for future, this code is only for type 1
     */
    @UriParam
    private int auth_type = 1;

    public EdanConfiguration() {
        super();
    }

    /**
     * Copy an EdanConfiguration object.
     *
     * @return a copy of the component-wide configuration
     */
    @Override
    protected EdanConfiguration clone() {
        try {
            return (EdanConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeCamelException(e);
        }
    }

    /**
     *
     * @return
     */
    public URL getEdanBaseUrl() {
        return edanBaseUrl;
    }

    /**
     * EDAN base url
     * @param edanBaseUrl
     */
    public void setEdanBaseUrl(URL edanBaseUrl) {
        this.edanBaseUrl = edanBaseUrl;
    }

    /**
     *
     * @return
     */
    public String getApp_id() {
        return app_id;
    }

    /**
     * EDAN AppId
     * @param app_id
     */
    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    /**
     *
     * @return
     */
    public String getEdan_key() {
        return edan_key;
    }

    /**
     * EDAN key
     * @param edan_key
     */
    public void setEdan_key(String edan_key) {
        this.edan_key = edan_key;
    }

    /**
     *
     * @return
     */
    public int getAuth_type() {
        return auth_type;
    }

    /**
     * EDAN Auth type
     * @param auth_type
     */
    public void setAuth_type(int auth_type) {
        this.auth_type = auth_type;
    }

    public CloseableHttpClient createClient() {
        return HttpClientBuilder.create().build();
    }

    @Override
    public String toString() {
        return "EdanConfiguration{" +
                "edanBaseUrl=" + edanBaseUrl +
                ", app_id='" + app_id + '\'' +
                ", edan_key='" + edan_key + '\'' +
                ", auth_type=" + auth_type +
                '}';
    }
}
