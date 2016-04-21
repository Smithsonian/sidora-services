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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Intended to be used as a singleton bean to store information across multiple deployments
 * during the multi-threaded Camera Trap ingest processes.  For example, we use this bean to hold
 * concept hierarchical (Project, SubProject, Plot) object identifiers to determine whether delay is needed
 * to avoid duplicate concept object being created.
 *
 * @author parkjohn
 */
public class CameraTrapStaticStore {

    private static final Logger log = LoggerFactory.getLogger(CameraTrapStaticStore.class);

    //map used to hold correlation ID(s) and the owner of the ID(s) to correlate during multi-thread processing
    private final Map<String, DeploymentCorrelationInformation> inFlightCorrelationIds = new HashMap<>(0);

    /**
     * Wrapper method to check correlation ID exists in the data structure that holds the in-flight correlation IDs
     *
     * @param correlationId correlation identifier such as ProjectId or SubProjectId from the deployment manifest
     * @return true or false
     */
    public synchronized boolean containsCorrelationtId(String correlationId) {
        log.debug("The cameratrap store contains following correlation IDs: " + inFlightCorrelationIds.toString());
        return inFlightCorrelationIds.containsKey(correlationId);
    }

    /**
     * Wrapper method to add correlation ID to the data structure that holds the in-flight correlation IDs
     *
     * @param correlationId correlation identifier such as ProjectId or SubProjectId from the deployment manifest
     * @param correlationInformation deployment package ID; this information used to determine who is the owner of the in-flight correlation ID(s)
     */
    public synchronized void addCorrelationId(String correlationId, DeploymentCorrelationInformation correlationInformation){
        log.debug("Adding correlation Id: " + correlationId );
        inFlightCorrelationIds.put(correlationId, correlationInformation);
    }

    /**
     * Wrapper method to remove correlation ID from the data structure that holds the in-flight correlation IDs
     *
     * @param correlationId correlation identifier such as ProjectId or SubProjectId from the deployment manifest
     */
    public synchronized void removeCorrelationId(String correlationId){
        log.debug("Removing correlation Id: " + correlationId);
        inFlightCorrelationIds.remove(correlationId);
    }

    /**
     * Wrapper method to get the value from the data structure that holds the DeploymentCorrelationInformation
     *
     * @param correlationId correlation identifier such as ProjectId or SubProjectId from the deployment manifest
     */
    public synchronized DeploymentCorrelationInformation getCorrelationInformationById(String correlationId){
        return inFlightCorrelationIds.get(correlationId);
    }

    /**
     * Wrapper method to retrieve the data structure that holds the in-flight correlation IDs
     *
     * @return contains the correlation identifier(s) in a map
     */
    public synchronized Map<String, DeploymentCorrelationInformation> getInFlightCorrelationIds() {
        return inFlightCorrelationIds;
    }

    /**
     * Removes all correlation Ids based on the passed in deploymentId.  (Reverse look up on the data structure map)
     *
     * @param deploymentId deployment package ID; mainly the package directory name used during the ingestion process
     */
    public synchronized void removeCorrelationIdsByDeploymentId(String deploymentId) {

        final Iterator<Map.Entry<String, DeploymentCorrelationInformation>> iterator = inFlightCorrelationIds.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, DeploymentCorrelationInformation> entry = iterator.next();
            if(entry.getValue().getDeploymentId().equals(deploymentId))
            {
                //removes correlationId from the storage
                iterator.remove();
            }
        }
    }

}