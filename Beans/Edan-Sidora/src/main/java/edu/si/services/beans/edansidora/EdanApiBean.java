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

package edu.si.services.beans.edansidora;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * @file EDAN API helper bean
 *
 * @author jbirkhimer
 *
 */

public class EdanApiBean {

    private static final Logger LOG = LoggerFactory.getLogger(EdanApiBean.class);

    @PropertyInject(value = "si.ct.uscbi.server")
    private String server;

    @PropertyInject(value = "si.ct.uscbi.appId")
    private String app_id;

    @PropertyInject(value = "si.ct.uscbi.edanKey")
    private String edan_key;

    /**
     * int indicates the authentication type/level 0 for unsigned/trusted/T1
     * requests; (currently unused) 1 for signed/T2 requests; 2 for password based
     * (unused)
     * auth_type is only for future, this code is only for type 1
     */
    @PropertyInject(value = "si.ct.uscbi.authType", defaultValue = "1")
    private int auth_type;

    private CloseableHttpClient client;

    public void setServer(String server) {
        this.server = server;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public void setEdan_key(String edan_key) {
        this.edan_key = edan_key;
    }

    public void setAuth_type(String auth_type) {
        this.auth_type = Integer.parseInt(auth_type);
    }

    /**
     * Constructor
     */
    public EdanApiBean() {

    }

    public void initIt() throws Exception {
        LOG.info("Creating EdanApiBean Http Client");
        client = HttpClientBuilder.create().build();
    }

    public void cleanUp() throws Exception {
        LOG.info("Closing EdanApiBean Http Client");
        client.close();
    }

    /**
     * Perform a EDAN http request
     *
     * @param exchange Camel exchange containing the edanServiceEndpoint and edanQueryParams that gets appended to the
     *                    service url
     */
    public void sendRequest(Exchange exchange,
                            @Header("edanServiceEndpoint") String edanServiceEndpoint,
                            @Header(Exchange.HTTP_QUERY) String edanQueryParams) throws Exception {

        Message out = exchange.getIn();

        try {
            if (client == null) {
                throw new EdanIdsException("EdanApiBean Client IS NULL");
            } else {
                System.setProperty("http.agent", "");
                String uri = server + edanServiceEndpoint + "?" + edanQueryParams;
                LOG.debug("EdanApiBean uri: {}", uri);

                MessageDigest md = MessageDigest.getInstance("SHA-1");
                String nonce = UUID.randomUUID().toString().replace("-", "");

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String sdfDate = df.format(new Date());

                String authContent = Base64Utility.encode(DigestUtils.sha1Hex(nonce + "\n" + edanQueryParams + "\n" + sdfDate + "\n" + edan_key).getBytes("UTF-8"));

                LOG.warn("setEdanAuth nonce:{}, edanQuery:{}, sfDate:{}, edan_key:{} | authContent:{}", nonce, edanQueryParams, sdfDate, edan_key, authContent);

                HttpGet httpget = new HttpGet(uri);
                httpget.setHeader("X-AppId", app_id);
                httpget.setHeader("X-Nonce", nonce);
                httpget.setHeader("X-RequestDate", sdfDate);
                httpget.setHeader("X-AuthContent", authContent);
                httpget.setHeader("X-AppVersion", "EDANInterface-0.10.1");
                httpget.setHeader("Accept", "*/*");
                httpget.setHeader("Accept-Encoding", "identity");
                httpget.setHeader("User-Agent", "unknown");

                LOG.debug("EdanApiBean httpGet:\nuri: {}\nheaders:{}", uri, Arrays.toString(httpget.getAllHeaders()));

                try (CloseableHttpResponse response = client.execute(httpget)) {
                    HttpEntity entity = response.getEntity();

                    LOG.debug("CloseableHttpResponse response.toString(): {}", response.toString());

                    Integer responseCode = response.getStatusLine().getStatusCode();
                    String statusLine = response.getStatusLine().getReasonPhrase();
                    String entityResponse = EntityUtils.toString(entity, "UTF-8");

                    //set the response
                    out.setBody(entityResponse);

                    //copy response headers to camel
                    org.apache.http.Header[] headers = response.getAllHeaders();
                    String contentType = null;
                    for (org.apache.http.Header header : response.getAllHeaders()) {
                        String name = header.getName();
                        // mapping the content-type
                        if (name.toLowerCase().equals("content-type")) {
                            name = Exchange.CONTENT_TYPE;
                            contentType = header.getValue();
                        }
                        out.setHeader(name, header.getValue());

                    }
                    out.setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
                    out.setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

                    if (responseCode != 200) {
                        LOG.debug("ERROR: Edan response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                        LOG.error("EDAN ERROR:\nRequest uri:{}[ nonce:{}, sfDate:{} authContent:{} ]Headers:{}\nResponse entity:{}", uri, nonce, sdfDate, authContent, Arrays.toString(httpget.getAllHeaders()), entityResponse);
                        throw new EdanIdsException("EDAN ERROR:\nResponse entity:" + entityResponse + "\nRequest uri:" + uri);
                    } else {
                        LOG.debug("Edan response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                    }

                    if (!ContentType.getOrDefault(entity).getMimeType().equalsIgnoreCase("application/json")) {
                        throw new EdanIdsException("The EDAN response did not contain and JSON! Content-Type is " + contentType);
                    }
                } catch (Exception e) {
                    //throw new EdanIdsException("EdanApiBean error sending Edan request", e);
                    throw e;
                }
            }
        } catch (Exception e) {
            //throw new EdanIdsException(e);
            throw e;
        }
    }
}
