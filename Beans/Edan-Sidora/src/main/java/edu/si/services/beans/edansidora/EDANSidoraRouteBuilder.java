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

package edu.si.services.beans.edansidora;

import edu.si.services.fedorarepo.FedoraObjectNotFoundException;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.io.File;
import java.net.SocketException;
import java.util.Base64;

/**
 * @author jbirkhimer
 */
public class EDANSidoraRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.edanIds")
    static private String LOG_NAME;

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    @PropertyInject(value = "si.fedora.password")
    private String fedoraPasword;

    @Override
    public void configure() throws Exception {
        Namespaces ns = new Namespaces("atom", "http://www.w3.org/2005/Atom");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("fs", "info:fedora/fedora-system:def/model#");
        ns.add("fedora-types", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("objDatastreams", "http://www.fedora.info/definitions/1/0/access/");
        ns.add("ri", "http://www.w3.org/2005/sparql-results#");
        ns.add("findObjects", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        ns.add("fedora", "info:fedora/fedora-system:def/relations-external#");
        ns.add("eac", "urn:isbn:1-931666-33-4");
        ns.add("mods", "http://www.loc.gov/mods/v3");
        ns.add("atom", "http://www.w3.org/2005/Atom");
        ns.add("sidora", "http://oris.si.edu/2017/01/relations#");

        //Exception handling
        onException(Exception.class, HttpOperationFailedException.class, SocketException.class)
                .onWhen(exchangeProperty(Exchange.TO_ENDPOINT).regex("^(http|https|cxfrs?(:http|:https)|http4):.*"))
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.ct.edanIds.redeliveryDelay}}")
                .maximumRedeliveries("{{min.edan.http.redeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logNewException(true)
                .setHeader("error").simple("[${routeId}] :: EdanIds Error reported: ${exception.message}")
                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}").id("logEdanIdsHTTPException");

        onException(EdanIdsException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.ct.edanIds.redeliveryDelay}}")
                .maximumRedeliveries("{{min.edan.redeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                //.logExhausted(true)
                .setHeader("error").simple("[${routeId}] :: EdanIds Error reported: ${exception.message}")
                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}").id("logEdanIdsException");

        //Retries for all exceptions after response has been sent
        onException(edu.si.services.fedorarepo.FedoraObjectNotFoundException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay(1000)
                .maximumRedeliveries(10)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true)
                .setHeader("error").simple("[${routeId}] :: EdanIds Error reported: ${exception.message}")
                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}");


        from("activemq:queue:{{edanIds.queue}}").routeId("EdanIdsStartProcessing")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting processing ...").id("logStart")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Fedora Authorization: ${header.Authorization}")

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders|CamelSchematronValidationReport|datastreamValidationXML")

                //Route Messages to the correct destination
                .choice()
                    .when().simple("${header.addEdanIds}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Camera Trap Ingest Message Found: ProjectId: ${header.ProjectId}, SiteId: ${header.SiteId}, SitePID: ${header.SitePID}")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Camera Trap Ingest Message Headers: ${headers}")
                        .to("direct:processCtDeployment").id("startProcessCtDeployment")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished processing Camera Trap Ingest Message")
                    .endChoice()
                    .otherwise()
                        .setHeader("origin").xpath("/atom:entry/atom:author/atom:name", String.class, ns)
                        .setHeader("dsID").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsID']/@term", String.class, ns)
                        .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)

                        .filter()
                            .simple("${header.origin} == '{{si.fedora.user}}' || " +
                                    " ${header.dsID} != 'OBJ' || " +
                                    " ${header.dsLabel} contains 'Observations' || " +
                                    " ${header.methodName} not in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest,purgeDatastream' || " +
                                    " ${header.pid} not contains '{{si.ct.namespace}}:'")

                            .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Filtered [ Origin=${header.origin}, PID=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel} ] - No message processing required.").id("logFilteredMessage")
                            .stop()
                        .end()

                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Fedora Message Found: Origin=${header.origin}, PID=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel}")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Processing Body: ${body}")
                        .to("direct:processFedoraMessage").id("startProcessingFedoraMessage")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished processing EDAN and IDS record update for PID: ${header.pid}.")
                    .endChoice()
                .end();

        from("direct:processFedoraMessage").routeId("EdanIdsProcessFedoraMessage")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting Fedora Message processing...")

                //Is this an Update or Delete
                .choice()
                    .when().simple("${headers.methodName} == 'purgeDatastream'")
                        .to("direct:edanDelete").id("processFedoraEdanDelete")
                    .endChoice()
                    .otherwise()
                        .to("direct:edanUpdate").id("processFedoraEdanUpdate")
                    .endChoice()
                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Fedora Message processing...");

        from("direct:edanUpdate").routeId("edanUpdate")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Update...")

                //Grab the objects datastreams as xml
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetDatastreams")

                //Get the label so we know what the imageid for image we are working with
                .setHeader("imageid").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns).id("setImageid")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: label: ${header.imageid}")

                /* Let check the speciesScientificName in the deployment manifest to see if we even need to continue processing */
                //Find the parent so we can grab the manifest
                .to("direct:findParentObject").id("processFedoraFindParentObject")

                //Grab the manifest from the parent
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.parentPid}/datastreams/MANIFEST/content")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetManifestDatastream")

                .setHeader("ManifestXML").simple("${body}", String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Manifest: ${header.ManifestXML}")

                //Filter out images that have speciesScientificName that are in the excluded list
                .filter().xpath("//ImageSequence[Image[ImageId/text() = $in:imageid]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.edanids.speciesScientificName.filter'), text())] != ''", "ManifestXML")
                    .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: NO POST TO EDAN for this PID: ${header.pid}, ImageId: ${header.imageid}")
                    .stop()
                .end()

                // Check EDAN to see if a record exists for this image id using the following service endpoint and query params
                .setHeader("edanServiceEndpoint").simple("/metadata/v2.0/metadata/search.htm")
                .setBody().simple("[\"p.emammal_image.image.id:${header.imageid}\"]")
                .setHeader(Exchange.HTTP_QUERY).groovy("\"fqs=\" + URLEncoder.encode(request.getBody(String.class));")
                .to("direct:edanHttpRequest").id("updateEdanSearchRequest")

                //Convert string JSON so we can access the data
                .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                //.unmarshal().json(JsonLibrary.Jackson, EdanSearch.class) //converts json to EdanSearch java object
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds:Body Type After Unmarshal from JSON= ${body}")

                //Are we editing or creating the EDAN record
                .choice()
                    .when().simple("${body[rowCount]} >= 1 && ${body[rows].get(0)[content][image][id]} == ${header.imageid}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: EDAN record found for ImageId: ${header.imageid}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Edit Content...")

                        //Headers needed for EDAN edit record
                        .setHeader("edanId").simple("${body[rows].get(0)[id]}")
                        .setHeader("edanTitle").simple("${body[rows].get(0)[title]}")
                        .setHeader("edanType").simple("${body[rows].get(0)[type]}")

                        //Headers needed for IDS push
                        .setHeader("SiteId").simple("${body[rows].get(0)[content][deployment_id]}")
                        //.setHeader("idsId").simple("${body[rows].get(0)[content][image][online_media].get(0)[idsId]}")

                        // create the EDAN content and encode
                        .to("direct:createEdanJsonContent")

                        // Set the EDAN endpoint and query params
                        .setHeader("edanServiceEndpoint", simple("/content/v1.1/admincontent/editContent.htm"))
                        .setHeader(Exchange.HTTP_QUERY).simple("id=${header.edanId}&content=${header.edanJson}")
                        .to("direct:edanHttpRequest").id("edanUpdateEditContent")

                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Edit Content...")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: EDAN record not found for ImageId: ${header.imageid}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Create Content...")

                        // create the EDAN content and encode
                        .to("direct:createEdanJsonContent")

                        // Set the EDAN endpoint and query params
                        .setHeader("edanServiceEndpoint", simple("/content/v1.1/admincontent/createContent.htm"))
                        .setHeader(Exchange.HTTP_QUERY).simple("content=${header.edanJson}")
                        .to("direct:edanHttpRequest").id("edanUpdateCreateContent")

                        //Convert string JSON so we can access the data
                        .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                        //.unmarshal().json(JsonLibrary.Jackson, EdanSearch.class) //converts json to EdanSearch java object
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds:Body Type After Unmarshal from JSON= ${body}")

                        //Get the edanId from the EDAN createRecord response
                        .setHeader("edanId").simple("${body[id]}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: EDAN record created for ImageId: ${header.imageid}, EDAN ID: ${header.edanId}")

                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Create Content...")
                    .endChoice()
                .end()

                //Asset XML atributes indicating serve to IDS (pub)
                .setHeader("isPublic").simple("Yes")
                .setHeader("isInternal").simple("No")

                // add the asset and create/append the asset xml
                .to("direct:idsAssetUpdate").id("fedoraUpdateIdsAssetUpdate")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Update...");

        from("direct:edanDelete").routeId("edanDelete")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Delete Record...")

                // Check EDAN to see if a record exists for the pid using the following service endpoint and query params
                .setHeader("edanServiceEndpoint").simple("/metadata/v2.0/metadata/search.htm")
                .setBody().simple("[\"p.emammal_image.image.online_media.sidorapid:${header.pid.replace(':', '?')}\"]")
                .setHeader(Exchange.HTTP_QUERY).groovy("\"fqs=\" + URLEncoder.encode(request.getBody(String.class));")
                .to("direct:edanHttpRequest").id("deleteEdanSearchRequest")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN metadata search Response Body:\n${body}")

                //Convert string JSON so we can access the data
                .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                //.unmarshal().json(JsonLibrary.Jackson, EdanSearch.class) //converts json to EdanSearch java object
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds:Body Type After Unmarshal from JSON= ${body}")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Body After Unmarshal from JSON= ${body}").id("edanDeleteUnmarshal")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Search Found PID=${body[rows].get(0)[content][image][online_media].get(0)[sidoraPid]}")

                //Are we editing or creating the EDAN record
                .choice()
                    .when().simple("${body[rowCount]} >= 1 && ${body[rows].get(0)[content][image][online_media].get(0)[sidoraPid]} == ${header.pid}")

                        .setHeader("edanSearchRows").simple("${body[rows]}")

                        .filter().simple("${body[rowCount]} > 1")
                            .log(LoggingLevel.WARN, LOG_NAME, "${id} EdanIds: Edan Delete: Search Found Multiple PID's!!! Each PID will be Deleted from EDAN!!!")
                        .end()

                        .split().simple("${body[rows]}")
                            .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: EDAN record found for PID: ${body[content][image][online_media].get(0)[sidoraPid]}, SiteId: ${body[content][deployment_id]}, imageid: ${body[content][image][id]}")
                            .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Release Record...")

                            //Headers needed for EDAN delete record
                            .setHeader("edanId").simple("${body[id]}")
                            .setHeader("edanType").simple("${body[type]}")

                            //Headers needed for IDS delete
                            .setHeader("imageid").simple("${body[content][image][id]}")
                            .setHeader("SiteId").simple("${body[content][deployment_id]}")

                            // Set the EDAN endpoint and query params
                            .setHeader("edanServiceEndpoint").simple("/content/v1.1/admincontent/releaseContent.htm")
                            .setHeader(Exchange.HTTP_QUERY).simple("id=${header.edanId}&type=${header.edanType}")
                            .to("direct:edanHttpRequest").id("deleteEdanRecordRequest")

                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN Delete Record Response Body:\n${body}")

                            //Asset XML attributes indicating delete asset
                            .setHeader("isPublic").simple("No")
                            .setHeader("isInternal").simple("No")

                            // add the asset and create/append the asset xml
                            .to("direct:idsAssetUpdate").id("fedoraDeleteIdsAssetUpdate")

                            .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Release Record...")
                        .end()
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.ERROR, LOG_NAME, "${id} EdanIds: EDAN record not found for PID: ${header.pid}")
                    .endChoice()
                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Delete Record...");


        from("direct:edanHttpRequest").routeId("edanHttpRequest")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Http Request...")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN Request: HTTP_QUERY = ${header.CamelHttpQuery}")

                // Set the EDAN Authorization headers and preform the EDAN query request
                .to("bean:edanApiBean?method=sendRequest").id("edanApiSendRequest")
                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN Request Status: ${header.CamelHttpResponseCode}, Response: ${body}")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Http Request...");

        from("direct:createEdanJsonContent").routeId("createEdanJsonContent")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting Edan JSON Content creation...")

                // create JSON content for EDAN
                .setHeader("CamelFedoraPid", simple("${header.pid}"))
                .setHeader("extraJson").simple("{{si.ct.uscbi.extra_property}}")
                .setBody(simple("${header.ManifestXML}"))
                .to("xslt:file:{{karaf.home}}/Input/xslt/edan_Transform.xsl?saxon=true")
                .setHeader("edanJson").groovy("URLEncoder.encode(request.body, 'UTF-8')")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN JSON content created and encoded: ${header.edanJson}")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Edan JSON Content creation...");

        from("direct:idsAssetUpdate").routeId("idsAssetUpdate")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting IDS Asset Update...")
                .log(LoggingLevel.DEBUG, LOG_NAME, "si.ct.uscbi.idsPushLocation = {{si.ct.uscbi.idsPushLocation}}")
                .log(LoggingLevel.INFO, LOG_NAME, "SiteId: ${header.SiteId}, imageid: ${header.imageid}")

                //The IDS Asset Directory and XMl file name
                .setHeader("idsAssetName", simple("{{si.edu.idsAssetFilePrefix}}${header.SiteId}"))
                .setHeader("idsAssetImagePrefix", simple("{{si.edu.idsAssetImagePrefix}}"))

                // Grab the existing IDS asset xml file to append to if it exists.
                // This avoids overwriting assets that have been updated but not yet ingested by IDS
                // and avoids having many single asset dir's for IDS to ingest when possible
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();

                        String idsPushLoc = getContext().resolvePropertyPlaceholders("{{si.ct.uscbi.idsPushLocation}}");
                        String idsAssetName = out.getHeader("idsAssetName", String.class);

                        String resourceFilePath = idsPushLoc + idsAssetName + "/" + idsAssetName + ".xml";
                        log.debug("*****[ Resource File: {} ]*****", resourceFilePath);
                        File resourceFile = new File(resourceFilePath);
                        if (resourceFile.exists()) {
                            out.setBody(resourceFile, String.class);
                        } else {
                            out.setBody(null);
                        }
                    }
                })
                // TODO: we can get the filename with extension from the headers when getting the OBJ datastream
                // from fedora but we would need to do that before updating the Asset XML.
                .choice()
                    .when().simple("${body} != null") // the asset xml should be in the body if it exists already
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: IDS Asset XML file exists and will be appended.")
                        //IDS Asset XML exists so append
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Pre Appended Asset XML Body:\n${body}")
                        .toD("xslt:file:{{karaf.home}}/Input/xslt/idsAssets.xsl")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Post Appended Asset XML Body:\n${body}")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: IDS Asset XML file does not exists and will be created.")
                        //IDS Asset XML does not exist so create it
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Pre Create Asset XML Body:\n${body}")
                        .toD("velocity:file:{{karaf.home}}/Input/templates/ids_template.vsl")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Post Create Asset XML Body:\n${body}")
                    .endChoice()
                .end()

                //Save the IDS Asset XML to file system
                //.setHeader("CamelOverruleFileName", simple("${header.idsAssetName}.xml"))
                .toD("file:{{si.ct.uscbi.idsPushLocation}}/${header.idsAssetName}?fileName=${header.idsAssetName}.xml")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: IDS Asset XML saved to CamelFileNameProduced:${header.CamelFileNameProduced}")

                //Only copy images files if this is not a delete asset
                .choice()
                    .when().simple("${header.isPublic} == 'Yes' && ${header.isInternal} == 'No'")
                        //Grab the OBJ datastream of the image asset from fedora
                        .setHeader("CamelHttpMethod").constant("GET")
                        .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams/OBJ/content")
                        .removeHeader("CamelHttpQuery")

                        .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("idsAssetUpdateGetFedoraOBJDatastreamContent")

                        //Save the image asset to the file system alongside the asset xml
                        //TODO: we can get the filename with extension from the headers when getting the OBJ datastream from fedora
                        //.setHeader("CamelOverruleFileName", simple("emammal_image_${header.imageid}.JPG"))
                        .toD("file:{{si.ct.uscbi.idsPushLocation}}/${header.idsAssetName}?fileName=${header.idsAssetImagePrefix}${header.imageid}.JPG")
                        .delay(1500)
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Added IDS Asset File CamelFileNameProduced:${header.CamelFileNameProduced}")
                    .endChoice()
                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished IDS Asset Update...");

        from("direct:findParentObject").routeId("EdanIdsFindParentObject").errorHandler(noErrorHandler())
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting Find Parent Object...")
                .setBody().simple("SELECT ?subject FROM <info:edu.si.fedora#ri> WHERE { ?subject <info:fedora/fedora-system:def/relations-external#hasResource> <info:fedora/${header.pid}> .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Find Parent Query - ${body}")

                .setHeader("CamelHttpMethod", constant("GET"))
                //.setHeader(Exchange.HTTP_URI).simple("{{si.fuseki.endpoint}}")
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")

                .toD("htp4://{{si.fuseki.endpoint}}?headerFilterStrategy=#dropHeadersStrategy")
                .convertBodyTo(String.class)

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Find Parent Query Result - ${body}")

                /* Example Query Result
                <?xml version="1.0"?>
                <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                  <head>
                    <variable name="subject"/>
                  </head>
                  <results>
                    <result>
                      <binding name="subject">
                        <uri>info:fedora/test:1398</uri>
                      </binding>
                    </result>
                  </results>
                </sparql>
                */

                .setBody().xpath("//ri:boolean/text()", String.class, ns)

                .choice()
                    .when().simple("${body} == false")
                        .setBody().simple("The fedora parent object not found!!!")
                        .log(LoggingLevel.ERROR, LOG_NAME, "${id} EdanIds: ${body}")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                throw new FedoraObjectNotFoundException(exchange.getIn().getBody(String.class));
                            }
                        })
                .end()

                .setHeader("parentPid").xpath("substring-after(/ri:sparql/ri:results/ri:result[1]/ri:binding/ri:uri, 'info:fedora/')", String.class, ns)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Parent PID: ${header.parentPid}")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Find Parent Object...");

        from("direct:processCtDeployment").routeId("EdanIdsProcessCtDeployment")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting Camera Trap Deployment processing...")

                // use the list of pids that the ct ingest route created and we sent as part of the JMS message
                // We could also get the deployment RELS-EXT and do the same thing or validate the pids in the
                // PIDAggregation header created during the CT ingest against the deployment RELS-EXT pids
                .split().tokenize(",", "PIDAggregation")
                    .setHeader("pid").simple("${body}")
                    .choice()
                        // Make sure the pid is not an observation object
                        .when().simple("${body} not in '${header.ResearcherObservationPID},${header.VolunteerObservationPID},${header.ImageObservationPID}'")
                            .log(LoggingLevel.INFO, LOG_NAME, "Split Body: ${body}, CamelSplitIndex: ${header.CamelSplitIndex}, CamelSplitSize: ${header.CamelSplitSize}, CamelSplitComplete: ${header.CamelSplitComplete}")

                            //Grab the objects datastreams as xml
                            .setHeader("CamelHttpMethod").constant("GET")
                            .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams")
                            .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                            .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("ctProcessGetFedoraDatastream")

                            //Get the label so we know what the imageid for image we are working with
                            .setHeader("imageid").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns)
                            .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: label: ${header.imageid} for PID: ${header.pid}")

                            // edit/create the EDAN record
                            .to("direct:edanUpdate").id("ctProcessEdanUpdate")

                            // add the asset and create/append the asset xml
                            .to("direct:idsAssetUpdate").id("ctProcessIdsAssetUpdate")
                    .end()
                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Camera Trap Deployment processing...");
    }
}
