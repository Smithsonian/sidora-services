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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Intended to be used as a bean with static variable(s) to store information cross deployments
 * during the multi-threaded Camera Trap ingest processes.  For example, we use this bean to hold
 * parent hierarchical (Project, SubProject, Plot) object identifiers to determine whether delay is needed
 * to avoid duplicate parent object being created.
 *
 * @author parkjohn
 */
public class CameraTrapStaticStore {

    private static final Logger log = LoggerFactory.getLogger(CameraTrapStaticStore.class);

    //map used to hold parent ID information and the owner of the parent ID(s) to correlate during multi-thread processing
    private static final Map<String, String> inFlightParentIds = new HashMap<>(0);

    /**
     * Wrapper method to check parent ID exists in the static data structure that holds the in-flight parent IDs
     *
     * @param parentId parent object identifier such as ProjectId or SubProjectId from the deployment manifest
     * @return true or false
     */
    public synchronized boolean containsParentId(String parentId) {
        log.debug("The static store contains following parent IDs: " + inFlightParentIds.toString());
        return inFlightParentIds.containsKey(parentId);
    }

    /**
     * Wrapper method to add parent ID to the static data structure that holds the in-flight parent IDs
     *
     * @param parentId parent object identifier such as ProjectId or SubProjectId from the deployment manifest
     * @param deploymentId deployment package ID; this information used to determine who is the owner of the in-flight parent ID(s)
     */
    public synchronized void addParentId(String parentId, String deploymentId){
        log.debug("Adding parent Id: " + parentId );
        inFlightParentIds.put(parentId, deploymentId);
    }

    /**
     * Wrapper method to remove parent ID from the static data structure that holds the in-flight parent IDs
     *
     * @param parentId parent object identifier such as ProjectId or SubProjectId from the deployment manifest
     */
    public synchronized void removeParentId(String parentId){
        log.debug("Removing parent Id: " + parentId);
        inFlightParentIds.remove(parentId);
    }

    /**
     * Wrapper method to retrieve the static data structure that holds the in-flight parent IDs
     *
     * @return contains the parent object identifier(s) in a map
     */
    public synchronized Map<String, String> getInFlightParentIds() {
        return inFlightParentIds;
    }

    /**
     * Removes all ParentIds based on the passed i deploymentId.  (Reverse look up on the data structure map)
     *
     * @param deploymentId deployment package ID; mainly the package directory name used during the ingestion process
     */
    public synchronized void removeParentIdsByDeploymentId(String deploymentId) {

        final Iterator<Map.Entry<String, String>> iterator = inFlightParentIds.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, String> entry = iterator.next();
            if(entry.getValue().equals(deploymentId))
            {
                //removes parentId from the storage
                iterator.remove();
            }
        }
    }
}