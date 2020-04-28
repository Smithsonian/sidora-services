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

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The post ingestion validator is used in the camera trap routes to perform validations after Fedora ingest process runs
 * on the deployments.
 *
 * @author parkjohn
 * @author jbirkhimer
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
        List<String> datastreamTypesCheckList = new ArrayList<>(Arrays.asList(datastreamTypesCheck.toUpperCase().trim().split("\\s*,\\s*")));
        List<String> foundDatastreamsList = new ArrayList<>(Arrays.asList(fedoraDatastreamIDsFound.toUpperCase().trim().split("\\s*,\\s*")));

        //ignore SIDORA datastream if found as it can be created by the workbench on some projects
        foundDatastreamsList.remove("SIDORA");

        return datastreamTypesCheckList.containsAll(foundDatastreamsList) && foundDatastreamsList.containsAll(datastreamTypesCheckList);
    }

    /**
     *  Used to compare the metadata fields of the deployment manifest to the datastream from the Fedora repository
     *  and create the validation message based on the result to be returned on the exchange body.
     * @param exchange the current exchange
     */
    public void validateField (Exchange exchange) {

        Namespaces ns = new Namespaces("fedora", "info:fedora/fedora-system:def/relations-external#");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("eac", "urn:isbn:1-931666-33-4");
        ns.add("mods","http://www.loc.gov/mods/v3");

        String fieldName;
        String deploymentPackageId;
        String message;
        CameraTrapValidationMessage.MessageBean messageBean = null;

        //Get exchange headers
        deploymentPackageId = exchange.getIn().getHeader("deploymentPackageId", String.class);
        String datastreamXML = exchange.getIn().getHeader("datastreamValidationXML", String.class);

        //Get the comma separated list of datastream and manifest and the xpaths for each field
        String[] validationList = exchange.getIn().getBody(String.class).split("\\|");

        //Field name from comma separated list
        fieldName = validationList[0];

        //datastream and manifest xPaths from comma separated list
        String datastreamXpath = validationList[1];
        String manifestXpath = validationList[2];

        //Use the xpath from the comma separated list to set the manifestField
        String manifestField = XPathBuilder
                .xpath(manifestXpath)
                .evaluate(exchange.getContext(),
                        exchange.getIn().getHeader("ManifestXML"));

        //Use the xPath from the comma separated list to set the datastreamField
        String datastreamField = XPathBuilder
                .xpath(datastreamXpath)
                //.namespace("eac", "urn:isbn:1-931666-33-4")
                .namespaces(ns)
                .evaluate(exchange.getContext(), datastreamXML);

        //Check if validation passed
        if (datastreamField.equals(manifestField)) {
            message = "Deployment Package ID - " + deploymentPackageId
                    + ", Message - " + fieldName + "  Field matches the Manifest Field. Validation passed...";

            //Create the validation message bean with validation message
            //messageBean = new CameraTrapValidationMessage().createValidationMessage(deploymentPackageId, message, true);

            log.debug(message);

        } else {
            message = "Deployment Package ID - " + deploymentPackageId
                    + ", Message - " + fieldName + " Field validation failed. "
                    + "Expected " + manifestField + " but found " + datastreamField + ".";

            //Create the validation message bean with validation message
            messageBean = new CameraTrapValidationMessage().createValidationMessage(deploymentPackageId, message, false);

            log.warn(message);
            exchange.getIn().setBody(messageBean);
        }
    }
}
