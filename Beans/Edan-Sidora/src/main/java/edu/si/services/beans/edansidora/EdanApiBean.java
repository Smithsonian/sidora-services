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
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @file EDANInterface This is a pretty raw class that just generates a call to
 * the EDAN API.
 */

public class EdanApiBean {

    private static final Logger LOG = LoggerFactory.getLogger(EdanApiBean.class);

    @PropertyInject(value = "si.ct.uscbi.server")
    private String server;

    @PropertyInject(value = "si.ct.uscbi.appId")
    private String app_id;

    @PropertyInject(value = "si.ct.uscbi.edanKey")
    private String edan_key;

    @PropertyInject(value = "si.ct.uscbi.edanService")
    private String edanService;

    /**
     * int indicates the authentication type/level 0 for unsigned/trusted/T1
     * requests; (currently unused) 1 for signed/T2 requests; 2 for password based
     * (unused)
     * auth_type is only for future, this code is only for type 1
     */
    @PropertyInject(value = "si.ct.uscbi.authType", defaultValue = "1")
    private int auth_type;

    public String result_format = "json";
    private CloseableHttpClient client;

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
     * Perform a http request
     *
     * @param exchange Camel exchange containing the querystring that gets appended to the
     *                    service url
     */
    public void sendRequest(Exchange exchange) throws EdanIdsException {

        try {
            Message out = exchange.getIn();

            String originalUri = out.getHeader("edanUri", String.class);

            if (client == null) {
                LOG.error("EdanApiBean Client IS NULL");
            } else {
                System.setProperty("http.agent", "");
                String uri = server + edanService + "?" + originalUri;
                LOG.debug("EdanApiBean uri: {}", uri);

                HttpGet httpget = new HttpGet(uri);
                Map<String, String> encodedHeaders = null;
                try {
                    encodedHeaders = encodeHeader(originalUri);
                } catch (Exception e) {
                    throw new EdanIdsException("EdanApiBean error encoding http get headers", e);
                }

                for (Map.Entry<String, String> entry : encodedHeaders.entrySet()) {
                    httpget.addHeader(entry.getKey(), entry.getValue());
                }

                httpget.setHeader("Accept", "*/*");
                httpget.setHeader("Accept-Encoding", "identity");
                httpget.setHeader("User-Agent", "unknown");
                httpget.removeHeaders("Connection");

                LOG.debug("EdanApiBean httpGet headers: " + Arrays.toString(httpget.getAllHeaders()));

                try (CloseableHttpResponse response = client.execute(httpget)) {
                    HttpEntity entity = response.getEntity();

                    if (response.getStatusLine().getStatusCode() != 200) {
                        LOG.error("Edan response status: {}", response.getStatusLine());
                        LOG.error("Edan response entity: {}", EntityUtils.toString(entity, "UTF-8"));
                    } else {
                        LOG.debug("Edan response status: {}", response.getStatusLine());
                    }
                } catch (Exception e) {
                    throw new EdanIdsException("EdanApiBean error sending Edan request", e);
                }
            }
        } catch (Exception e) {
            throw new EdanIdsException(e);
        }
    }

    /**
     * Creates the header for the request to EDAN. Takes $uri, prepends a nonce,
     * and appends the date and appID key. Hashes as sha1() and base64_encode()
     * the result.
     *
     * @param uri The URI (string) to be hashed and encoded.
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @returns Array containing all the elements and signed header value
     */
    private Map<String, String> encodeHeader(String uri) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String ipnonce = this.get_nonce();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sdfDate = df.format(new Date());

        Map<String, String> toReturn = new HashMap<String, String>();
        toReturn.put("X-AppId", this.app_id);
        toReturn.put("X-RequestDate", sdfDate);
        toReturn.put("X-AppVersion", "EDANInterface-0.10.1");

        // For signed/T2 requests
        if (this.auth_type == 1) {
            String auth = ipnonce + "\n" + uri + "\n" + sdfDate + "\n" + this.edan_key;
            String content = SimpleSHA1.SHA1plusBase64(auth);
            toReturn.put("X-Nonce", ipnonce);
            toReturn.put("X-AuthContent", content);
        }

        LOG.debug("encodeHeader toReturn: " + String.valueOf(toReturn));

        return toReturn;
    }

    public String simpleReturnNonce() {
        return this.get_nonce(); // Alternatively you could do:
    }

    public Map<String, String> encodeHeaderSample(String uri) {
        try {
            return encodeHeader(uri);
        } catch (Exception e) {
            Map<String, String> toReturn = new HashMap<String, String>();
            toReturn.put("errorString", e.toString());
            return toReturn;
        }
    }

    /**
     * Generates a nonce.
     *
     * @return string Returns a string containing a randomized set of letters and
     * numbers $length long with $prefix prepended.
     */
    private String get_nonce() {
        return get_nonce(15);
    }

    /**
     * Generates a nonce.
     *
     * @param length $length (optional) Int representing the length of the random
     *               string.
     * @return string Returns a string containing a randomized set of letters and
     * numbers $length long with $prefix prepended.
     */
    private String get_nonce(int length) {
        return get_nonce(length, "");
    }

    /**
     * Generates a nonce.
     *
     * @param length $length (optional) Int representing the length of the random
     *               string.
     * @param prefix $prefix (optional) String containing a prefix to be prepended to
     *               the random string.
     * @return string Returns a string containing a randomized set of letters and
     * numbers $length long with $prefix prepended.
     */
    private String get_nonce(int length, String prefix) {
        String password = "";
        String possible = "0123456789abcdefghijklmnopqrstuvwxyz";
        int i = 0;
        while (i < length) {
            char singleChar = possible.charAt((int) Math.floor(Math.random() * possible.length()));
            if (password.indexOf(singleChar) == -1) {
                password += singleChar;
                i++;
            }
        }
        return prefix + password;
    }
}
