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

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * The Edan producer.
 */
public abstract class AbstractEdanProducer extends DefaultProducer {
    private static final Logger log = LoggerFactory.getLogger(AbstractEdanProducer.class);
    private EdanEndpoint endpoint;
    private EdanConfiguration edanConfiguration;

    private CloseableHttpClient client;
    private String nonce;
    private String sdfDate;
    private String authContent;
    private String edanPath;

    public AbstractEdanProducer(EdanEndpoint endpoint, EdanConfiguration edanConfiguration, String edanPath) {
        super(endpoint);
        this.endpoint = endpoint;
        this.edanConfiguration = edanConfiguration;
        this.edanPath = edanPath;
    }

    @Override
    protected void doStop() throws Exception {
        log.info("Closing EDAN Http Client");
        if (client != null) {
            client.close();
        }

        super.doStop();
    }

    /**
     * Setting the required http request headers for EDAN Authentication
     * (See <a href="http://edandoc.si.edu/authentication">http://edandoc.si.edu/authentication</a> for more information)
     *
     * @param edanParams the EDAN params string
     */
    public void setEdanAuth(String edanParams) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        nonce = UUID.randomUUID().toString().replace("-", "");

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdfDate = df.format(new Date());

        log.info("setEdanAuth edanParams = {}", edanParams);

        authContent = Base64.getEncoder().encodeToString(DigestUtils.sha1Hex(nonce + "\n" + edanParams + "\n" + sdfDate + "\n" + endpoint.getEdanConfiguration().getEdan_key()).getBytes("UTF-8"));
    }

    protected URIBuilder createUri() throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(edanConfiguration.getEdanBaseUrl().getProtocol());
        builder.setHost(edanConfiguration.getEdanBaseUrl().getHost());
        builder.setPath(edanPath);
        return builder;
    }

    protected void get(final Exchange exchange, URI uri) throws Exception {
        log.info("setEdanAuth getRawQuery:\n{}", uri.getRawQuery());
        log.info("setEdanAuth getQuery:\n{}", uri.getQuery());
        setEdanAuth(uri.getRawQuery());
        //String uri = edanConfiguration.getEdanBaseUrl() + edanPath + "?" + uri;
        log.debug("EdanApiBean uri: {}", uri.toString());

        HttpGet httpget = new HttpGet(uri);
        httpget.setHeader("X-AppId", edanConfiguration.getApp_id());
        httpget.setHeader("X-Nonce", getNonce());
        httpget.setHeader("X-RequestDate", getSdfDate());
        httpget.setHeader("X-AuthContent", getAuthContent());
        httpget.setHeader("X-AppVersion", "EDANInterface-0.10.1");
        httpget.setHeader("Accept", "*/*");
        httpget.setHeader("Accept-Encoding", "identity");
        httpget.setHeader("User-Agent", "unknown");

        log.debug("EdanApiBean httpGet headers: " + Arrays.toString(httpget.getAllHeaders()));

        try (CloseableHttpResponse response = getClient().execute(httpget)) {
            HttpEntity entity = response.getEntity();

            log.debug("CloseableHttpResponse response.toString(): {}", response.toString());

            Integer responseCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().getReasonPhrase();
            String entityResponse = EntityUtils.toString(entity, "UTF-8");

            //set the response
            exchange.getIn().setBody(entityResponse);

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
                exchange.getIn().setHeader(name, header.getValue());

            }
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, statusLine);

            if (responseCode != 200) {
                log.debug("Edan response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
                log.error("Edan response entity: {}", entityResponse);
            } else {
                log.debug("Edan response: " + Exchange.HTTP_RESPONSE_CODE + "= {}, " + Exchange.HTTP_RESPONSE_TEXT + "= {}", responseCode, statusLine);
            }



            if (!ContentType.getOrDefault(entity).getMimeType().equalsIgnoreCase("application/json")) {
                throw new EdanIdsException("The EDAN response did not contain and JSON! Content-Type is " + contentType);
            }
        } catch (Exception e) {
            throw new EdanIdsException("EdanApiBean error sending Edan request", e);
        }
    }

    protected CloseableHttpClient getClient() throws Exception {
        if (client == null) {
            client = ((EdanEndpoint)getEndpoint()).createClient();
        }

        return client;
    }

    public EdanConfiguration getEdanConfiguration() {
        return edanConfiguration;
    }

    public String getNonce() {
        return nonce;
    }

    public String getSdfDate() {
        return sdfDate;
    }

    public String getAuthContent() {
        return authContent;
    }

    public boolean isNotEmpty(String param) {
        return !isEmpty(param);
    }

    public static boolean isEmpty(String str) {
        if (str != null) {
            int len = str.length();
            for (int x = 0; x < len; ++x) {
                if (str.charAt(x) > ' ') {
                    return false;
                }
            }
        }
        return true;
    }
}
