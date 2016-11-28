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

package edu.si.services.sidora.rest.batch.model.status;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;

/**
 * @author jbirkhimer
 */
@XmlRootElement(name = "Batch")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"batchDone", "correlationId", "parentPID", "resourceOwner", "resourceCount", "resourcesProcessed", "contentModel", "codebookPID", "resources"})
public class BatchStatus {


    @XmlElement(name = "BatchDone")
    private Boolean batchDone;

    @XmlElement(name = "CorrelationID")
    private String correlationId;

    @XmlElement(name = "ParentPID")
    private String parentPID;

    @XmlElement
    private String resourceOwner;

    @XmlElement(name = "ResourceCount")
    private Integer resourceCount;

    @XmlElement(name = "ResourcesProcessed")
    private Integer resourcesProcessed;

    @XmlElement
    private String contentModel;

    @XmlElement
    private String codebookPID;

    @XmlElementWrapper
    @XmlElement(name = "resource")
    private ArrayList<ResourceStatus> resources;

    public BatchStatus(){}

    public BatchStatus(String parentPID, String resourceOwner, String correlationId, Integer resourceCount, Integer resourcesProcessed, Boolean batchDone, String contentModel, String codebookPID, ArrayList<ResourceStatus> resources) {
        this.parentPID = parentPID;
        this.resourceOwner = resourceOwner;
        this.correlationId = correlationId;
        this.resourceCount = resourceCount;
        this.resourcesProcessed = resourcesProcessed;
        this.batchDone = batchDone;
        this.contentModel = contentModel;
        this.codebookPID = codebookPID;
        this.resources = resources;
    }

    public String getParentPID() {
        return parentPID;
    }

    public void setParentPID(String parentPID) {
        this.parentPID = parentPID;
    }

    public String getResourceOwner() {
        return resourceOwner;
    }

    public void setResourceOwner(String resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Integer getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(Integer resourceCount) {
        this.resourceCount = resourceCount;
    }

    public Integer getResourcesProcessed() {
        return resourcesProcessed;
    }

    public void setResourcesProcessed(Integer resourcesProcessed) {
        this.resourcesProcessed = resourcesProcessed;
    }

    public Boolean getBatchDone() {
        return batchDone;
    }

    public void setBatchDone(Boolean batchDone) {
        this.batchDone = batchDone;
    }

    public String getContentModel() {
        return contentModel;
    }

    public void setContentModel(String contentModel) {
        this.contentModel = contentModel;
    }

    public String getCodebookPID() {
        return codebookPID;
    }

    public void setCodebookPID(String codebookPID) {
        this.codebookPID = codebookPID;
    }

    public ArrayList<ResourceStatus> getResources() {
        return resources;
    }

    public void setResources(ArrayList<ResourceStatus> resources) {
        this.resources = resources;
    }
}
