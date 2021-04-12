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

package edu.si.services.sidora.cameratrap.validation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Intended to be used to hold the camera trap validation results information and to be used
 * along with the aggregation strategy EIP in the camel routes.  The CameraTrapValidationMessage class is
 * used in Camel Routes in XML DSL to create MessageBeans for holding the individual validation results.
 *
 * @author parkjohn
 */
public class CameraTrapValidationMessage implements Serializable{

    /**
     * Creates a new MessageBean object with the three parameters
     *
     * @param deploymentPackageId deployment package identifier such as the name from the deployment zip package
     * @param message validation results
     * @param validationSuccess boolean flag to indicate the validation status
     * @return new MessageBean object which holds the validation result information
     */
    public MessageBean createValidationMessage(String deploymentPackageId, String message, Boolean validationSuccess) {

        MessageBean messageBean = new MessageBean();

        messageBean.setDeploymentPackageId(deploymentPackageId);
        messageBean.setMessage(message);
        messageBean.setValidationSuccess(validationSuccess);

        return messageBean;
    }

    /**
     * Creates a new MessageBean object with the four parameters
     * @param deploymentPackageId deployment package identifier such as the name from the deployment zip package
     * @param resourceObjectPid resource identifier used to determine
     * @param message validation results
     * @param validationSuccess boolean flag to indicate the validation status
     * @return new MessageBean object which holds the validation result information
     */
    public MessageBean createValidationMessage(String deploymentPackageId, String resourceObjectPid, String message, Boolean validationSuccess) {

        MessageBean messageBean = new MessageBean();

        messageBean.setDeploymentPackageId(deploymentPackageId);
        messageBean.setMessage(message);
        messageBean.setResourceObjectPid(resourceObjectPid);
        messageBean.setValidationSuccess(validationSuccess);

        return messageBean;
    }

    /**
     * Inner class for the CameraTrapValidationMessage to hold the individual camera trap validation results
     *
     * @author parkjohn
     */
    public class MessageBean implements Serializable {

        private String deploymentPackageId;
        private String resourceObjectPid;
        private String message;
        private Boolean isValidationSuccess;

        public String getDeploymentPackageId() {
            return deploymentPackageId;
        }

        public void setDeploymentPackageId(String deploymentPackageId) {
            this.deploymentPackageId = deploymentPackageId;
        }

        public String getResourceObjectPid() {
            return resourceObjectPid;
        }

        public void setResourceObjectPid(String resourceObjectPid) {
            this.resourceObjectPid = resourceObjectPid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Boolean getValidationSuccess() {
            return isValidationSuccess;
        }

        public void setValidationSuccess(Boolean validationSuccess) {
            isValidationSuccess = validationSuccess;
        }

        @Override
        public String toString() {
            return "MessageBean{" +
                    "deploymentPackageId='" + deploymentPackageId + '\'' +
                    ", resourceObjectPid='" + resourceObjectPid + '\'' +
                    ", message='" + message + '\'' +
                    ", isValidationSuccess=" + isValidationSuccess +
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
            MessageBean that = (MessageBean) o;
            return Objects.equals(deploymentPackageId, that.deploymentPackageId) &&
                    Objects.equals(resourceObjectPid, that.resourceObjectPid) &&
                    Objects.equals(message, that.message) &&
                    Objects.equals(isValidationSuccess, that.isValidationSuccess);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deploymentPackageId, resourceObjectPid, message, isValidationSuccess);
        }
    }
}

