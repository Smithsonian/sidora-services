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

package edu.si.services.sidora.rest.batch.routes;

import edu.si.services.fedorarepo.aggregators.PidAggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfOperationException;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.support.builder.Namespaces;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.apache.camel.LoggingLevel.*;

/**
 * @author jbirkhimer
 */
@Component
public class SidoraBatchRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.batch")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.batch");

    /**
     * <b>Called on initialization to build the routes using the fluent builder syntax.</b>
     * <p/>
     * This is a central method for RouteBuilder implementations to implement the routes using the Java fluent builder
     * syntax.
     *
     * @throws Exception can be thrown during configuration
     */
    @Override
    public void configure() throws Exception {

        Namespaces ns = new Namespaces();
        ns.add("objDatastreams", "http://www.fedora.info/definitions/1/0/access/");
        ns.add("findObjects", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("ri", "http://www.w3.org/2005/sparql-results#");
        ns.add("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        ns.add("fedora", "info:fedora/fedora-system:def/relations-external#");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("eac", "urn:isbn:1-931666-33-4");
        ns.add("mods", "http://www.loc.gov/mods/v3");
        ns.add("fgdc", "http://localhost/");
        ns.add("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        ns.add("dc", "http://purl.org/dc/elements/1.1/");
        ns.add("srw_dc", "info:srw/schema/1/dc-schema");

        onException(ConnectException.class)
                .useExponentialBackOff()
                .backOffMultiplier("{{batch.net.redelivery.backOffMultiplier}}")
                .redeliveryDelay("{{batch.net.redelivery.redeliveryDelay}}")
                .maximumRedeliveries("{{batch.net.redelivery.maximumRedeliveries}}")
                .retryAttemptedLogLevel(WARN)
                .retriesExhaustedLogLevel(WARN)
                .logExhaustedMessageHistory(true)
                .handled(true)
                .end();

        onException(SQLException.class, CannotGetJdbcConnectionException.class)
                //onException(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException.class)
                .useExponentialBackOff()
                .backOffMultiplier("{{batch.net.redelivery.backOffMultiplier}}")
                .redeliveryDelay("{{batch.net.redelivery.redeliveryDelay}}")
                .maximumRedeliveries("{{batch.net.redelivery.maximumRedeliveries}}")
                .retryAttemptedLogLevel(WARN)
                .retriesExhaustedLogLevel(WARN)
                .logExhaustedMessageHistory(true)
//                .handled(true)
                .end();

        onException(CxfOperationException.class)
                .onWhen(simple("${exception.statusCode} not in '200,201'"))
                .useExponentialBackOff()
                .backOffMultiplier("{{batch.cxf.redelivery.backOffMultiplier}}")
                .redeliveryDelay("{{batch.cxf.redelivery.redeliveryDelay}}")
                .maximumRedeliveries("{{batch.cxf.redelivery.maximumRedeliveries}}")
                .retryAttemptedLogLevel(WARN)
                .retriesExhaustedLogLevel(WARN)
                .logExhaustedMessageHistory(true)
                .handled(true)
                .end();



        from("direct:addResourceObjects").routeId("BatchProcessAddResourceObjects")//.errorHandler(noErrorHandler())
                .log(INFO, LOG_NAME, "${id}: Starting Batch Request - Add Resource Objects...")

                // removing headers making it hard to debug
                //.removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|CamelHttpUri|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod")
                .removeHeaders("*","resourceOwner|association|ds_sidora|ds_metadata|resourceFileList|codebookPID|parentId")

                .to("bean:batchRequestControllerBean?method=createCorrelationId")

                .log(INFO, LOG_NAME, "${id}: Batch Request CorrelationId: ${header.correlationId}")

                .setHeader(Exchange.HTTP_METHOD).simple("GET")

                .toD("${header.association}?headerFilterStrategy=#dropHeadersStrategy").id("httpGetAssociationDuringRequest")
                //.toD("cxfrs://${header.association}?headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("httpGetAssociationDuringRequest")

                .setHeader("contentModel").xpath("//content_model/text()", String.class, ns)

                /*
                .setHeader(Exchange.HTTP_METHOD).simple("GET")
                .setHeader("Content-Type").constant("text/xml")
                .setHeader("Accept").constant("application/xml")
                */

                .toD("${header.resourceFileList}").id("httpGetResourceList")
                //.toD("cxfrs://${header.resourceFileList}?pattern=InOut").id("httpGetResourceList")

                .setHeader("resourceXML").simple("${body}", String.class)

                .setHeader("resourceCount").xpath("count(//file)", Integer.class, ns, "resourceXML")

                .setBody(simple("${header.resourceXML}"))

                .log(DEBUG, LOG_NAME, "${id}: Body for split: ${body}")

                // add each resource to the camelBatchResources mysql table
                .split().xtokenize("//file", 'w', ns)
                    .log(DEBUG, LOG_NAME, "${id}:  Split Body: ${body}")

                    .setHeader("resourceFile").xpath("//file/text()", String.class, ns)
                    .setHeader("objDsLabel").xpath("string(//file/@originalname)", String.class, ns)

                    .to("sql:{{sql.insertResources}}").id("sqlInsertResources")

                    .log(DEBUG, LOG_NAME, "${id}: Batch Request ResourceFile: ${header.resourceFile} added")
                .end()

                .to("sql:{{sql.insertNewBatchRequest}}").id("sqlInsertNewBatchRequest")

                // send the xml response body
                .to("bean:responseControllerBean?method=batchRequestResponse")

                .removeHeader("resourceXML")

                .log(INFO, LOG_NAME, "${id}: Finished Batch Request - Add Resource Objects...");

        from("direct:requestStatus").routeId("BatchProcessRequestStatus")
                .log(INFO, LOG_NAME, "${id}: Starting Batch Request Status for CorrelationID: ${header.correlationId} and ParentID: ${header.parentId}...")

                //.to("bean:batchRequestControllerBean?method=checkStatus")

                .to("sql:{{sql.checkRequestStatus}}?outputHeader=batchRequest&outputType=SelectList")

                .to("sql:{{sql.checkResourceStatus}}?outputHeader=statusResponse&outputType=SelectList")

                .to("bean:responseControllerBean?method=batchStatus")
                .log(INFO, LOG_NAME, "${id}: Finished Batch Request Statusfor CorrelationID: ${header.correlationId} and ParentID: ${header.parentId}...");



        // route that process the Batch Requests by picking up new rows from the database
        // and when done processing then update the row to mark it as complete
        from("sql:{{sql.selectBatchRequest}}?onConsume={{sql.markRequestComplete}}").routeId("BatchProcessResources")//.autoStartup(false)
                .log(INFO, LOG_NAME, "${id}: Starting Batch Resources Processing...")

                .to("sql:{{sql.markRequestConsumed}}")

                // Setup the authorization header for http calls to fedora REST endpoint
                .to("bean:requestProcessor?method=setAuthorization")

                // setting up headers and body from sql polling consumer
                .to("bean:batchRequestControllerBean?method=processBatchRequest")

                // process associations information
                .to("direct:processAssociationInfo")

                .to("sql:{{sql.selectResources}}")

                .log(DEBUG, LOG_NAME, "${id} BatchProcessResources sql.selectResources headers: ${headers}\nBody: ${body}")

                .log(INFO, LOG_NAME, "${id}: Batch Request CorrelationId: ${header.correlationId}")

                .setHeader("CamelFedoraPid").simple("${header.parentId}")

                .log(INFO, LOG_NAME, "${id}: Batch Request Parent PID=${header.CamelFedoraPid},  Resource Count=${header.resourceCount}")

                // There may be several kinds of resources with zero or more instances.
                .split(simple("${body}"), new PidAggregationStrategy()) // pidAggregator stores pids to ${header.PIDAggregation}

                    .log(DEBUG, LOG_NAME, "${id}: Batch Process: Split Body: ${body}")

                    .log(DEBUG, LOG_NAME, "${id}: Batch Process CamelSplitSize - ${header.CamelSplitSize} CamelSplitIndex - ${header.CamelSplitIndex}")

                    .setHeader("resourceFile").simple("${body[resourceFile]}")

                    .setHeader("objDsLabel").simple("${body[objDsLabel]}")

                    .to("bean:batchRequestControllerBean?method=setPrimaryTitleLabel")

                    .to("sql:{{sql.markResourceConsumed}}")

                    .to("direct:addResource")

                    .setHeader("processCount").simple("${header.CamelSplitIndex}++")

                    .to("sql:{{sql.updateBatchProcessCount}}")

                    /*
                    .setHeader("request_complete").simple("${property.CamelSplitComplete}")
                    .to("sql:{{sql.markRequestComplete}}")
                     */

                    .log(INFO, LOG_NAME, "${id}: Created Resource - ${body}.")
                .end()

                .log(INFO, LOG_NAME, "${id}: Created Resource Objects - ${header.PIDAggregation}.")

                .log(INFO, LOG_NAME, "${id}: Finished Batch Resource Processing.");



        from("direct:processAssociationInfo").routeId("BatchProcessAssociationInformation")
                .log(INFO, LOG_NAME, "${id}: Start processing Association Info.")

                .setHeader(Exchange.HTTP_METHOD).simple("GET")
                .setBody(simple("${null}"))

                .toD("${header.association}?headerFilterStrategy=#dropHeadersStrategy").id("httpGetAssociation")
                //.toD("cxfrs://${header.association}?headerFilterStrategy=#dropHeadersStrategy").id("httpGetAssociation")

                .setHeader("associationXML").simple("${body}", String.class)

                .setHeader("contentModel").xpath("//content_model/text()", String.class, ns, "associationXML")

                .setHeader("dsID").xpath("//dsid/text()", String.class, ns, "associationXML")

                .setHeader("metadata_to_dc_XSL").xpath("//transform/text()", String.class, ns, "associationXML")

                .setBody(simple("${header.associationXML}", String.class))

                .to("xslt-saxon:file:config/xslt/BatchAssociationTitlePath.xsl")

                // header is an XSLT param
                .setHeader("titlePath").simple("${body}", String.class)

                .log(INFO, LOG_NAME, "${id}: Finished processing Association Info.");



        from("direct:addResource").routeId("BatchProcessAddResource")
                .log(INFO, LOG_NAME, "${id}: Started Batch Process: Add Resource...")

                .to("direct:setupMetadata")
                .to("direct:createNewResource")
                .to("direct:updateDCDatastream")
                .to("direct:addRelsExtDatastream")
                .to("direct:addMetadataDatastream")
                .to("direct:addSidoraDatastream")
                .to("direct:addOBJDatastream")

                // add thumbnail for audio resources.
                //The derivatives does not create thumbnails for audio resources
                .filter(simple("${header.contentModel} == 'islandora:sp-audioCModel'"))
                    .to("direct:addTNDatastream")
                .end()
                .process(exchange -> {
                    Message out = exchange.getIn();
                    log.info("debug here");
                })
                //.to("direct:addFITSDatastream")
                .to("direct:addRelationships")

                .setHeader("resource_complete").simple("true", Boolean.class)

                .to("sql:{{sql.markResourceComplete}}")

                .log(INFO, LOG_NAME, "${id}: Finished Batch Process: Add Resource...");



        from("direct:setupMetadata").routeId("BatchProcessSetupMetadata")
                .log(INFO, LOG_NAME, "${id}: Started setting up Metadata ...")

                // get metadata file and update title
                .setHeader(Exchange.HTTP_METHOD).simple("GET")
                .setBody(simple(""))

                // http call to get the metadata file
                .toD("${header.ds_metadata}?headerFilterStrategy=#dropHeadersStrategy").id("httpGetDataStreamFile")
                //.toD("cxfrs://${header.ds_metadata}?headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("httpGetDataStreamFile")
                .convertBodyTo(String.class)
                .process(exchange -> {
                    Message out = exchange.getIn();
                    log.info("debug here");
                })
                // Use the uploaded filename (objDsLabel) if no title info provided
                .to("xslt-saxon:file:config/xslt/BatchProcess_ManifestResource.xsl")

                // store the metadata with updated title
                .setHeader("ds_metadataXML").simple("${body}", String.class)

                // Get the titleLabel and stash in header
                .to("bean:batchRequestControllerBean?method=setTitleLabel")

                // Get the resource file extension and stash in header
                .to("bean:batchRequestControllerBean?method=setExtension")

                .log(DEBUG, LOG_NAME, "${id}: Batch Process Title/Label: ${header.titleLabel} For Resource: ${header.resourceFile}")

                .log(INFO, LOG_NAME, "${id}: Finished setting up Metadata ...");



        from("direct:createNewResource").routeId("BatchProcessCreateNewResource")
                .log(INFO, LOG_NAME, "${id}: Started creating new resource...")

                // Create a new resource object
                .setHeader(Exchange.HTTP_METHOD).simple("POST")
                .setHeader("Content-Type").constant("text/xml")
                .setBody(simple("${null}"))

                // Http call to create a new resource object
                .toD("{{si.fedora.host}}/objects/new?ownerId=${header.resourceOwner}&namespace=si&label=${header.titleLabel}&headerFilterStrategy=#dropHeadersStrategy").id("newFedoraObject")
                //.toD("cxfrs://{{si.fedora.host}}/objects/new?ownerId=${header.resourceOwner}&namespace=si&label=${header.titleLabel}&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("newFedoraObject")
                .convertBodyTo(String.class)
                .process(exchange -> {
                    Message out = exchange.getIn();
                    log.info("debug here");
                })
                .setHeader("CamelFedoraPid").simple("${body}", String.class)
                .process(exchange -> {
                    Message out = exchange.getIn();
                    log.info("debug here");
                })
                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, resource_created)")
                .to("sql:{{sql.updateResourceCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished creating new resource...");



        from("direct:updateDCDatastream").routeId("BatchProcessUpdateDCDatastream")
                .log(INFO, LOG_NAME, "${id}: Started processing DC ...")

                .setBody(simple("${header.ds_metadataXML}"))

                .log(DEBUG, LOG_NAME, "=============[ Original Metadata ]==============\n${body}")

                // the double transform is necessary to create a DC that matches what workbench produces
                // the first transform sanitizes the metadata stripping empty fields and cleaning up the mods for the next transform
                .toD("xslt:{{extract.mods.from.collection.xsl}}")

                .log(DEBUG, LOG_NAME, "=============[ Metadata after extract_mods_from_collection.xsl ]==============\n${body}")

                // transform that created the DC from sanitized metadata
                .toD("xslt-saxon:${header.metadata_to_dc_XSL}")

                .log(DEBUG, LOG_NAME, "=============[ Metadata after extract_mods_from_collection.xsl AND mods_to_dc.xsl/fgdc_to_dc.xsl ]==============\n${body}")

                .log(DEBUG, LOG_NAME, "${id}: DC XML -\n${body}")

                // Update dc datastream
                .setHeader(Exchange.HTTP_METHOD).simple("POST")

                .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/DC?headerFilterStrategy=#dropHeadersStrategy").id("updateDCDatastream")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/DC?headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("updateDCDatastream")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, ds_dc_created)")
                .to("sql:{{sql.updateDsDcCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished processing DC.");



        from("direct:addRelsExtDatastream").routeId("BatchProcessAddRelsExtDatastream")
                .log(INFO, LOG_NAME, "${id}: Started processing RELS-EXT ...")

                .to("velocity:file:config/templates/BatchResourceTemplate.vsl")

                // Add a datastream for RELS-EXT.
                .setHeader(Exchange.HTTP_METHOD).simple("POST")

                .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/RELS-EXT?controlGroup=X&dsLabel=RDF Statements about this object&mimeType=application/rdf+xml&versionable=false&headerFilterStrategy=#dropHeadersStrategy").id("addRels-ExtDatastream")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/RELS-EXT?controlGroup=X&dsLabel=RDF Statements about this object&mimeType=application/rdf+xml&versionable=false&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addRels-ExtDatastream")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, ds_relsExt_created)")
                .to("sql:{{sql.updateDsRelsExtCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished processing RELS-EXT.");



        from("direct:addMetadataDatastream").routeId("BatchProcessAddMetadataDatastream")
                .log(INFO, LOG_NAME, "${id}: Started processing ${header.dsID} ...")

                // Add a datastream for metadata.
                .setBody(simple("${header.ds_metadataXML}"))

                .log(DEBUG, LOG_NAME, "${id}: ${header.dsID} XML -\n${body}")

                .setHeader(Exchange.HTTP_METHOD).simple("POST")

                .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/${header.dsID}?mimeType=text/xml&controlGroup=X&dsLabel=${header.dsID} Record&headerFilterStrategy=#dropHeadersStrategy").id("addMetadataDatastream")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/${header.dsID}?mimeType=text/xml&controlGroup=X&dsLabel=${header.dsID} Record&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addMetadataDatastream")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, ds_metadata_created)")
                .to("sql:{{sql.updateDsMetadataCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished processing ${header.dsID}.");



        from("direct:addSidoraDatastream").routeId("BatchProcessAddSidoraDatastream")
                .log(INFO, LOG_NAME, "${id}: Started processing SIDORA Datastream ...")

                .setHeader(Exchange.HTTP_METHOD).simple("POST")
                .setBody(simple("${null}"))

                .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/SIDORA?dsLocation=${header.ds_sidora}&mimeType=text/xml&controlGroup=X&dsLabel=SIDORA&headerFilterStrategy=#dropHeadersStrategy").id("addSidoraDatastream")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/SIDORA?dsLocation=${header.ds_sidora}&mimeType=text/xml&controlGroup=X&dsLabel=SIDORA&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addSidoraDatastream")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, ds_sidora_created)")
                .to("sql:{{sql.updateDsSidoraCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished processing SIDORA Datastream ...");



        from("direct:addOBJDatastream").routeId("BatchProcessAddOBJDatastream")
                .log(INFO, LOG_NAME, "${id}: Started processing OBJ ...")

                //mainly for testing to resolve properties
                //.setHeader("resourceFile").simple("${header.resourceFile}")

                // Need the Mime Type for the OBJ datastream
                .toD("bean:batchRequestControllerBean?method=getMIMEType(*, ${header.resourceFile})")

                .log(INFO, LOG_NAME, "${id}: Batch Process: Found: ${header.dsMIME}")

                .setHeader(Exchange.HTTP_METHOD).simple("POST")

                .setBody(simple("${null}"))

                .log(DEBUG, LOG_NAME, "${id}: BatchProcessAddOBJDatastream objDsLabel: ${header.objDsLabel}")

                .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/OBJ?dsLocation=${header.resourceFile}&mimeType=${header.dsMIME}&controlGroup=M&dsLabel=${header.objDsLabel}&versionable=true&headerFilterStrategy=#dropHeadersStrategy").id("addObjDatastream")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/OBJ?dsLocation=${header.resourceFile}&mimeType=${header.dsMIME}&controlGroup=M&dsLabel=${header.objDsLabel}&versionable=true&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addObjDatastream")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, ds_obj_created)")
                .to("sql:{{sql.updateDsObjCreated}}").id("sqlUpdateDsObjCreated")

                .log(INFO, LOG_NAME, "${id}: Finished processing OBJ.");



        from("direct:addTNDatastream").routeId("BatchProcessAddTNDatastream")
                .log(INFO, LOG_NAME, "${id}: Batch Process: Audio Resource Found: ${header.dsMIME}")

                .setHeader(Exchange.HTTP_METHOD).simple("POST")

                .setBody(simple("${null}"))

                // Create a audio thumbnail TN.
                .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/TN?dsLocation={{audio.thumbnail.png}}&mimeType=image/jpg&controlGroup=M&dsLabel=TN&versionable=false&headerFilterStrategy=#dropHeadersStrategy").id("addTNDatastream")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/TN?dsLocation={{audio.thumbnail.png}}&mimeType=image/jpg&controlGroup=M&dsLabel=TN&versionable=false&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addTNDatastream")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, ds_tn_created)")
                .to("sql:{{sql.updateDsTnCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished processing TN.");



        from("direct:addRelationships").routeId("BatchProcessAddRelationships")
                .log(INFO, LOG_NAME, "${id}: Started processing Relationships for ${header.dsID}...")

                .setHeader(Exchange.HTTP_METHOD).simple("POST")
                .setBody(simple("${null}"))

                .filter(simple("${header.codebookPID}"))
                    // add codebook relationship
                    .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/relationships/new?predicate=info:fedora/fedora-system:def/relations-external#hasCodebook&subject=info:fedora/${header.CamelFedoraPid}&object=info:fedora/${header.codebookPID}&headerFilterStrategy=#dropHeadersStrategy").id("addCodebookRelationship")
                    //.toD("cxfrs://{{si.fedora.host}}/objects/${header.CamelFedoraPid}/relationships/new?predicate=info:fedora/fedora-system:def/relations-external #hasCodebook&subject=info:fedora/${header.CamelFedoraPid}&object=info:fedora/${header.codebookPID}&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addCodebookRelationship")

                    .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, codebook_relationship_created)")
                    .to("sql:{{sql.updateCodebookRelationshipCreated}}")
                .end()

                .setHeader(Exchange.HTTP_METHOD).simple("POST")
                .setBody(simple("${null}"))

                // add relationship to parent
                .toD("{{si.fedora.host}}/objects/${header.parentId}/relationships/new?predicate=info:fedora/fedora-system:def/relations-external#hasResource&subject=info:fedora/${header.parentId}&object=info:fedora/${header.CamelFedoraPid}&headerFilterStrategy=#dropHeadersStrategy").id("addRelationship")
                //.toD("cxfrs://{{si.fedora.host}}/objects/${header.parentId}/relationships/new?predicate=info:fedora/fedora-system:def/relations-external #hasResource&subject=info:fedora/${header.parentId}&object=info:fedora/${header.CamelFedoraPid}&headerFilterStrategy=#dropHeadersStrategy&pattern=InOut").id("addRelationship")

                .to("bean:batchRequestControllerBean?method=updateCreatedStatus(*, parent_child_resource_relationship_created)")
                .to("sql:{{sql.updateParentChildResourceRelationshipCreated}}")

                .log(INFO, LOG_NAME, "${id}: Finished processing Relationships for ${header.dsID}.");

    }
}
