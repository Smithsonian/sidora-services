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
 * Simple POJO to hold the deployment correlation information such as the concept ID and concept label from the deployment manifest, as well as
 * the parentObjectPid from Fedora that could be used during the find object operation.
 *
 * @author parkjohn
 */
public class DeploymentCorrelationInformation {

    private String deploymentPackageId;
    private String correlationId;
    private String correlationLabel;
    private String parentObjectPid;

    /**
     * Constructor that takes in the deploymentPackageId, correlationId, correlationLabel, and parentObjectPid
     *
     * @param deploymentPackageId deployment package identifier such as the directory name for the deployment package
     * @param correlationId concept identifier from the deployment manifest such as the projectId, subProjectId etc
     * @param correlationLabel concept name from the deployment manifest such as the projectName, subProjectName etc
     * @param parentObjectPid parent object PID from Fedora Repository for the given concept object from the manifest.
     */
    public DeploymentCorrelationInformation(String deploymentPackageId, String correlationId, String correlationLabel, String parentObjectPid) {
        this.deploymentPackageId = deploymentPackageId;
        this.correlationId = correlationId;
        this.correlationLabel = correlationLabel;
        this.parentObjectPid = parentObjectPid;
    }

    public String getDeploymentPackageId() {
        return deploymentPackageId;
    }

    public void setDeploymentPackageId(String deploymentPackageId) {
        this.deploymentPackageId = deploymentPackageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCorrelationLabel() {
        return correlationLabel;
    }

    public void setCorrelationLabel(String correlationLabel) {
        this.correlationLabel = correlationLabel;
    }

    public String getParentObjectPid() {
        return parentObjectPid;
    }

    public void setParentObjectPid(String parentObjectPid) {
        this.parentObjectPid = parentObjectPid;
    }

    @Override
    public String toString() {
        return "DeploymentCorrelationInformation{" +
                "deploymentPackageId='" + deploymentPackageId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", correlationLabel='" + correlationLabel + '\'' +
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
        DeploymentCorrelationInformation that = (DeploymentCorrelationInformation) o;
        return Objects.equals(deploymentPackageId, that.deploymentPackageId) &&
                Objects.equals(correlationId, that.correlationId) &&
                Objects.equals(correlationLabel, that.correlationLabel) &&
                Objects.equals(parentObjectPid, that.parentObjectPid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deploymentPackageId, correlationId, correlationLabel, parentObjectPid);
    }
}
