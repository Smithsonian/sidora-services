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

import edu.si.services.beans.edansidora.aggregation.EdanBulkAggregationStrategy;
import edu.si.services.beans.edansidora.aggregation.EdanIdsAggregationStrategy;
import edu.si.services.beans.edansidora.aggregation.IdsBatchAggregationStrategy;
import edu.si.services.beans.edansidora.model.IdsAsset;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.seda.SedaConsumer;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.concurrent.CamelThreadFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author jbirkhimer
 */
public class EDANSidoraRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.edanIds")
    static private String LOG_NAME;
    Marker logMarker = MarkerFactory.getMarker("edu.si.edanIds");

    @PropertyInject(value = "si.fedora.user")
    private String fedoraUser;

    @PropertyInject(value = "si.fedora.password")
    private String fedoraPasword;

    @PropertyInject(value = "si.ct.edanIds.parallelProcessing", defaultValue = "true")
    private String PARALLEL_PROCESSING;

    @PropertyInject(value = "si.ct.edanIds.concurrentConsumers", defaultValue = "20")
    private String CONCURRENT_CONSUMERS;

    @PropertyInject(value = "si.ct.edanIds.corePoolSize", defaultValue = "1")
    private String IDS_POOL_MIN;

    @PropertyInject(value = "si.ct.edanIds.maximumPoolSize", defaultValue = "5")
    private String IDS_POOL_MAX;

    @PropertyInject(value = "si.ct.edanIds.ct.deployment.queue.size", defaultValue = "5")
    private String CT_PROCESSING_QUEUE_SIZE;

    @PropertyInject(value = "si.ct.edanIds.xml2json.keepStrings", defaultValue = "false")
    private String KEEP_STRINGS;

    @PropertyInject(value = "si.ct.edanIds.edan.bulk.size", defaultValue ="1000")
    private String BATCH_SIZE;

    @PropertyInject(value = "si.ct.edanIds.edan.bulk.completionTimeout", defaultValue = "1000")
    private String COMPLETION_TIMEOUT;

    // convert xslt xml output to json and convert array elements to json array
    public Processor xml2JsonProcessor() {
        Processor xml2JsonProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message out = exchange.getIn();
                String xmlBody = out.getBody(String.class);
                try {

                    /**
                     * Convert a well-formed (but not necessarily valid) XML string into a
                     * JSONObject. Some information may be lost in this transformation because
                     * JSON is a data format and XML is a document format. XML uses elements,
                     * attributes, and content text, while JSON uses unordered collections of
                     * name/value pairs and arrays of values. JSON does not does not like to
                     * distinguish between elements and attributes. Sequences of similar
                     * elements are represented as JSONArrays. Content text may be placed in a
                     * "content" member. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code>
                     * are ignored.
                     *
                     * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
                     * numbers but will instead be the exact value as seen in the XML document.
                     *
                     * @param string The source string.
                     * @param keepStrings If true, then values will not be coerced into boolean
                     *  or numeric values and will instead be left as strings
                     * @return A JSONObject containing the structured data from the XML string.
                     * @throws JSONException Thrown if there is an errors while parsing the string
                     */
                    JSONObject xmlJSONObj = XML.toJSONObject(xmlBody, Boolean.parseBoolean(KEEP_STRINGS));

                    log.debug(logMarker, "xml 2 json Output:\n{}", xmlJSONObj);

                    if (Boolean.parseBoolean(out.getHeader("test_edan_url", String.class))) {

                        String existingEdanURL = out.getHeader("edanURL", String.class);

                        if (existingEdanURL != null && !existingEdanURL.isEmpty()) {
                            existingEdanURL = existingEdanURL.replaceAll("emammal_image:", "").replaceAll("emammal-image-", "");
                            log.warn(logMarker, "SETTING TEST URL = {}", existingEdanURL);
                            xmlJSONObj.getJSONObject("xml").put("url", existingEdanURL);
                        } else {
                            String edan_url = xmlJSONObj.getJSONObject("xml").getString("url");
                            String test_edan_url = edan_url + "_test_" + UUID.randomUUID().toString().replace("-", "");
                            log.warn(logMarker, "SETTING TEST URL = {}", test_edan_url);
                            xmlJSONObj.getJSONObject("xml").put("url", test_edan_url);
                        }

                    }

                    JSONObject edan_content = xmlJSONObj.getJSONObject("xml").getJSONObject("content");
                    log.debug(logMarker, "edan_content json before online_media and image_identifications array fix:\n{}", edan_content.toString(4));

                    //convert online_media to json array
                    JSONObject image = edan_content.getJSONObject("image");
                    JSONObject online_media = image.getJSONObject("online_media");
                    Object online_media_array_element = online_media.get("online_media_array_element");
                    if (online_media_array_element instanceof JSONObject) {
                        JSONArray online_media_replace = new JSONArray();
                        online_media_replace.put(online_media_array_element);
                        edan_content.getJSONObject("image").put("online_media", online_media_replace);
                    } else if (online_media_array_element instanceof JSONArray) {
                        edan_content.getJSONObject("image").put("online_media", online_media_array_element);
                    }

                    //log.debug(logMarker, "json after online_media fix:\n{}", edan_content.getJSONObject("image").getJSONArray("online_media").toString(4));

                    //convert image_identifications to json array
                    JSONObject image_identifications = edan_content.getJSONObject("image_identifications");
                    Object image_identifications_array_element = image_identifications.get("image_identifications_array_element");
                    if (image_identifications_array_element instanceof JSONObject) {
                        JSONArray image_identifications_replace = new JSONArray();
                        image_identifications_replace.put(image_identifications_array_element);
                        edan_content.getJSONObject("image").put("image_identifications", image_identifications_replace);
                    } else if (image_identifications_array_element instanceof JSONArray) {
                        edan_content.put("image_identifications", image_identifications_array_element);
                    }

                    //log.debug(logMarker, "json after image_identifications fix:\n{}", edan_content.getJSONArray("image_identifications").toString(4));

                    log.debug(logMarker, "json final edan_content :\n{}", xmlJSONObj.getJSONObject("xml").toString(4));
                    out.setBody(xmlJSONObj.getJSONObject("xml").toString());
                } catch (JSONException je) {
                    throw new EdanIdsException("Error creating edan json", je);
                }
            }
        };

        return xml2JsonProcessor;
    }

    public Processor assetListProcessor() {
        Processor assetListProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message out = exchange.getIn();

                List<IdsAsset> idsAssetList = new ArrayList<>();

                IdsAsset asset = new IdsAsset();
                asset.setImageid(out.getHeader("imageid", String.class));
                asset.setIsPublic(out.getHeader("isPublic", String.class));
                asset.setIsInternal(out.getHeader("isInternal", String.class));
                asset.setPid(out.getHeader("pid", String.class));
                asset.setSiteId(out.getHeader("SiteId", String.class));

                idsAssetList.add(asset);

                out.setHeader("idsAssetList", idsAssetList);
            }
        };

        return assetListProcessor;
    }

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
                .logNewException(true);
