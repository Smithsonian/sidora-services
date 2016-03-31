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

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel Processor used in the Camera Trap routes during onException to check for in-flight concept object creation for the
 * multithreaded processing to avoid duplicate concept object being created in Fedora Repository
 *
 * @author parkjohn
 */
public class InFlightConceptCheckProcessor implements Processor{

    private static final Logger log = LoggerFactory.getLogger(InFlightConceptCheckProcessor.class);

    @BeanInject
    private CameraTrapStaticStore cameraTrapStaticStore;

    /**
     * This method is called during the Fedora find object operation redelivery attempts to check if there is an existing in-flight
     * process for the given conceptId and the deployment ID.  The deployment ID information is used to determine
     * which deployment is currently processing the concept object(s).  The other deployment processes should wait
     * while the initial deployment is finishing up the first concept object(s) creation.

     * @param exchange camel message exchange; requires CamelFileParent, DeploymentConceptId, CamelFedoraLabel and CamelFedoraPid from the headers
     *                 for the inFlightCheck to add the concept information to the static storage
     * @throws InterruptedException
     */
    @Override
    public void process(Exchange exchange) throws InterruptedException {
        Message in = exchange.getIn();

        String deploymentId = in.getHeader("CamelFileParent", String.class);
        String conceptId = in.getHeader("DeploymentConceptId", String.class);
        String conceptLabel = in.getHeader("CamelFedoraLabel", String.class);
        String parentObjectPid = in.getHeader("CamelFedoraPid", String.class);

        //check if CamelFileParent is set in the header
        if (deploymentId == null || deploymentId.length()==0){
            throw new IllegalArgumentException("CamelFileParent not found");
        }

        if (conceptId!=null && conceptLabel!=null
                && parentObjectPid!=null && !cameraTrapStaticStore.containsConceptId(conceptId)){
            DeploymentConceptInformation conceptInformation = new DeploymentConceptInformation(deploymentId, conceptId, conceptLabel, parentObjectPid);
            cameraTrapStaticStore.addConceptId(conceptId, conceptInformation);
        }

        DeploymentConceptInformation lockOwner = cameraTrapStaticStore.getInFlightConceptIds().get(conceptId);
        if (lockOwner!=null && !deploymentId.equals(lockOwner.getDeploymentId())){
            waitWhileProcessing(conceptId);
        }

    }

    /**
     * Checks the current status of in-flight static storage and uses while loop to enforce wait on the current thread if necessary
     *
     * @param conceptId concept object identifier from the parent hierarchy such as the ProjectId or SubProjectId from the deployment manifest
     *                  and it is used to determine whether there is an existing in-flight process for the same concept identifier
     * @throws InterruptedException
     */
    private synchronized void waitWhileProcessing(String conceptId) throws InterruptedException {

        boolean conceptInProcess = cameraTrapStaticStore.containsConceptId(conceptId);

        //wait if there is another process running for the same concept identifier
        while(conceptInProcess){
            log.debug("Current thread waiting due to in-flight process: " + conceptId);
            //change sleeping behavior event driven or configurable
            Thread.sleep(6000);
            conceptInProcess = cameraTrapStaticStore.containsConceptId(conceptId);

        }
    }
}

