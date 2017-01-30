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

import javax.xml.bind.annotation.*;

/** POJO for Batch Requests
 * @author jbirkhimer
 */
public class BatchRequest {

    private String correlationId;
    private String parentId;
    private String resourceFileList;
    private String ds_metadata;
    private String ds_sidora;
    private String association;
    private String resourceOwner;
    private String codebookPID;
    private Integer resourceCount;
    private Integer processCount;
    private Boolean request_consumed;
    private Boolean request_complete;
    private String created;
    private String updated;

    public BatchRequest(){}

    public BatchRequest(String correlationId, String parentId, String resourceFileList, String ds_metadata, String ds_sidora, String association, String resourceOwner, String codebookPID, Integer resourceCount, Integer processCount, Boolean request_consumed, Boolean request_complete, String created, String updated) {
        this.correlationId = correlationId;
        this.parentId = parentId;
        this.resourceFileList = resourceFileList;
        this.ds_metadata = ds_metadata;
        this.ds_sidora = ds_sidora;
        this.association = association;
        this.resourceOwner = resourceOwner;
        this.codebookPID = codebookPID;
        this.resourceCount = resourceCount;
        this.processCount = processCount;
        this.request_consumed = request_consumed;
        this.request_complete = request_complete;
        this.created = created;
        this.updated = updated;
    }

    @XmlElement(name = "CorrelationID")
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @XmlElement(name = "ParentPID")
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getResourceFileList() {
        return resourceFileList;
    }

    public void setResourceFileList(String resourceFileList) {
        this.resourceFileList = resourceFileList;
    }

    public String getDs_metadata() {
        return ds_metadata;
    }

    public void setDs_metadata(String ds_metadata) {
        this.ds_metadata = ds_metadata;
    }

    public String getDs_sidora() {
        return ds_sidora;
    }

    public void setDs_sidora(String ds_sidora) {
        this.ds_sidora = ds_sidora;
    }

    public String getAssociation() {
        return association;
    }

    public void setAssociation(String association) {
        this.association = association;
    }

    public String getResourceOwner() {
        return resourceOwner;
    }

    public void setResourceOwner(String resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public String getCodebookPID() {
        return codebookPID;
    }

    public void setCodebookPID(String codebookPID) {
        this.codebookPID = codebookPID;
    }

    public Integer getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(Integer resourceCount) {
        this.resourceCount = resourceCount;
    }

    public Integer getProcessCount() {
        return processCount;
    }

    public void setProcessCount(Integer processCount) {
        this.processCount = processCount;
    }

    public Boolean getRequest_consumed() {
        return request_consumed;
    }

    public void setRequest_consumed(Boolean request_consumed) {
        this.request_consumed = request_consumed;
    }

    public Boolean getRequest_complete() {
        return request_complete;
    }

    public void setRequest_complete(Boolean request_complete) {
        this.request_complete = request_complete;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return "BatchRequest{" +
                "correlationId='" + correlationId + '\'' +
                ", parentId='" + parentId + '\'' +
                ", resourceFileList='" + resourceFileList + '\'' +
                ", ds_metadata='" + ds_metadata + '\'' +
                ", ds_sidora='" + ds_sidora + '\'' +
                ", association='" + association + '\'' +
                ", resourceOwner='" + resourceOwner + '\'' +
                ", codebookPID='" + codebookPID + '\'' +
                ", resourceCount=" + resourceCount +
                ", processCount=" + processCount +
                ", request_consumed=" + request_consumed +
                ", request_complete=" + request_complete +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                '}';
    }
}