//                .setHeader("error").simple("[${routeId}] :: EdanIds Error reported: ${exception.message}");
//                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}").id("logEdanIdsHTTPException");

        onException(EdanIdsException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.ct.edanIds.redeliveryDelay}}")
                .maximumRedeliveries("{{min.edan.redeliveries}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true);
//                .setHeader("error").simple("[${routeId}] :: EdanIds Error reported: ${exception.message}");
//                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}").id("logEdanIdsException");

        //Retries for all exceptions after response has been sent
        onException(edu.si.services.fedorarepo.FedoraObjectNotFoundException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay(1000)
                .maximumRedeliveries(10)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true);
//                .setHeader("error").simple("[${routeId}] :: EdanIds Error reported: ${exception.message}");
//                .log(LoggingLevel.ERROR, LOG_NAME, "${header.error}\nCamel Headers:\n${headers}");

        ThreadFactory threadFactory = new CamelThreadFactory("Camel (EdanIdsCamelContext) thread ##counter# - #name#", "customSplit", true);
        ThreadPoolExecutor customThreadPoolExecutor = new ThreadPoolExecutor(Integer.parseInt(IDS_POOL_MIN), Integer.parseInt(IDS_POOL_MAX), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);

        from("activemq:queue:{{edanIds.queue}}").routeId("EdanIdsStartProcessingFedoraMessage")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting processing ...").id("logStart")
                .setHeader("fedoraAtom").simple("${body}", String.class)
                .log(LoggingLevel.DEBUG, "${id} EdanIds: JMS Body: ${body}")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Fedora Authorization: ${header.Authorization}")

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders|CamelSchematronValidationReport|datastreamValidationXML")

                .setHeader("origin").xpath("/atom:entry/atom:author/atom:name", String.class, ns)
                .setHeader("dsID").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsID']/@term", String.class, ns)
                .setHeader("dsLabel").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLabel']/@term", String.class, ns)

                .filter()
                    .simple("${header.origin} == '{{si.fedora.user}}'")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Filtered CT USER!! [ Origin=${header.origin}, PID=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel} ] - No message processing required!").id("logFilteredMessage")
                        .stop()
                .end()

                .filter()
                    .simple("${header.methodName} == 'purgeObject'")
                        .setHeader("imageid").simple("${header.dsLabel}")
                        .to("seda:edanDelete?waitForTaskToComplete=Always").id("processFedoraEdanDelete")
                        .stop()
                .end()

                //Grab the objects datastreams as xml
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetDatastreams")

                .choice()
                    .when().simple("${body} != null || ${body} != ''")
                        .setHeader("objLabel").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns).id("setobjLabel")
                        .setHeader("mimeType").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@mimeType", String.class, ns).id("setMimeType")
                    .endChoice()
                .end()

                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams/RELS-EXT/content")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")
                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetRELS-EXT")

                .choice()
                    .when().simple("${body} != null || ${body} != ''")
                        .setHeader("hasModel").xpath("string-join(//fs:hasModel/@rdf:resource, ',')", String.class, ns).id("setHasModel")
                    .endChoice()
                .end()

                .filter()
                    .simple("${header.origin} == '{{si.fedora.user}}' || " +
                            " ${header.dsID} != 'OBJ' || " +
                            " ${header.dsLabel} contains 'Observations' || " +
                            " ${header.objLabel} contains 'Observations' || " +
                            " ${header.dsLabel} == null || " +
                            " ${header.objLabel} == null || " +
                            " ${header.dsLabel} in ',null' || " +
                            " ${header.objLabel} in ',null' || " +
                            " ${header.methodName} not in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest,purgeDatastream' || " +
                            " ${header.pid} not contains '{{si.ct.namespace}}:' || " +
                            " ${header.mimeType} not contains 'image' || " +
                            " ${header.hasModel?.toLowerCase()} not contains 'image'")

                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Filtered [ Origin=${header.origin}, PID=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel}, objLabel=${header.objLabel}, objMimeType=${header.mimeType}, hasModel=${header.hasModel} ] - No message processing required!").id("logFilteredMessage")
                        .stop()
                .end()

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Fedora Message Found: Origin=${header.origin}, PID=${header.pid}, Method Name=${header.methodName}, dsID=${header.dsID}, dsLabel=${header.dsLabel}, objLabel=${header.objLabel}, objMimeType=${header.mimeType}, hasModel= ${header.hasModel}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Processing Body: ${body}")

                .to("seda:processFedoraMessage?waitForTaskToComplete=Never").id("startProcessingFedoraMessage")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished processing EDAN and IDS record update for PID: ${header.pid}.");


        from("seda:processFedoraMessage?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("EdanIdsProcessFedoraMessage")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting Fedora Message processing...")

                //Find the parent so we can grab the manifest
                .to("seda:findParentObject?waitForTaskToComplete=Always").id("processFedoraFindParentObject")

                //Grab the manifest from the parent
                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.parentPid}/datastreams/MANIFEST/content")
                .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("processFedoraGetManifestDatastream")

                .setHeader("ManifestXML").simple("${body}", String.class)
                //.log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Manifest: ${header.ManifestXML}")

                //Headers needed for IDS push
                .setHeader("SiteId").xpath("//CameraDeploymentID", String.class, ns, "ManifestXML")

                .setHeader("imageid").simple("${header.dsLabel}").id("setImageid")

                //Filter out images that have speciesScientificName that are in the excluded list
                //check the speciesScientificName in the deployment manifest to see if we even need to continue processing
                .filter().xpath("//ImageSequence[Image[ImageId/text() = $in:imageid]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.edanids.speciesScientificName.filter'), text())] != ''", "ManifestXML")
                    .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: NO POST TO EDAN for this PID: ${header.pid}, ImageId: ${header.imageid}")
                    .stop()
                .end()

                .to("seda:edanUpdate?waitForTaskToComplete=Never").id("processFedoraEdanUpdate")

                //Asset XML atributes indicating serve to IDS (pub)
                .setHeader("isPublic").simple("Yes")
                .setHeader("isInternal").simple("No")

                .process(assetListProcessor())

                .log(LoggingLevel.DEBUG, "*******************************[ processFedoraMessage Calling idsAssetUpdate ]*******************************")

                // add the asset and create/append the asset xml
                .to("seda:idsAssetImageUpdate").id("fedoraUpdateIdsAssetUpdate")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Fedora Message processing...");

        from("seda:edanUpdate?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("edanUpdate")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Update...")

                // Check EDAN to see if a record exists for this image id using the following service endpoint and query params
                .setHeader("edanServiceEndpoint").simple("/metadata/v2.0/metadata/search.htm")

                //Search EDAN for imageId, deploymentId, app_id, and type
                .setBody().simple("p.emammal_image.image.id:${header.imageid} AND p.emammal_image.deployment_id:${header.SiteId} AND app_id:{{si.ct.uscbi.appId}} AND type:{{si.ct.uscbi.edan_type}}")
                .setHeader(Exchange.HTTP_QUERY).groovy("\"q=\" + URLEncoder.encode(request.getBody(String.class));")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds:\nEdanSearch Query:\n${header.CamelHttpQuery}")

                .to("seda:edanHttpRequest?waitForTaskToComplete=Always").id("updateEdanSearchRequest")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds:\nEdanSearch result:\n${body}")

                //Convert string JSON so we can access the data
                .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                //.unmarshal().json(JsonLibrary.Jackson, EdanSearch.class) //converts json to EdanSearch java object
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds:Body After Unmarshal from JSON= ${body}")

                //Are we editing or creating the EDAN record
                .choice()
                    .when().simple("${body[rowCount]} >= 1 && ${body[rows].get(0)[content][image][id]} == ${header.imageid}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Edit Content for ImageId: ${header.imageid}")

                        //Headers needed for EDAN edit record
                        .setHeader("edanId").simple("${body[rows].get(0)[id]}")
                        .setHeader("edanURL").simple("${body[rows].get(0)[url]}")
                        .setHeader("edanTitle").simple("${body[rows].get(0)[title]}")
                        .setHeader("edanType").simple("${body[rows].get(0)[type]}")

                        // create the EDAN content and encode
                        .to("seda:createEdanJsonContent?waitForTaskToComplete=Always")

                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();

                                // is bulk edan request enabled
                                if (getContext().resolvePropertyPlaceholders("{{si.ct.batchEnabled}}").equals("true")) {
                                    JSONObject edanRecordEdit = new JSONObject();
                                    edanRecordEdit.put("id", out.getHeader("edanId"));
                                    edanRecordEdit.put("content", out.getBody());
                                    out.setBody(edanRecordEdit);
                                } else {
                                    String id = out.getHeader("edanId", String.class);
                                    String content = out.getHeader("edanJson", String.class);
                                    out.setHeader(Exchange.HTTP_QUERY, "id="+ id + "&content=" + content);
                                }

                            }
                        })

                        // Set the EDAN endpoint and query params
                        .setHeader("edanServiceEndpoint", simple("/content/v1.1/admincontent/editContent.htm"))
                        .to("seda:edanHttpRequest?waitForTaskToComplete=Always").id("edanUpdateEditContent")

                        //Get the edan id and url from the EDAN response
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String body = out.getBody(String.class);
                                if (body != null) {
                                    JSONObject edanResponse = new JSONObject(body);
                                    String edanId = edanResponse.getString("id");
                                    String edanUrl = edanResponse.getString("url");
                                    out.setHeader("edanId", edanId);
                                    out.setHeader("edanUrl", edanUrl);
                                }
                            }
                        })

                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Finished EditContent Request ${header.edanServiceEndpoint} for ImageId: ${header.imageid}, EDAN id: ${header.edanId}, url: ${header.edanUrl}, Status: ${header.CamelHttpResponseCode}, Response: ${body}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EditContent Request ${header.edanServiceEndpoint} for ImageId: ${header.imageid}, EDAN id: ${header.edanId}, url: ${header.edanUrl}")
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Create Content for ImageId: ${header.imageid}")

                        // create the EDAN content and encode
                        .to("seda:createEdanJsonContent?waitForTaskToComplete=Always")

                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();

                                // is bulk edan request enabled
                                if (getContext().resolvePropertyPlaceholders("{{si.ct.batchEnabled}}").equals("true")) {
                                    JSONObject edanRecordCreate = new JSONObject();
                                    edanRecordCreate.put("content", out.getBody());
                                    out.setBody(edanRecordCreate);
                                } else {
                                    String content = out.getHeader("edanJson", String.class);
                                    out.setHeader(Exchange.HTTP_QUERY, "content=" + content);
                                }
                            }
                        })

                        // Set the EDAN endpoint and query params
                        .setHeader("edanServiceEndpoint", simple("/content/v1.1/admincontent/createContent.htm"))
                        .to("seda:edanHttpRequest?waitForTaskToComplete=Always").id("edanUpdateCreateContent")

                        //Get the edan id and url from the EDAN response
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String body = out.getBody(String.class);
                                if (body != null) {
                                    JSONObject edanResponse = new JSONObject(body);
                                    String edanId = edanResponse.getString("id");
                                    String edanUrl = edanResponse.getString("url");
                                    out.setHeader("edanId", edanId);
                                    out.setHeader("edanUrl", edanUrl);
                                }
                            }
                        })

                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Finished CreateContent Request ${header.edanServiceEndpoint} for ImageId: ${header.imageid}, EDAN id: ${header.edanId}, url: ${header.edanUrl}, Status: ${header.CamelHttpResponseCode}, Response: ${body}")
                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished CreateContent Request ${header.edanServiceEndpoint} for ImageId: ${header.imageid}, EDAN id: ${header.edanId}, url: ${header.edanUrl}")
                    .endChoice()
                .end()

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Finished EDAN Update...");

        from("seda:edanDelete?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("edanDelete")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting EDAN Delete Record...")

                // Check EDAN to see if a record exists for the pid using the following service endpoint and query params
                .setHeader("edanServiceEndpoint").simple("/metadata/v2.0/metadata/search.htm")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message out = exchange.getIn();
                        String sidoraPid = out.getHeader("pid", String.class).replace(":", "?");
                        out.setHeader("sidoraPid", sidoraPid);
                    }
                })

                //Search EDAN for imageId, deploymentId, app_id, and type
                .setBody().simple("p.emammal_image.image.online_media.sidorapid:${header.sidoraPid} AND p.emammal_image.image.id:${header.imageid} AND app_id:{{si.ct.uscbi.appId}} AND type:{{si.ct.uscbi.edan_type}}")
                .setHeader(Exchange.HTTP_QUERY).groovy("\"q=\" + URLEncoder.encode(request.getBody(String.class));")
                .to("seda:edanHttpRequest?waitForTaskToComplete=Always").id("deleteEdanSearchRequest")

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN delete metadata search Response Body:\n${body}")

                //Convert string JSON so we can access the data
                .unmarshal().json(JsonLibrary.Gson, true) //converts json to LinkedTreeMap
                //.unmarshal().json(JsonLibrary.Jackson, EdanSearch.class) //converts json to EdanSearch java object
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Body After Unmarshal from JSON= ${body}").id("edanDeleteUnmarshal")

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

                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    Message out = exchange.getIn();

                                    // is bulk edan request enabled
                                    if (getContext().resolvePropertyPlaceholders("{{si.ct.batchEnabled}}").equals("true")) {
                                        JSONObject edanRecordDelete = new JSONObject();
                                        edanRecordDelete.put("id", out.getHeader("edanId"));
                                        edanRecordDelete.put("type", out.getHeader("edanType"));

                                        out.setBody(edanRecordDelete);
                                    } else {
                                        String id = out.getHeader("edanId", String.class);
                                        String type = out.getHeader("edanType", String.class);
                                        out.setHeader(Exchange.HTTP_QUERY, "id="+ id + "&type=" + type);
                                    }

                                }
                            })

                            // Set the EDAN endpoint and query params
                            .setHeader("edanServiceEndpoint").simple("/content/v1.1/admincontent/releaseContent.htm")
                            .to("seda:edanHttpRequest?waitForTaskToComplete=Always").id("deleteEdanRecordRequest")
                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN Delete Record Response Body:\n${body}")

                            //Asset XML attributes indicating delete asset
                            .setHeader("isPublic").simple("No")
                            .setHeader("isInternal").simple("No")

                            .process(assetListProcessor())

                            // add the asset and create/append the asset xml
                            .to("seda:idsAssetImageUpdate?waitForTaskToComplete=Never").id("fedoraDeleteIdsAssetUpdate")

                            .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Release Record...")
                        .end()
                    .endChoice()
                    .otherwise()
                        .log(LoggingLevel.ERROR, LOG_NAME, "${id} EdanIds: EDAN record not found for PID: ${header.pid}")
                    .endChoice()
                .end()

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Finished EDAN Delete Record...");

        from("seda:edanHttpRequest?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("edanHttpRequest")
                .errorHandler(noErrorHandler())

                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Starting EDAN Http Request: Endpoint = ${header.edanServiceEndpoint}, HTTP_QUERY = ${header.CamelHttpQuery}")

                .choice()
                    .when().simple("{{si.ct.batchEnabled}} == 'true' && ${header.edanServiceEndpoint} not contains 'search'")
                        .aggregate(simple("${header.edanServiceEndpoint}"), new EdanBulkAggregationStrategy())
                            //.completionTimeout(1000)
                            .completionSize(Integer.valueOf(BATCH_SIZE))
                            .completionTimeout(Integer.parseInt(COMPLETION_TIMEOUT))
                            .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))

                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: edanBulkRequest JSON: ${headers.edanBulkRequests}")

                                // TODO: refactor EdanAPIBean to do post for bulk
                                .setBody().groovy("URLEncoder.encode(request.headers.get('edanBulkRequests').toString());")

                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN Bulk Request JSON\n: ${body}")

                                .to("bean:edanApiBean?method=sendRequest").id("edanApiSendBulkRequest")
                                .convertBodyTo(String.class)

                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN Bulk Request Status: ${header.CamelHttpResponseCode}, Response: ${body}")

                                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished EDAN Http Bulk Request...")
                    .endChoice()
                    .otherwise()
                        // Set the EDAN Authorization headers and preform the EDAN query request
                        .to("bean:edanApiBean?method=sendRequest").id("edanApiSendSingleRequest")
                        .convertBodyTo(String.class)

                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message out = exchange.getIn();
                                String body = out.getBody(String.class);
                                if (body != null) {
                                    JSONObject edanResponse = new JSONObject(body);
                                    if (edanResponse.has("error")) {
                                        String endpoint = out.getHeader("edanServiceEndpoint", String.class);
                                        String query = out.getHeader(Exchange.HTTP_QUERY, String.class);
                                        //throw new EdanIdsException("EDAN Request Error!!! Endpoint: " + endpoint + ", Query: " + query + ", Error:" + edanResponse.toString(4));
                                        throw new EdanIdsException("EDAN Request Error!!! " + endpoint + "?" + query + ", Error: " + edanResponse.toString());
                                    }
                                }

                            }
                        })
                    .endChoice()
                .end();

        from("seda:createEdanJsonContent?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("createEdanJsonContent")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Starting Edan JSON Content creation...")

                // create xml edan content from ManifestXML which will be converted to json for EDAN using a processor
                .setHeader("CamelFedoraPid", simple("${header.pid}"))
                .setHeader("extraJson").simple("{{si.ct.uscbi.extra_property}}")
                .setBody(simple("${header.ManifestXML}"))
                //.to("xslt:file:{{karaf.home}}/Input/xslt/edan_Transform.xsl?saxon=true").id("transform2json")
                .to("xslt:file:{{karaf.home}}/Input/xslt/edan_Transform_2_xml.xsl?saxon=true").id("transform2xml")

                // convert xslt xml output to json and convert array elements to json array
                .process(xml2JsonProcessor()).id("edanConvertXml2Json")

                .setHeader("edanJson").groovy("URLEncoder.encode(request.body, 'UTF-8')").id("encodeJson")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: EDAN JSON content created and encoded: ${header.edanJson}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Finished Edan JSON Content creation...");


        from("seda:idsAssetImageUpdate?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("idsAssetImageUpdate")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting IDS Image Asset Processing for SiteId: ${header.siteId}...")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: si.ct.uscbi.idsPushLocation = {{si.ct.uscbi.idsPushLocation}}, SiteId: ${header.siteId}, idsAssetList:\n${header.idsAssetList}")

                .setHeader("idsAssetImagePrefix", simple("{{si.edu.idsAssetImagePrefix}}"))

                // Split list of Image assets and save to ids dropbox
                .split(header("idsAssetList"))
                    .executorService(customThreadPoolExecutor)
                    .id("idsAssetSplitter")

                    //The IDS Asset Directory and XMl file name
                    .setHeader("idsAssetName", simple("{{si.edu.idsAssetFilePrefix}}${body.siteId}"))
                    // set the image asset file name
                    .setHeader("CamelOverruleFileName", simple("{{si.edu.idsAssetImagePrefix}}${body.imageid}.JPG"))


                    .choice()
                        //Only copy images files if this is not a delete asset
                        .when().simple("${body.isPublic} == 'Yes' && ${body.isInternal} == 'No'")
                            //send to asset xml writer aggregator
                            .to("seda:idsAssetXMLWriter")

                            //Grab the OBJ datastream of the image asset from fedora
                            .setHeader("CamelHttpMethod").constant("GET")
                            .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${body.pid}/datastreams/OBJ/content")
                            .removeHeader("CamelHttpQuery")

                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Get Image http uri = ${header.CamelHttpUri}")

                            .setBody().simple("")

                            .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("idsAssetUpdateGetFedoraOBJDatastreamContent")

                            //Save the image asset to the file system alongside the asset xml
                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Saving File: {{si.ct.uscbi.idsPushLocation}}/${header.idsAssetName}/${header.idsAssetImagePrefix}${header.CamelOverruleFileName}.JPG")
                            .toD("file:{{si.ct.uscbi.idsPushLocation}}/${header.idsAssetName}")
                            .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Added IDS Asset File CamelFileNameProduced:${header.CamelFileNameProduced}")
                        .endChoice()
                        // Dont Copy Images Just write asset xml for delete
                        .when().simple("${body.isPublic} == 'No' && ${body.isInternal} == 'No'")
                            //send to asset xml writer aggregator
                            .to("seda:idsAssetXMLWriter")
                        .endChoice()
                    .end()
                .end()

                //notify the aggregator to complete on this idsAssetName that is based on siteId
                .setHeader(Exchange.AGGREGATION_COMPLETE_ALL_GROUPS, simple("true", boolean.class))
                .to("seda:idsAssetXMLWriter")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished IDS Image Asset Processing for SiteId: ${header.siteId}...");


        from("seda:idsAssetXMLWriter?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("idsAssetXMLWriter")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Starting IDS Image Asset XML for SiteId: ${header.siteId}...")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: si.ct.uscbi.idsPushLocation = {{si.ct.uscbi.idsPushLocation}}, SiteId: ${header.siteId}, idsAssetName: ${header.idsAssetName}, idsAsset:\n${body}")

                // Aggregate on idsAssetName that is based on siteId
                .aggregate(simple("${header.idsAssetName}"), new IdsBatchAggregationStrategy())
                    .completionTimeout(Integer.parseInt(COMPLETION_TIMEOUT))
                    .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))

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
                                log.debug(logMarker, "*****[ Resource File: {} ]*****", resourceFilePath);
                                File resourceFile = new File(resourceFilePath);
                                if (resourceFile.exists()) {
                                    out.setBody(resourceFile, File.class);
                                } else {
                                    out.setBody(null);
                                }
                            }
                        }).id("getAssetXml")


                        .choice()
                            .when().simple("${body} != null") // the asset xml should be in the body if it exists already
                                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: IDS Asset XML file exists and will be appended.")

                                //IDS Asset XML exists so append
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Pre Appended Asset XML Body:\n${body}")
                                //.toD("xslt:file:{{karaf.home}}/Input/xslt/idsAssets.xsl").id("idsAssetXMLEndpointXSLT")

                                .process(new Processor() {
                                    @Override
                                    public void process(Exchange exchange) throws Exception {
                                        Message out = exchange.getIn();

                                        List<IdsAsset> idsAssetList = out.getHeader("idsAssetList", List.class);

                                        Document doc = out.getBody(Document.class);
                                        Element root = doc.getDocumentElement();
                                        NodeList assetsList = root.getElementsByTagName("Asset");

                                        for (IdsAsset asset : idsAssetList) {
                                            boolean inXML = true;

                                            for (int i = 0; i < assetsList.getLength(); i++) {
                                                Node currentAsset = assetsList.item(i);
                                                String assetValue = asset.getImageid().trim();
                                                String nodeValue = currentAsset.getFirstChild().getTextContent().replace(out.getHeader("idsAssetImagePrefix", String.class), "").trim();

                                                if (assetValue.equals(nodeValue)) {
                                                    inXML = false;
                                                    log.debug(logMarker, assetValue + " does not equal " + nodeValue);
                                                } else {
                                                    log.debug(logMarker, assetValue + " does not equal " + nodeValue);
                                                }
                                            }

                                            if (inXML) {
                                                Element newAsset = doc.createElement("Asset");
                                                newAsset.setAttribute("Name", out.getHeader("idsAssetImagePrefix") + asset.getImageid() + ".JPG");
                                                newAsset.setAttribute("IsPublic", asset.getIsPublic());
                                                newAsset.setAttribute("IsInternal", asset.getIsInternal());
                                                newAsset.setAttribute("MaxSize", "3000");
                                                newAsset.setAttribute("InternalMaxSize", "4000");
                                                newAsset.appendChild(doc.createTextNode(out.getHeader("idsAssetImagePrefix") + asset.getImageid()));
                                                root.appendChild(newAsset);
                                            }
                                        }
                                        out.setBody(doc, String.class);
                                    }
                                })
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Post Appended Asset XML Body:\n${body}")
                            .endChoice()
                            .otherwise()
                                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: IDS Asset XML file does not exists and will be created.")
                                //IDS Asset XML does not exist so create it
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Pre Create Asset XML Body:\n${body}")
                                .convertBodyTo(String.class)
                                .toD("velocity:file:{{karaf.home}}/Input/templates/ids_template.vsl")
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Post Create Asset XML Body:\n${body}")
                            .endChoice()
                        .end()

                        //Save the IDS Asset XML to file system
                        .setHeader("CamelOverruleFileName", simple("${header.idsAssetName}.xml"))
                        .toD("file:{{si.ct.uscbi.idsPushLocation}}/${header.idsAssetName}?fileName=${header.idsAssetName}.xml").id("saveFile")
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: IDS Asset XML saved to CamelFileNameProduced:${header.CamelFileNameProduced}")

                        .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished IDS Asset Update for SiteId: ${header.siteId}...");

        from("seda:findParentObject?concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS)).routeId("EdanIdsFindParentObject").errorHandler(noErrorHandler())
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Starting Find Parent Object...")
                .setBody().simple("SELECT ?subject FROM <info:edu.si.fedora#ri> WHERE { ?subject <info:fedora/fedora-system:def/relations-external#hasResource> <info:fedora/${header.pid}> .}")
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Find Parent Query - ${body}")

                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader(Exchange.HTTP_URI).simple("{{si.fuseki.endpoint}}")
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")

                .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy")
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

                .setHeader("parentPid").xpath("substring-after(/ri:sparql/ri:results/ri:result[1]/ri:binding/ri:uri, 'info:fedora/')", String.class, ns)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Parent PID: ${header.parentPid}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Finished Find Parent Object...");

        from("activemq:queue:{{edanIds.ct.queue}}").routeId("EdanIdsProcessCtMsg")
                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting processing Camera Trap Ingest Message...").id("ctEdanIdsStart")

                //Set the Authorization header for Fedora HTTP calls
                .setHeader("Authorization").simple("Basic " + Base64.getEncoder().encodeToString((fedoraUser + ":" + fedoraPasword).getBytes("UTF-8")), String.class)
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Fedora Authorization: ${header.Authorization}")

                //Remove unneeded headers that may cause problems later on
                .removeHeaders("User-Agent|CamelHttpCharacterEncoding|CamelHttpPath|CamelHttpQuery|connection|Content-Length|Content-Type|boundary|CamelCxfRsResponseGenericType|org.apache.cxf.request.uri|CamelCxfMessage|CamelHttpResponseCode|Host|accept-encoding|CamelAcceptContentType|CamelCxfRsOperationResourceInfoStack|CamelCxfRsResponseClass|CamelHttpMethod|incomingHeaders|CamelSchematronValidationReport|datastreamValidationXML")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Received Camera Trap Ingest Message: ProjectId: ${header.ProjectId}, SiteId: ${header.SiteId}, SitePID: ${header.SitePID}")
                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Camera Trap Ingest Message Headers: ${headers}")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: ***** Delaying for a moment before sending to Seda processing queue *****")
                .delay(1500)

                .to("seda:processCtDeployment?waitForTaskToComplete=Never&size=" + Integer.valueOf(CT_PROCESSING_QUEUE_SIZE)
                        + "&blockWhenFull=true").id("ctStartProcessCtDeployment")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Adding Camera Trap Ingest Message to Seda processing queue");

        from("seda:processCtDeployment?size=" + Integer.valueOf(CT_PROCESSING_QUEUE_SIZE)
                + "&concurrentConsumers=" + Integer.valueOf(CONCURRENT_CONSUMERS))
                .routeId("EdanIdsProcessCtDeployment")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Starting Camera Trap Deployment processing ProjectId: ${header.ProjectId}, SiteId: ${header.SiteId}, SitePID: ${header.SitePID}...")

                // use the list of pids that the ct ingest route created and we sent as part of the JMS message
                // We could also get the deployment RELS-EXT and do the same thing or validate the pids in the
                // PIDAggregation header created during the CT ingest against the deployment RELS-EXT pids
                .split().tokenize(",", "PIDAggregation")
                    .parallelProcessing(Boolean.parseBoolean(PARALLEL_PROCESSING))
                    .aggregationStrategy(new EdanIdsAggregationStrategy())
                    //use the EdanIdsAggregationStrategy ^^^ to collect headers and body during split that are needed for idsAsset.xsl or ids_template.vsl

                    .setHeader("pid").simple("${body}")

                    // Make sure the pid is not an observation object
                    .filter().simple("${body} not in '${header.ResearcherObservationPID},${header.VolunteerObservationPID},${header.ImageObservationPID}'")

                        .log(LoggingLevel.DEBUG, LOG_NAME, "Split Body: ${body}, CamelSplitIndex: ${header.CamelSplitIndex}, CamelSplitSize: ${header.CamelSplitSize}, CamelSplitComplete: ${header.CamelSplitComplete}")

                        //Grab the objects datastreams as xml
                        .setHeader("CamelHttpMethod").constant("GET")
                        .setHeader(Exchange.HTTP_URI).simple("{{si.fedora.host}}/objects/${header.pid}/datastreams").id("edanIdsCtGetDatastreams")
                        .setHeader(Exchange.HTTP_QUERY).simple("format=xml")

                        .toD("http4://useHttpUriHeader?headerFilterStrategy=#dropHeadersStrategy").id("ctProcessGetFedoraDatastream")

                        //Get the label so we know what the imageid for image we are working with
                        .setHeader("imageid").xpath("/objDatastreams:objectDatastreams/objDatastreams:datastream[@dsid='OBJ']/@label", String.class, ns)
                        .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: label: ${header.imageid} for PID: ${header.pid}")

                        //Filter out images that have speciesScientificName that are in the excluded list
                        //check the speciesScientificName in the deployment manifest to see if we even need to continue processing
                        .choice()
                            .when().xpath("//ImageSequence[Image[ImageId/text() = $in:imageid]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.edanids.speciesScientificName.filter'), text())] != ''", "ManifestXML")

                                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: NO POST TO EDAN or IDS for this PID: ${header.pid}, ImageId: ${header.imageid}")
                            .endChoice()
                            .otherwise()
                                .log(LoggingLevel.DEBUG, LOG_NAME, "${id} EdanIds: Send to EDAN [ label: ${header.imageid} for PID: ${header.pid} ]")

                                // edit/create the EDAN record
                                .to("seda:edanUpdate?waitForTaskToComplete=Never").id("ctProcessEdanUpdate")

                                //Asset XML atributes indicating serve to IDS (pub)
                                .setHeader("isPublic").simple("Yes")
                                .setHeader("isInternal").simple("No")
                            .endChoice()
                        .end()
                    .end()
                .end()

                .setBody().simple("${header.idsAssetList}")
                .log(LoggingLevel.DEBUG, "idsAssertList:\n${header.idsAssetList}")

                // add the asset and create/append the asset xml
                // call idsAssetUpdate to write once using idsAssetList header from split aggregator to create idsAsset xml
                .to("seda:idsAssetImageUpdate").id("processCtDeploymentIdsAsset")

                .log(LoggingLevel.INFO, LOG_NAME, "${id} EdanIds: Finished Camera Trap Deployment processing ProjectId: ${header.ProjectId}, SiteId: ${header.SiteId}, SitePID: ${header.SitePID}...");
    }
}
