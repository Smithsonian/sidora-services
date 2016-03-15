/*
 * Copyright 2015 Smithsonian Institution.
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

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author parkjohn
 */
public class InFlightCheckProcessor implements Processor{

    private static final Logger log = LoggerFactory.getLogger(InFlightCheckProcessor.class);

    @BeanInject
    private CameraTrapStaticStore cameraTrapStaticStore;

    /**
     * This method is called during the Fedora find object operation redelivery attempts to check if there is an existing in-flight
     * process for the given parentId and the deployment ID.  The deployment ID information is used to determine
     * which deployment is currently processing the parent objects.  The other deployment processes should wait
     * while the initial deployment is finishing up the first parent object creation.

     * @param exchange camel message exchange; requires DeploymentParentId and CamelFileParent set in the headers
     * @throws InterruptedException
     */
    @Override
    public void process(Exchange exchange) throws InterruptedException {
        Message in = exchange.getIn();
        String parentId = in.getHeader("DeploymentParentId", String.class);
        String deploymentId = in.getHeader("CamelFileParent", String.class);

        //check if CamelFileParent is set in the header
        if (deploymentId == null || deploymentId.length()==0){
            throw new IllegalArgumentException("CamelFileParent not found");
        }

        if (parentId!=null && !cameraTrapStaticStore.containsParentId(parentId)){
            cameraTrapStaticStore.addParentId(parentId, deploymentId);
        }

        String lockOwner = cameraTrapStaticStore.getInFlightParentIds().get(parentId);
        if (!deploymentId.equals(lockOwner)){
            waitWhileProcessing(parentId);
        }

    }

    /**
     * Checks the current status of in-flight static storage and uses while loop to enforce wait on the current thread if necessary
     *
     * @param parentId parent object identifier such as ProjectId or SubProjectId from the deployment manifest and it is used
     *                 to determine whether there is an existing in-flight process for the same parent identifier
     * @throws InterruptedException
     */
    private synchronized void waitWhileProcessing(String parentId) throws InterruptedException {

        boolean parentInProcess = cameraTrapStaticStore.containsParentId(parentId);

        //wait if there is another process running for the same parent identifier
        while(parentInProcess){
            log.debug("Current thread waiting due to in-flight process: " + parentId);
            //change sleeping behavior event driven or configurable
            Thread.sleep(2000);
            parentInProcess = cameraTrapStaticStore.containsParentId(parentId);

        }
    }
}

