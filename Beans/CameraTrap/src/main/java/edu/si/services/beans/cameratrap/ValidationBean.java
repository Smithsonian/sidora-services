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

import org.apache.camel.Exchange;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jbirkhimer on 3/10/16.
 */
public class ValidationBean {

    private static final Logger log = LoggerFactory.getLogger(ValidationBean.class);
    static final private String CT_LOG_NAME = "edu.si.ctingest";
    String fieldName = null;
    String camelFileParent = null;
    String message = null;

    public void validateField (Exchange exchange) {

        Namespaces ns = new Namespaces("fedora", "info:fedora/fedora-system:def/relations-external#");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("eac", "urn:isbn:1-931666-33-4");
        ns.add("mods","http://www.loc.gov/mods/v3");

        camelFileParent = exchange.getIn().getHeader("CamelFileParent", String.class);

        String datastreamXML = exchange.getIn().getHeader("datastreamValidationXML", String.class);

        String[] validationList = exchange.getIn().getBody(String.class).split(",");

        fieldName = validationList[0];
        String datastreamXpath = validationList[1];
        String manifestXpath = validationList[2];

        //Set the manifestField
        String manifestField = XPathBuilder
                .xpath(manifestXpath)
                .evaluate(exchange.getContext(),
                        exchange.getIn().getHeader("ManifestXML"),
                        String.class);

        //Set the datastreamField
        String datastreamField = XPathBuilder
                .xpath(datastreamXpath)
                        //.namespace("eac", "urn:isbn:1-931666-33-4")
                .namespaces(ns)
                .evaluate(exchange.getContext(), datastreamXML);

        if (datastreamField.equals(manifestField)) {
            message = "passed";

            log.info(CT_LOG_NAME + ": " + fieldName + " Field matches the Manifest Field. Validation passed...");

        } else {
            message = "Deployment Package ID - " + camelFileParent
                    + ", Message - " + fieldName + " Field validation failed."
                    + "Expected " + manifestField + " but found " +datastreamField + ".";

                    log.info(CT_LOG_NAME + ": " + message);
        }

        //exchange.getIn().setHeader("ValidationErrors", "ValidationErrors");
        exchange.getIn().setBody(message);

    }
}
