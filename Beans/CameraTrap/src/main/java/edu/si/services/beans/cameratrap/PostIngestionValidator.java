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

import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * The post ingestion validator is used in the camera trap routes to perform validations after Fedora ingest process runs
 * on the deployments.
 *
 * @author parkjohn
 */
public class PostIngestionValidator {

    private static final Logger log = LoggerFactory.getLogger(PostIngestionValidator.class);

    /**
     * validateDatastreamExists() is used to compare the datastreams to check against the found datastream IDs from the Fedora repository.
     * For example, a deployment concept object is expected to contain DC, MANIFEST, FGDC, RELS-EXT datastreams.
     *
     * @param datastreamTypesCheck (required) The expected DSIDs to be found in comma separated value format.
     *                      This value is passed in from the message header.  It has to be a non-null/non-empty String value.
     * @param fedoraDatastreamIDsFound (required) The actual DSIDs found from the Fedora repository for the given Fedora Object.
     *                      This value is passed in from the message header and has to be a non-null/non-empty String value.
     * @return returns true if the all expected DSIDs are found; false if not all DSIDs are found.
     *
     */
    public boolean validateDatastreamExists(@Header(value="DatastreamTypesCheck") String datastreamTypesCheck,
                                            @Header(value="FedoraDatastreamIDsFound") String fedoraDatastreamIDsFound) {

        log.debug("DatastreamTypesCheck header {}", datastreamTypesCheck);
        log.debug("FedoraDatastreamIDsFound header {}", fedoraDatastreamIDsFound);

        if (datastreamTypesCheck == null || datastreamTypesCheck.trim().length()==0){
            throw new IllegalArgumentException("DatastreamTypesCheck header is empty");
        }

        if (fedoraDatastreamIDsFound == null || fedoraDatastreamIDsFound.trim().length()==0){
            throw new IllegalArgumentException("FedoraDatastreamIDsFound header is empty");
        }

        List<String> datastreamTypesCheckList = Arrays.asList(datastreamTypesCheck.trim().split("\\s*,\\s*"));
        List<String> foundDatastreamsList = Arrays.asList(fedoraDatastreamIDsFound.trim().split("\\s*,\\s*"));

        return datastreamTypesCheckList.containsAll(foundDatastreamsList) && foundDatastreamsList.containsAll(datastreamTypesCheckList);
    }
}
