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

package edu.si.services.beans.cameratrap;

import java.util.Objects;

/**
 * Simple POJO to hold the deployment concept information such as the concept ID and concept label from the deployment manifest, as well as
 * the parentObjectPid from Fedora that could be used during the find object operation.
 *
 * @author parkjohn
 */
public class DeploymentConceptInformation {

    private String deploymentId;
    private String conceptId;
    private String conceptLabel;
    private String parentObjectPid;

    /**
     * Constructor that takes in the deploymentId, conceptId, conceptLable, and parentObjectPid
     *
     * @param deploymentId deployment package identifier such as the directory name for the deployment package
     * @param conceptId concept identifier from the deployment manifest such as the projectId, subProjectId etc
     * @param conceptLabel concept name from the deployment manifest such as the projectName, subProjectName etc
     * @param parentObjectPid parent object PID from Fedora Repository for the given concept object from the manifest.
     */
    public DeploymentConceptInformation(String deploymentId, String conceptId, String conceptLabel, String parentObjectPid) {
        this.deploymentId = deploymentId;
        this.conceptId = conceptId;
        this.conceptLabel = conceptLabel;
        this.parentObjectPid = parentObjectPid;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getConceptLabel() {
        return conceptLabel;
    }

    public void setConceptLabel(String conceptLabel) {
        this.conceptLabel = conceptLabel;
    }

    public String getParentObjectPid() {
        return parentObjectPid;
    }

    public void setParentObjectPid(String parentObjectPid) {
        this.parentObjectPid = parentObjectPid;
    }

    @Override
    public String toString() {
        return "DeploymentConceptInformation{" +
                "deploymentID='" + deploymentId + '\'' +
                ", conceptId='" + conceptId + '\'' +
                ", conceptLabel='" + conceptLabel + '\'' +
                ", parentObjectPid='" + parentObjectPid + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeploymentConceptInformation that = (DeploymentConceptInformation) o;
        return Objects.equals(deploymentId, that.deploymentId) &&
                Objects.equals(conceptId, that.conceptId) &&
                Objects.equals(conceptLabel, that.conceptLabel) &&
                Objects.equals(parentObjectPid, that.parentObjectPid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deploymentId, conceptId, conceptLabel, parentObjectPid);
    }
}
