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

import edu.si.services.camel.edan.producer.*;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Edan endpoint.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "edan", title = "Edan", syntax="edan:service", producerOnly = true, label = "Edan Sidora")
public class EdanEndpoint extends DefaultEndpoint {

    private static final Logger log = LoggerFactory.getLogger(EdanEndpoint.class);

    private EdanConfiguration edanConfiguration;

    @UriPath(defaultValue = "search", enums = "search,getContent,editContent,createContent,delete", description = "The EDAN Action to be performed.")
    @Metadata(required = true)
    private String edanService;


    /**
     * /metadata/v2.0/metadata/search.htm
     *
     * Params:
     *  facet
     *  fqs
     *  linkedContent
     *  profile
     *  q
     *  rows
     *  sortDir
     *  sart
     *
     *
     *  /content/v2.0/content/getContent.htm Params
     *
     * Params
     *  url
     *  id
     *  linkedContent
     *  linkedContentType
     */
    @UriParam(defaultValue = "false", description = "Include query facets in the results.")
    private String facet;

    @UriParam(description = "Filter query params.")
    private String fqs;

    //@UriParam(description = "A linked content option.", defaultValue = "true") //also a getContent param
    @UriParam(defaultValue = "[]", description = "A JSON array of EDAN search requests formatted with Solr join syntax. Each JSON object requires a \"name\" property.")
    private String linkedContent;

    @UriParam(defaultValue = "search", description = "The search profile.")
    private String profile;

    @UriParam(description = "The search term.")
    private String q;

    @UriParam(defaultValue = "10", defaultValueNote = "Size range: 0-100", description = "The number of rows returned.")
    private String rows;

    @UriParam(defaultValue = "asc", defaultValueNote = "Allowed values: asc, desc", description = "The sort direction.")
    private String sortDir;

    @UriParam(description = "The offset to start the results from.", defaultValue = "0")
    private String start;

    @UriParam(description = "The url of the content. Either the url or id fields are required.")
    private String url;

    @UriParam(description = "The id of the content. Either the url or id fields are required.")
    private String edanId;

    @UriParam(defaultValue = "[]", description = "The linked content type of the content")
    private String linkedContentType;

    @UriParam(description = "the content to create or edit. Must be JSON.")
    private String content;

    @UriParam(description = "The type of the content.")
    private String type;

    /**
     * Create a FcrepoEndpoint with a uri, path and component
     * @param uri the endpoint uri (without path values)
     * @param edanComponent an existing component value
     * @param edanConfiguration configuration settings for this endpoint
     */
    public EdanEndpoint(String uri, EdanComponent edanComponent, EdanConfiguration edanConfiguration) {
        super(uri, edanComponent);
        this.edanConfiguration = edanConfiguration;
    }

    public Producer createProducer() throws Exception {
        log.debug(toString());

        if (edanService.equals("search")) {
            return new EdanSearchProducer(this, getEdanConfiguration(), EdanApiConstants.EDAN_SEARCH_PATH);
        } else if (edanService.equals("getContent")) {
            return new EdanGetContentProducer(this, getEdanConfiguration(), EdanApiConstants.EDAN_GET_CONTENT_PATH);
        } else if (edanService.equals("getAdminContent")) {
            return new EdanGetAdminContentProducer(this, getEdanConfiguration(), EdanApiConstants.EDAN_GET_ADMN_CONTENT_PATH);
        } else if (edanService.equals("editContent")) {
            return new EdanEditContentProducer(this, getEdanConfiguration(), EdanApiConstants.EDAN_EDIT_CONTENT_PATH);
        } else if (edanService.equals("createContent")) {
            return new EdanCreateContentProducer(this, getEdanConfiguration(), EdanApiConstants.EDAN_CREATE_CONTENT_PATH);
        } else if (edanService.equals("delete")) {
            return new EdanDeleteContentProducer(this, getEdanConfiguration(), EdanApiConstants.EDAN_DELETE_PATH);
        }

        throw new IllegalArgumentException("Cannot create producer with type " + edanService);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new RuntimeCamelException("Edan Component cannot start a route.");
    }

    /**
     * Define the component as a singleton
     *
     * @return whether the endpoint is implemented as a singleton.
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /*@Override
    protected String createEndpointUri() {
        // Make sure it's properly encoded
        return "edan:" + UnsafeUriCharactersEncoder.encode(query);
    }*/

    /**
     * Some description of this option, and what it does
     * @param edanConfiguration
     */
    public void setEdanConfiguration(EdanConfiguration edanConfiguration) {
        this.edanConfiguration = edanConfiguration;
    }

    public EdanConfiguration getEdanConfiguration() {
        return edanConfiguration;
    }

    public void setEdanService(String edanService) {
        this.edanService = edanService;
    }

    public String getEdanService() {
        return edanService;
    }

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public String getFqs() {
        return fqs;
    }

    public void setFqs(String fqs) {
        this.fqs = fqs;
    }

    public String getLinkedContent() {
        return linkedContent;
    }

    public void setLinkedContent(String linkedContent) {
        this.linkedContent = linkedContent;
    }

    public String getLinkedContentType() {
        return linkedContentType;
    }

    public void setLinkedContentType(String linkedContentType) {
        this.linkedContentType = linkedContentType;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getRows() {
        return rows;
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEdanId() {
        return edanId;
    }

    public void setEdanId(String edanId) {
        this.edanId = edanId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "EdanEndpoint{" +
                "edanConfiguration=" + edanConfiguration.toString() +
                ", edanService='" + edanService + '\'' +
                ", facet='" + facet + '\'' +
                ", fqs='" + fqs + '\'' +
                ", linkedContent='" + linkedContent + '\'' +
                ", profile='" + profile + '\'' +
                ", q='" + q + '\'' +
                ", rows='" + rows + '\'' +
                ", sortDir='" + sortDir + '\'' +
                ", start='" + start + '\'' +
                ", url='" + url + '\'' +
                ", edanId='" + edanId + '\'' +
                ", linkedContentType='" + linkedContentType + '\'' +
                ", content='" + content + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public CloseableHttpClient createClient() {
        log.info("Creating EdanApiBean Http Client");
        return edanConfiguration.createClient();
    }
}
