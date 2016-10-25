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

package edu.si.services.sidora.rest.batch.model;

/**
 * @author jbirkhimer
 */
public class BatchRequest {
    String correlationId;
    String parentId;
    String resourceZipFileURL;
    String metadataFileURL;
    String sidoraDatastreamFileURL;
    String contentModel;
    String resourceOwner;
    String titleField;
    Integer resourceCount;
    Integer processCount;
    Boolean complete;
    String created;
    String modified;


    /*final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
    final MyPojo pojo = mapper.convertValue(map, MyPojo.class);*/

    public String getCorrelationId() {
        return correlationId;
    }

    public BatchRequest setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public String getParentId() {
        return parentId;
    }

    public BatchRequest setParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getResourceZipFileURL() {
        return resourceZipFileURL;
    }

    public BatchRequest setResourceZipFileURL(String resourceZipFileURL) {
        this.resourceZipFileURL = resourceZipFileURL;
        return this;
    }

    public String getMetadataFileURL() {
        return metadataFileURL;
    }

    public BatchRequest setMetadataFileURL(String metadataFileURL) {
        this.metadataFileURL = metadataFileURL;
        return this;
    }

    public String getSidoraDatastreamFileURL() {
        return sidoraDatastreamFileURL;
    }

    public BatchRequest setSidoraDatastreamFileURL(String sidoraDatastreamFileURL) {
        this.sidoraDatastreamFileURL = sidoraDatastreamFileURL;
        return this;
    }

    public String getContentModel() {
        return contentModel;
    }

    public BatchRequest setContentModel(String contentModel) {
        this.contentModel = contentModel;
        return this;
    }

    public String getResourceOwner() {
        return resourceOwner;
    }

    public BatchRequest setResourceOwner(String resourceOwner) {
        this.resourceOwner = resourceOwner;
        return this;
    }

    public String getTitleField() {
        return titleField;
    }

    public BatchRequest setTitleField(String titleField) {
        this.titleField = titleField;
        return this;
    }

    public Integer getResourceCount() {
        return resourceCount;
    }

    public BatchRequest setResourceCount(Integer resourceCount) {
        this.resourceCount = resourceCount;
        return this;
    }

    public Integer getProcessCount() {
        return processCount;
    }

    public BatchRequest setProcessCount(Integer processCount) {
        this.processCount = processCount;
        return this;
    }

    public Boolean getComplete() {
        return complete;
    }

    public BatchRequest setComplete(Boolean complete) {
        this.complete = complete;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public BatchRequest setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getModified() {
        return modified;
    }

    public BatchRequest setModified(String modified) {
        this.modified = modified;
        return this;
    }
}
