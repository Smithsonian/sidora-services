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

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

public class EdanApi {

    private static final Logger LOG = LoggerFactory.getLogger(EdanApi.class);

    private String server = null;
    private String app_id = null;
    private String edan_key = null;

    /**
     * int indicates the authentication type/level 0 for unsigned/trusted/T1
     * requests; (currently unused) 1 for signed/T2 requests; 2 for password based
     * (unused)
     * auth_type is only for future, this code is only for type 1
     */
    private int auth_type = 1;

    public String result_format = "json";

    /**
     * Constructor
     */
    public EdanApi(String server, String app_id, String edan_key, int auth_type) {
        build(server, app_id, edan_key, auth_type);
    }

    public EdanApi(String server, String app_id, String edan_key) {
        build(server, app_id, edan_key, 1);
    }

    private void build(String server, String app_id, String edan_key, int auth_type) {
        this.server = server;
        this.app_id = app_id;
        this.edan_key = edan_key;
        this.auth_type = auth_type;
        this.result_format = "json";
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

        LOG.info("encodeHeader toReturn: " + String.valueOf(toReturn));

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
     * Perform a http request
     *
     * @param uri     A string containing the querystring that gets appended to the
     *                service url
     * @param service The service name you are curling
     *                {metadataService,tagService,collectService}
     * @return Map<String, String>
     */
    public Map<String, String> sendRequest(String uri, String service) {
        return this.sendRequest(uri, service, false);
    }

    /**
     * Perform a http request
     *
     * @param originalUri A string containing the querystring that gets appended to the
     *                    service url
     * @param service     The service name you are curling
     *                    {metadataService,tagService,collectService}
     * @param post        boolean, defaults to false; on true $uri sent CURLOPT_POSTFIELDS
     */
    public Map<String, String> sendRequest(String originalUri, String service, boolean post) {

        System.setProperty("http.agent", "");
        String targetURL = this.server + service;
        targetURL = targetURL + "?" + originalUri;
        String uri = targetURL;

        LOG.info("uri = {}", uri);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet httpget = new HttpGet(uri);
        CloseableHttpResponse response;
        String result = null;
        String errorInfo = "";
        String errorName = "";

        try {
            Map<String, String> encodedHeaders = this.encodeHeader(originalUri);

            for (Map.Entry<String, String> entry : encodedHeaders.entrySet()) {
                httpget.addHeader(entry.getKey(), entry.getValue());
            }

            httpget.setHeader("Accept", "*/*");
            httpget.setHeader("Accept-Encoding", "identity");
            httpget.setHeader("User-Agent", "unknown");
            httpget.removeHeaders("Connection");

            LOG.info("httpGet headers: " + Arrays.toString(httpget.getAllHeaders()));

            response = client.execute(httpget);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
            response.close();
        } catch (ParseException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorName = e.toString();
            errorInfo = sw.toString();
        } catch (ClientProtocolException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorName = e.toString();
            errorInfo = sw.toString();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorName = e.toString();
            errorInfo = sw.toString();
        } catch (NoSuchAlgorithmException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorName = e.toString();
            errorInfo = sw.toString();
        }

        Map<String, String> toReturn = new HashMap<String, String>();
        toReturn.put("errorString", errorName);
        toReturn.put("stackTrace", errorInfo);
        toReturn.put("response", result);
        toReturn.put("targetURL", targetURL);

        LOG.info("toResponse: " + toReturn);

        return toReturn;
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

    /**
     * This section is for ease of testing while developing, fill in a server, appid, and key
     *
     * @param args
     */

    //MOVED TO UNIT TEST SEE src/test/java/edu.si.services.edansidora.EdanApiTest.java

    //@SuppressWarnings("deprecation")
    /*public static void main(String[] args) {
        System.out.println("Hello World");
        EdanApi ea = null;
        ea = new EdanApi("http://edan.si.edu/", "EMAMMAL", "6a1cdadc22351377ab87f39e7b990261db9b94f1");
        if (ea.app_id == "APPID") {
            System.out.println("Fill in server information in EdanApi.java source file in order to run a test.");
            return;
        }
        String sampleJsonContent = ""
                + "\n{"

                + "\n \"project_id\": \"p125\","
                + "\n \"project_name\": \"Sample Triangle Camera Trap Survey Project\","
                + "\n \"sub_project_id\": \"sp818\","
                + "\n \"sub_project_name\": \"Triangle Wild\","
                + "\n \"deployment_id\": \"d18981\","
                + "\n \"deployment_name\": \"RaleighBYO 416\","
                + "\n \"image_sequence_id\": \"d18981s2\","
                + "\n \"image\": {"
                + "\n   \"id\": \"emammal_ct:1696705\","
                + "\n   \"online_media\": {"
                + "\n     \"mediaCount\": 1,"
                + "\n     \"media\": ["
                + "\n       {"
                + "\n         \"content\": \"emammal_ct:1696705\","
                + "\n         \"idsId\": \"emammal_ct:1696705\","
                + "\n         \"type\": \"Images\","
                + "\n         \"caption\": \"Camera Trap Image White-tailed Deer\""
                + "\n       }"
                + "\n     ]"
                + "\n   },"
                + "\n   \"date_time\": \"2016-02-24 18:45:30\","
                + "\n   \"photo_type\": \"animal\","
                + "\n   \"photo_type_identified_by\": \"\","
                + "\n   \"interest_ranking\": \"None\""
                + "\n"
                + "\n },"
                + "\n \"image_identifications\": ["
                + "\n  {"
                + "\n    \"iucn_id\": \"42394\","
                + "\n    \"species_scientific_name\": \"Odocoileus virginianus\","
                + "\n    \"individual_animal_notes\": \"\","
                + "\n    \"species_common_name\": \"White-tailed Deer\","
                + "\n    \"count\": 1,"
                + "\n    \"age\": \"\","
                + "\n    \"sex\": \"\","
                + "\n    \"individual_id\": \"\","
                + "\n    \"animal_recognizable\": \"\""
                + "\n  }"
                + "\n ]"
                + "\n}";

        String sampleJson = "{\"type\":\"gg\",\"content\":" + sampleJsonContent + ",\"status\":1,\"title\":\"Emammal test\",\"publicSearch\":false}";
        String edanUri = "content=" + URLEncoder.encode(sampleJson).replaceAll("\\+", "%20");
        Map<String, String> ma = ea.sendRequest(edanUri, "content/v1.1/admincontent/createContent.htm");
        for (Map.Entry<String, String> entry : ma.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println("K:" + key + "\tval:" + value.replaceAll("\\\\n", "\n"));
        }
    }*/
}
