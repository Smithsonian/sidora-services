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

import javax.xml.bind.annotation.XmlType;

/** POJO for Batch Resource
 * @author jbirkhimer
 */
@XmlType(propOrder = {"file", "pid", "title", "resourceObjectCreated", "dsDcCreated", "dsRelsExtCreated", "dsDcMetadata", "dsObjCreated", "dsSidoraCreated", "parentChildRelationshipCreated", "codebookRelationshipCreated", "complete"})
public class BatchResource {

    private String correlationId;
    private String resourceFile;
    private String parentId;
    private String pid;
    private String contentModel;
    private String resourceOwner;
    private String titleLabel;
    private Boolean resource_created;
    private Boolean ds_relsExt_created;
    private Boolean ds_metadata_created;
    private Boolean ds_sidora_created;
    private Boolean ds_dc_created;
    private Boolean ds_obj_created;
    private Boolean ds_tn_created;
    private Boolean codebook_relationship_created;
    private Boolean parent_child_resource_relationship_created;
    private Boolean resource_consumed;
    private Boolean resource_complete;
    private String created_date;
    private String updated_date;

    public BatchResource(String correlationId, String resourceFile, String parentId, String pid, String contentModel, String resourceOwner, String titleLabel, Boolean resource_created, Boolean ds_relsExt_created, Boolean ds_metadata_created, Boolean ds_sidora_created, Boolean ds_dc_created, Boolean ds_obj_created, Boolean ds_tn_created, Boolean codebook_relationship_created, Boolean parent_child_resource_relationship_created, Boolean resource_consumed, Boolean resource_complete, String created_date, String updated_date) {
        this.correlationId = correlationId;
        this.resourceFile = resourceFile;
        this.parentId = parentId;
        this.pid = pid;
        this.contentModel = contentModel;
        this.resourceOwner = resourceOwner;
        this.titleLabel = titleLabel;
        this.resource_created = resource_created;
        this.ds_relsExt_created = ds_relsExt_created;
        this.ds_metadata_created = ds_metadata_created;
        this.ds_sidora_created = ds_sidora_created;
        this.ds_dc_created = ds_dc_created;
        this.ds_obj_created = ds_obj_created;
        this.ds_tn_created = ds_tn_created;
        this.codebook_relationship_created = codebook_relationship_created;
        this.parent_child_resource_relationship_created = parent_child_resource_relationship_created;
        this.resource_consumed = resource_consumed;
        this.resource_complete = resource_complete;
        this.created_date = created_date;
        this.updated_date = updated_date;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getResourceFile() {
        return resourceFile;
    }

    public void setResourceFile(String resourceFile) {
        this.resourceFile = resourceFile;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getContentModel() {
        return contentModel;
    }

    public void setContentModel(String contentModel) {
        this.contentModel = contentModel;
    }

    public String getResourceOwner() {
        return resourceOwner;
    }

    public void setResourceOwner(String resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public String getTitleLabel() {
        return titleLabel;
    }

    public void setTitleLabel(String titleLabel) {
        this.titleLabel = titleLabel;
    }

    public Boolean getResource_created() {
        return resource_created;
    }

    public void setResource_created(Boolean resource_created) {
        this.resource_created = resource_created;
    }

    public Boolean getDs_relsExt_created() {
        return ds_relsExt_created;
    }

    public void setDs_relsExt_created(Boolean ds_relsExt_created) {
        this.ds_relsExt_created = ds_relsExt_created;
    }

    public Boolean getDs_metadata_created() {
        return ds_metadata_created;
    }

    public void setDs_metadata_created(Boolean ds_metadata_created) {
        this.ds_metadata_created = ds_metadata_created;
    }

    public Boolean getDs_sidora_created() {
        return ds_sidora_created;
    }

    public void setDs_sidora_created(Boolean ds_sidora_created) {
        this.ds_sidora_created = ds_sidora_created;
    }

    public Boolean getDs_dc_created() {
        return ds_dc_created;
    }

    public void setDs_dc_created(Boolean ds_dc_created) {
        this.ds_dc_created = ds_dc_created;
    }

    public Boolean getDs_obj_created() {
        return ds_obj_created;
    }

    public void setDs_obj_created(Boolean ds_obj_created) {
        this.ds_obj_created = ds_obj_created;
    }

    public Boolean getDs_tn_created() {
        return ds_tn_created;
    }

    public void setDs_tn_created(Boolean ds_tn_created) {
        this.ds_tn_created = ds_tn_created;
    }

    public Boolean getCodebook_relationship_created() {
        return codebook_relationship_created;
    }

    public void setCodebook_relationship_created(Boolean codebook_relationship_created) {
        this.codebook_relationship_created = codebook_relationship_created;
    }

    public Boolean getParent_child_resource_relationship_created() {
        return parent_child_resource_relationship_created;
    }

    public void setParent_child_resource_relationship_created(Boolean parent_child_resource_relationship_created) {
        this.parent_child_resource_relationship_created = parent_child_resource_relationship_created;
    }

    public Boolean getResource_consumed() {
        return resource_consumed;
    }

    public void setResource_consumed(Boolean resource_consumed) {
        this.resource_consumed = resource_consumed;
    }

    public Boolean getResource_complete() {
        return resource_complete;
    }

    public void setResource_complete(Boolean resource_complete) {
        this.resource_complete = resource_complete;
    }

    public String getCreated_date() {
        return created_date;
    }

    public void setCreated_date(String created_date) {
        this.created_date = created_date;
    }

    public String getUpdated_date() {
        return updated_date;
    }

    public void setUpdated_date(String updated_date) {
        this.updated_date = updated_date;
    }
}
