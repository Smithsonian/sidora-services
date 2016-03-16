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
