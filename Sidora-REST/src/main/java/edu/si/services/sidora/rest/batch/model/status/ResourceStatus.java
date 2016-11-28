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

import javax.xml.bind.annotation.XmlType;

/**
 * @author jbirkhimer
 */
@XmlType(propOrder = {"file", "pid", "title", "resourceObjectCreated", "dsDcCreated", "dsRelsExtCreated", "dsMetadata", "dsObjCreated", "dsTnCreated", "dsSidoraCreated", "parentChildRelationshipCreated", "codebookRelationshipCreated", "complete"})
public class ResourceStatus {

    private String file;
    private String pid;
    private String title;
    private Boolean resourceObjectCreated;
    private Boolean dsDcCreated;
    private Boolean dsRelsExtCreated;
    private Boolean dsMetadata;
    private Boolean dsObjCreated;
    private Boolean dsTnCreated;
    private Boolean dsSidoraCreated;
    private Boolean parentChildRelationshipCreated;
    private Boolean codebookRelationshipCreated;
    private Boolean complete;

    public ResourceStatus(){}

    public ResourceStatus(String file, String pid, String title, Boolean resourceObjectCreated, Boolean dsDcCreated, Boolean dsRelsExtCreated, Boolean dsMetadata, Boolean dsObjCreated, Boolean dsTnCreated, Boolean dsSidoraCreated, Boolean parentChildRelationshipCreated, Boolean codebookRelationshipCreated, Boolean complete) {
        this.file = file;
        this.pid = pid;
        this.title = title;
        this.resourceObjectCreated = resourceObjectCreated;
        this.dsDcCreated = dsDcCreated;
        this.dsRelsExtCreated = dsRelsExtCreated;
        this.dsMetadata = dsMetadata;
        this.dsObjCreated = dsObjCreated;
        this.dsTnCreated = dsTnCreated;
        this.dsSidoraCreated = dsSidoraCreated;
        this.parentChildRelationshipCreated = parentChildRelationshipCreated;
        this.codebookRelationshipCreated = codebookRelationshipCreated;
        this.complete = complete;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getResourceObjectCreated() {
        return resourceObjectCreated;
    }

    public void setResourceObjectCreated(Boolean resourceObjectCreated) {
        this.resourceObjectCreated = resourceObjectCreated;
    }

    public Boolean getDsDcCreated() {
        return dsDcCreated;
    }

    public void setDsDcCreated(Boolean dsDcCreated) {
        this.dsDcCreated = dsDcCreated;
    }

    public Boolean getDsRelsExtCreated() {
        return dsRelsExtCreated;
    }

    public void setDsRelsExtCreated(Boolean dsRelsExtCreated) {
        this.dsRelsExtCreated = dsRelsExtCreated;
    }

    public Boolean getDsMetadata() {
        return dsMetadata;
    }

    public void setDsMetadata(Boolean dsMetadata) {
        this.dsMetadata = dsMetadata;
    }

    public Boolean getDsObjCreated() {
        return dsObjCreated;
    }

    public void setDsObjCreated(Boolean dsObjCreated) {
        this.dsObjCreated = dsObjCreated;
    }

    public Boolean getDsTnCreated() {
        return dsTnCreated;
    }

    public void setDsTnCreated(Boolean dsTnCreated) {
        this.dsTnCreated = dsTnCreated;
    }

    public Boolean getDsSidoraCreated() {
        return dsSidoraCreated;
    }

    public void setDsSidoraCreated(Boolean dsSidoraCreated) {
        this.dsSidoraCreated = dsSidoraCreated;
    }

    public Boolean getParentChildRelationshipCreated() {
        return parentChildRelationshipCreated;
    }

    public void setParentChildRelationshipCreated(Boolean parentChildRelationshipCreated) {
        this.parentChildRelationshipCreated = parentChildRelationshipCreated;
    }

    public Boolean getCodebookRelationshipCreated() {
        return codebookRelationshipCreated;
    }

    public void setCodebookRelationshipCreated(Boolean codebookRelationshipCreated) {
        this.codebookRelationshipCreated = codebookRelationshipCreated;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

}

