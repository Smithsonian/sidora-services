package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by jbirkhimer on 3/10/16.
 */
public class PostValidationBean {

    private static final Logger log = LoggerFactory.getLogger(PostValidationBean.class);
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
        fieldName = exchange.getIn().getHeader("FieldName", String.class);
        String[] validationXpaths = exchange.getIn().getHeader("validationXpaths", String.class).split(",");

        //log.debug(CT_LOG_NAME + ": " + "datastreamXML body = " + datastreamXML);
        //log.debug(CT_LOG_NAME + ": " + "validationXpaths header = " + Arrays.toString(validationXpaths));
        //log.debug(CT_LOG_NAME + ": " + "validationXpaths = " + Arrays.toString(validationXpaths));

        String datastreamXpath = validationXpaths[0];
        String manifestXpath = validationXpaths[1];
        String datastreamFieldExistsXpath = "exists(" + datastreamXpath + ")";

        String datastreamFieldExists = XPathBuilder
                .xpath(datastreamFieldExistsXpath, String.class)
                //.namespace("eac", "urn:isbn:1-931666-33-4")
                .namespaces(ns)
                .evaluate(exchange.getContext(), datastreamXML, String.class);

        log.debug(CT_LOG_NAME + ": " + "datastreamFieldExists = " + datastreamFieldExists + ":");

        //Set the datastreamFieldExists
        //exchange.getIn().setHeader("datastreamFieldExists", datastreamFieldExists);

        //log.debug(CT_LOG_NAME + ": " + "datastreamFieldExists header = " + exchange.getIn().getHeader("datastreamFieldExists", String.class));

        //Set the manifestField
        String manifestField = XPathBuilder
                .xpath(manifestXpath)
                .evaluate(exchange.getContext(),
                        exchange.getIn().getHeader("ManifestXML"),
                        String.class);

        //exchange.getIn().setHeader("manifestField", manifestField);

        log.debug(CT_LOG_NAME + ": " + "manifestField = " + manifestField + ":");

        //Set the datastreamField
        if (datastreamFieldExists.equals("true")) {

            //Set the datastreamField
            String datastreamField = XPathBuilder
                    .xpath(datastreamXpath)
                    //.namespace("eac", "urn:isbn:1-931666-33-4")
                    .namespaces(ns)
                    .evaluate(exchange.getContext(), datastreamXML);

            //exchange.getIn().setHeader("datastreamField", datastreamField);

            log.info(CT_LOG_NAME + ": " + fieldName + " Field exists validation passed");

            fieldMatchesManifest(datastreamField, manifestField);

        } else {
            message = "Deployment Package ID - " + camelFileParent
                    + ", Message - " + fieldName + " Field validation failed.\n"
                    +"Expected " + manifestField + " but found the" + fieldName + " Field does not exist.";
            log.info(CT_LOG_NAME + ": " + message);
        }

        exchange.getIn().setHeader("validationStatusMessage", "PostValidationErrors");
        exchange.getIn().setBody(message);

    }

    private void fieldMatchesManifest(String datastreamField, String manifestField) {

        if (datastreamField.equals(manifestField)) {
            log.info(CT_LOG_NAME + ": " + fieldName + " Field matches the Manifest Field. Validation passed...");
        } else {
            message = "Deployment Package ID - " + camelFileParent
                    + ", Message - " + fieldName + " Field validation failed.\n"
                    + "Expected " + manifestField + " but found " +datastreamField + ".";
            log.info(CT_LOG_NAME + ": " + message);
        }
    }
}
