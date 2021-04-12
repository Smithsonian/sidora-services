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

package edu.si.services.sidora.derivatives.routes;

import edu.si.services.beans.excel.ExcelToCSV;
import edu.si.services.beans.fitsservlet.FITSServletRouteBuilder;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.support.builder.Namespaces;
import org.springframework.stereotype.Component;

import javax.imageio.IIOException;
import java.net.ConnectException;
import java.net.SocketException;

import static org.apache.camel.LoggingLevel.*;

/**
 * @author jbirkhimer
 */
@Component
public class DerivativesRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "edu.si.derivatives")
    static private String CT_LOG_NAME;

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

        getContext().addRoutes(new FITSServletRouteBuilder());

        Namespaces ns = new Namespaces();
        ns.add("atom", "http://www.w3.org/2005/Atom");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("fs", "info:fedora/fedora-system:def/model#");
        ns.add("fedora-types", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        ns.add("fsmgmt", "http://www.fedora.info/definitions/1/0/management/");
        ns.add("sidora", "http://oris.si.edu/2017/01/relations#");
        ns.add("objectProfile", "http://www.fedora.info/definitions/1/0/access/");

//        errorHandler(noErrorHandler());

        onException(ConnectException.class, HttpOperationFailedException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("1000")
                .maximumRedeliveries("200")
                .retryAttemptedLogLevel(WARN)
                .end();


        from("activemq:queue:sidora.apim.update").routeId("DerivativesStartProcessing")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting processing ...")

                .setHeader("origin").xpath("/atom:entry/atom:author/atom:name/text()", ns)

                .log(INFO, CT_LOG_NAME, "${id} Derivatives: Processing PID: ${headers.pid}  Method Name: ${headers.methodName}  Origin: ${header.origin}")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Processing BODY: ${body}")

                // The login used by the source of the message is put in the ATOM Author element by Fedora.
                // Since this is a system login not a end-user, we can use this to identify the process that is the source of the message.

                // Filter for messages coming from monitored processes.
                .filter().xpath("/atom:entry/atom:author/atom:name = '{{si.fedora.user}}'", ns)
                    .log(INFO, CT_LOG_NAME, "${id} Derivatives: Filtered ${header.origin} - No message processing required.")
                    .stop()
                .end()

                // Get the DSID from the Atom message if any.
                .setHeader("DSID").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsID']/@term", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Atom DSID: ${headers.DSID}")

                // Filter for messages where DSID is not OBJ that do not need derivatives processing.
                .filter().simple("${header.DSID} != 'OBJ'")
                    .log(INFO, CT_LOG_NAME, "${id} Derivatives: Filtered ${header.DSID} - No derivative processing required.")
                    .stop()
                .end()

                .setHeader("dsLocation").xpath("/atom:entry/atom:category[@scheme='fedora-types:dsLocation']/@term", String.class, ns)
                .setHeader("logMessage").xpath("/atom:entry/atom:category[@scheme='fedora-types:logMessage']/@term", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: dsLocation = ${headers.dsLocation}, logMessage = ${headers.logMessage}")

                // Filter CT Face Blurred OBJ images that have already been processed
                // Otherwise will cause a loop
                .filter().simple("${header.logMessage} == 'faceBlurred'")
                    .log(INFO, CT_LOG_NAME, "${id} Derivatives: Filtered ${header.logMessage} - No derivative processing required.")
                    .stop()
                .end()

                // Filter out Fedora API methods that do not need derivatives processing.
                // This could be done with a JMS selector.
                .filter().simple("${headers.methodName} in 'addDatastream,modifyDatastreamByValue,modifyDatastreamByReference,modifyObject,ingest'")
                    .log(INFO, CT_LOG_NAME, "${id} Derivatives: Process Message.")
                    .to("direct:processDerivativesMessage")
                .end()

                .log(INFO, CT_LOG_NAME, "${id} Derivatives: Finished processing.");


        from("direct:processDerivativesMessage").routeId("DerivativesProcessMessage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting Derivatives Message processing ...")

                // Get the PID of the FDO that was just operated upon.
                .log(INFO, CT_LOG_NAME, "${id} Derivatives: PID: ${headers.pid}  Method Name: ${headers.methodName}")

                .setHeader("CamelFedoraPid").simple("${headers.pid}")

                // Get the Content Models for the FDO and put them on a list.
                .to("fedora://getDatastreamDissemination?dsId=RELS-EXT&exchangePattern=InOut").id("getContentModels")
                .convertBodyTo(String.class, "utf-8")

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: RELS-EXT: ${body}")

                .setHeader("isAdministeredBy").xpath("substring-after(//sidora:isAdministeredBy/@rdf:resource, 'info:fedora/')", String.class, ns)
                
                /*.split()
                    .xpath("//fs:hasModel/@rdf:resource")
                    //.aggregationStrategy("modelAggregator")
                    .aggregationStrategy(new ContentModelAggregationStrategy())
                        .convertBodyTo(String.class)
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Split Content Model. BODY: ${body}")
                .end*/
                
                .setHeader("ContentModels").xpath("string-join(//fs:hasModel/@rdf:resource, ',')", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Content Models: ${header.ContentModels}")

                // Filter by content model.
                .choice()

                    // If imageCModel or generalImageCModel, and the DSID is "OBJ" - process the image."
                    .when().spel("#{(request.headers[ContentModels].contains('info:fedora/si:imageCModel') or request.headers[ContentModels].contains('info:fedora/si:generalImageCModel')) and request.headers[DSID] == 'OBJ'}")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found Image.")
                        .to("direct:processDerivativesImage")
                    .endChoice()

                    // If imageCModel or generalImageCModel, and the DSID is "OBJ" - process the image."
                    .when().spel("#{request.headers[ContentModels].contains('info:fedora/si:fieldbookCModel') and request.headers[DSID] == 'OBJ'}")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found PDF.")
                        .to("direct:processDerivativesPDF")
                    .endChoice()

                    // If imageCModel or generalImageCModel, and the DSID is "OBJ" - process the image."
                    .when().spel("#{request.headers[ContentModels].contains('info:fedora/si:datasetCModel') and request.headers[DSID] == 'OBJ'}")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found Dataset.")
                        .to("direct:processDerivativesDataset")
                    .endChoice()

                    // If sp-audioCModel, and the DSID is "OBJ" - process the audio file."
                    .when().spel("#{request.headers[ContentModels].contains('info:fedora/islandora:sp-audioCModel') and request.headers[DSID] == 'OBJ'}")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found Audio.")
                        .to("direct:processDerivativesAudio")
                    .endChoice()

                    // If sp_videoCModel, and the DSID is "OBJ" - process the video file."
                    .when().spel("#{request.headers[ContentModels].contains('info:fedora/islandora:sp_videoCModel') and request.headers[DSID] == 'OBJ'}")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found Video.")
                        .to("direct:processDerivativesVideo")
                    .endChoice()

                    // No supported content model was found.
                    .otherwise()
                        .log(INFO, CT_LOG_NAME, "${id} Derivatives: No supported content model was found. No message processing required.")
                    .endChoice()
                .end()
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished Message processing.");


        from("direct:processDerivativesImage").routeId("DerivativesProcessImage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting Image processing ...")

                // We could submit the file to FITS processing to get the MIME but that can be complicated.
                // We could get the MIME type from the datastream metadata or FITS (or both and compare).
                // For now we will just trust Fedora's datastream metadata.
                // We really should only make new derivatives if the OBJ has changed.

                // Get the MIME type from the datastream profile.
                .to("fedora://getDatastream?dsId=OBJ&exchangePattern=InOut").id("getObjDs")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. BODY: ${body}")
                .setHeader("dsMIME").xpath("/fsmgmt:datastreamProfile/fsmgmt:dsMIME/text() ", String.class, ns)
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. MIME: ${header.dsMIME}")

                //DGI has quite a bit more in their Python conversions so we need to improve this.
                .to("fedora://getDatastreamDissemination?dsId=OBJ&exchangePattern=InOut").id("getObjContent")
                .toD("file:{{staging.dir}}/").id("saveToStaging")
                .log(INFO, CT_LOG_NAME, "${id} Derivatives: Staged file Name: ${header.CamelFileNameProduced}")

                // only check for uploads (note WB triggers several OBJ updates operating on all causes issues)
                .filter().simple("${header.dsLocation} contains 'uploaded' || ${header.dsLocation} contains 'http://'")
                    // If this is a CT image that needs face blurred
                    .to("direct:isFaceBlur").id("isFaceBlur")
                .end()

                .choice().description("Filter content by image format.")
                    .when().simple("${header.dsMIME} == 'image/jpg' || ${header.dsMIME} == 'image/jpeg' || ${header.dsMIME} == 'image/jpe'").description("If the image is a JPEG? Add an archival JPEG2000 and a thumbnail to the FDO.")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found JPEG.")
                        .multicast()
                            .description("Add a thumbnail from the JPEG.")
                            .to("direct:processDerivativesThumbnailator")
                            .to("direct:processDerivativesFITS")
                            // Future: Make a JPEG2000 archival image and store it in the MASTER datastream.
                        .end()
                    .endChoice()

                    //.description(" If the image is a TIFF? Add a JPG datastream and thumbnail for the image to the FDO.")
                    .when().simple("${header.dsMIME} == 'image/tiff' || ${header.dsMIME} == 'image/tif'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found TIFF.")
                        .multicast()
                            .to("direct:processDerivativesTIFFImage")
                            .to("direct:processDerivativesFITS")
                            // Future: Make a JPEG2000 archival image and store it in the MASTER datastream.
                        .end()
                    .endChoice()

                    //.description("If the image is a JPEG2000? Add a JPG datastream and thumbnail to the FDO.")
                    .when().simple("${header.dsMIME} == 'image/jp2'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found JPEG2000.")
                        .multicast()
                            .to("direct:processDerivativesJP2Image")
                            .to("direct:processDerivativesFITS")
                        .end()
                    .endChoice()

                    //.description("If the image is a PNG, GIF, BMP? Add a JPG datastream and thumbnail for the image to the FDO.")
                    .when().simple("${header.dsMIME} == 'image/png' || ${header.dsMIME} == 'image/gif' || ${header.dsMIME} == 'image/bmp'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found PNG, GIF or BMP.")
                        .multicast()
                            .to("direct:processDerivativesBGPImage")
                            .to("direct:processDerivativesFITS")
                            // Future: Make a JPEG2000 archival image and store it in the MASTER datastream.
                        .end()
                    .endChoice()

                    //.description("If the image is a NEF? Add a JPG datastream and thumbnail for the image to the FDO.")
                    .when().simple("${header.dsMIME} == 'image/x-nikon-nef'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found NEF (Nikon RAW).")
                        .multicast()
                            .to("direct:processDerivativesNEFImage")
                            .to("direct:processDerivativesFITS")
                            // Future: Make a JPEG2000 archival image and store it in the MASTER datastream.
                        .end()
                    .endChoice()

                    //.description("If the image is a DNG? Add a JPG datastream and thumbnail for the image to the FDO.")
                    .when().simple("${header.dsMIME} == 'image/x-adobe-dng'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found DNG (Camera RAW).")

                        // FITS will not generate the correct output for DNG without the file ext.
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Renaming Staged File For DNG processing: ${header.CamelFileNameProduced} to ${header.CamelFileNameProduced}.dng")

                        .setHeader("CamelFileNameProducedStash").simple("${header.CamelFileNameProduced}")
                        .setBody().simple("${header.CamelFileNameProduced}")

                        .to("reader:file")
                        .to("file://?fileName=${header.CamelFileNameProduced}.dng")

                        .multicast()
                            .to("direct:processDerivativesDNGImage")
                            .to("direct:processDerivativesFITS")
                            // Future: Make a JPEG2000 archival image and store it in the MASTER datastream.
                        .end()

                        // Delete the temporary file created for DNG processing. Note: This approach is Unix specific.
                        .toD("exec:rm?args=-f ${header.CamelFileNameProduced}")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}")
                        .end()

                        // Unstash the original filename for deletion
                        .setHeader("CamelFileNameProduced").simple("${header.CamelFileNameProducedStash}")
                    .endChoice()

                    .otherwise().description("If the image is some other MIME type, just log a warning?")
                        .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unsupported image type found. MIME: ${headers.dsMIME}")
                    .endChoice()
                .end()

                // Delete the temporary file. Note: This approach is Unix specific.
                .toD("exec:rm?args=-f ${header.CamelFileNameProduced}")

                .filter(simple("${headers.CamelExecExitValue} != 0"))
                    .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}")
                .end()

                // Delete the temporary file. Note: This approach is Unix specific.
                .toD("exec:rm?args=-f ${header.BlurSourceFile}")

                .filter(simple("${headers.CamelExecExitValue} != 0"))
                    .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}")
                .end()

                .removeHeader("BlurSourceFile")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished Image processing.");


        from("direct:processDerivativesDNGImage").routeId("DerivativesProcessDNGImage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting DNG image processing ...")

                // Create a JPEG from DNG image
                .toD("exec:convert?args= - jpg:${header.CamelFileNameProduced}.jpg")

                .choice().description("If the conversion succeeded? Add a display datastream for the image to the FDO.")
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setBody().simple("${header.CamelFileNameProduced}.jpg")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=DISPLAY&type=image/jpg&group=M&dsLabel=DISPLAY&versionable=false")
                        // Also add a thumbnail from the JPEG since DNG is not supported.
                        .to("direct:processDerivativesThumbnailator")
                        // Delete the temporary JPG conversion file. Note: This approach is Unix specific.

                        .toD("exec:rm?args=-f ${header.CamelFileNameProduced}.jpg")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.jpg")
                        .end()

                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to convert DNG to JPG. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished DNG Image processing.");


        from("direct:processDerivativesNEFImage").routeId("DerivativesProcessNEFImage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting NEF (Nikon RAW) image processing ...")

                //Create a JPEG from NEF (Nikon RAW) image
                //.toD("exec:{{exiftool.path}}?args= -b -JpgFromRaw ${header.CamelFileNameProduced}")
                .toD("exec:sh?args=-c &quot;{{exiftool.path}} -b -JpgFromRaw ${header.CamelFileNameProduced} > ${header.CamelFileNameProduced}.jpg&quot;")

                .choice().description("If the conversion succeeded? Add a display datastream for the image to the FDO.")
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setBody().simple("${header.CamelFileNameProduced}.jpg")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=DISPLAY&type=image/jpg&group=M&dsLabel=DISPLAY&versionable=false")

                        // Also add a thumbnail from the JPEG since TIFF is not supported.
                        .to("direct:processDerivativesThumbnailator")

                        // Delete the temporary JPG conversion file. Note: This approach is Unix specific.
                        .toD("exec:rm?args=-f ${header.CamelFileNameProduced}.jpg")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.jpg")
                        .end()
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to convert NEF (Nikon RAW) to JPG. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished NEF (Nikon RAW) Image processing.");


        from("direct:processDerivativesTIFFImage").routeId("DerivativesProcessTIFFImage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting TIFF image processing ...")

                //.recipientList().simple("exec:convert?args= - jpg:-)
                .recipientList().simple("exec:convert?args= - jpg:${header.CamelFileNameProduced}.jpg")

                .choice()
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setBody().simple("${header.CamelFileNameProduced}.jpg")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=DISPLAY&type=image/jpg&group=M&dsLabel=DISPLAY&versionable=false")
                        // Also add a thumbnail from the JPEG since TIFF is not supported.

                        .to("direct:processDerivativesThumbnailator")
                        // Delete the temporary JPG conversion file. Note: This approach is Unix specific.

                        .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}.jpg")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.jpg")
                            // We also want to proactively tell monitoring
                        .end()

                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to convert TIFF to JPG. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished TIFF Image processing.");


        from("direct:processDerivativesJP2Image").routeId("DerivativesProcessJP2Image")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting JP2 image processing ...")

                // We need to convert to a good lossless JPEG2000. This current code uses a very naive approach.
                //.recipientList().simple("exec:convert?args= - jpg:-)
                .recipientList().simple("exec:convert?args= - jpg:${header.CamelFileNameProduced}.jpg")

                .choice()
                    // If the conversion succeeded, Add a display datastream for the image to the FDO?
                    .when().simple("${headers.CamelExecExitValue} == 0")
                    .setBody().simple("${header.CamelFileNameProduced}.jpg")

                    .to("reader:file")
                    .to("fedora:addDatastream?name=DISPLAY&type=image/jpg&group=M&dsLabel=DISPLAY&versionable=false")
                    // Also add a thumbnail from the JPEG since JPG2 is not supported.

                    .to("direct:processDerivativesThumbnailator")
                    // Delete the temporary JPG conversion file. Note: This approach is Unix specific.

                    .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}.jpg")

                    .filter(simple("${headers.CamelExecExitValue} != 0"))
                        .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.jpg")
                        // We also want to proactively tell monitoring
                    .end()
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to convert JP2 to JPG. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished JP2 Image processing.");


        from("direct:processDerivativesBGPImage").routeId("DerivativesProcessBGPImage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting BGP image processing ...")

                //.recipientList().simple("exec:convert?args= - jpg:-)
                .recipientList().simple("exec:convert?args= - jpg:${header.CamelFileNameProduced}.jpg")

                .choice()
                    // If the conversion succeeded? Add a display datastream for the image to the FDO.
                    .when().simple("${headers.CamelExecExitValue} == 0")
                    .setBody().simple("${header.CamelFileNameProduced}.jpg")

                    .to("reader:file")

                    .to("fedora:addDatastream?name=DISPLAY&type=image/jpg&group=M&dsLabel=DISPLAY&versionable=false")

                    .to("direct:processDerivativesThumbnailator")

                    // Delete the temporary JPG conversion file. Note: This approach is Unix specific.
                    .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}.jpg")

                    .filter(simple("${headers.CamelExecExitValue} != 0"))
                        .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.jpg")
                        // We also want to proactively tell monitoring
                    .end()

                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to convert TIFF to JPG. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished BGP Image processing.");


        from("direct:processDerivativesDataset").routeId("DerivativesProcessDataset")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting Dataset processing ...")

                // We could submit the file to FITS processing to get the MIME but that can be complicated.
                // We could get the MIME type from the datastream metadata or FITS (or both and compare).
                // For now we will just trust Fedora's datastream metadata.
                // We really should only make new derivatives if the OBJ has changed.
                // .to("direct://processDerivativesFITS")

                //Get the MIME type from the datastream profile.
                .to("fedora://getDatastream?dsId=OBJ&exchangePattern=InOut")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. BODY: ${body}")

                .setHeader("dsMIME").xpath("/fsmgmt:datastreamProfile/fsmgmt:dsMIME/text() ", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. MIME: ${header.dsMIME}")

                // Filter content by dataset format.
                .choice()
                    // If the content is Excel?
                    .when().simple("${header.dsMIME} == 'application/vnd.ms-excel' || ${header.dsMIME} == 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found Excel.")

                        .to("fedora://getDatastreamDissemination?dsId=OBJ&exchangePattern=InOut")

                        .bean(ExcelToCSV.class, "convertExcelToCSV")

                        .to("fedora:addDatastream?name=CSV&type=text/csv&group=M&dsLabel=CSV&versionable=false")
                    .endChoice()
                    // If the content is a CSV? Just add a datastream to the FDO.
                    .when().simple("${header.dsMIME} == 'text/csv'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Found CSV.")

                        // Copy the OBJ datastream to the CSV datastream.
                        .to("fedora://getDatastreamDissemination?dsId=OBJ&exchangePattern=InOut")
                        .to("fedora:addDatastream?name=CSV&type=text/csv&group=M&dsLabel=CSV&versionable=false")
                    .endChoice()
                    .otherwise()
                        // If the dataset is some other mime type, just log a warning?
                        .log(WARN, CT_LOG_NAME, "${id} Derivatives: Not a supported dataset format.. MIME: ${headers.dsMIME}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished Dataset processing.");


        from("direct:processDerivativesPDF").routeId("DerivativesProcessPDF")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Starting PDF processing ...")

                //We really should make new derivatives only if OBJ has changed.
                //We could get the MIME type from the datastream metadata or FITS (or both and compare).
                //.to("direct://processDerivativesFITS")

                //Get the MIME type from the datastream profile.
                //We should try the getDatastream more than once if it fails.
                .to("fedora://getDatastream?dsId=OBJ&exchangePattern=InOut")

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. BODY: ${body}")

                .setHeader("dsMIME").xpath("/fsmgmt:datastreamProfile/fsmgmt:dsMIME/text() ", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. MIME: ${header.dsMIME}")

                // Filter the content by PDF type (remember PDF-A)
                .choice()
                    // If the content is a PDF? Create a thumbnail and an SWF derivative.
                    .when().simple("${header.dsMIME} == 'application/pdf'")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Process PDF.")

                        // Get the PDF from the FDO.
                        // We should try the getDatastreamDissemination more than once if it fails.
                        .to("fedora://getDatastreamDissemination?dsId=OBJ&exchangePattern=InOut")
                        .toD("file:{{staging.dir}}/")  // This will create a temporary file that must be cleaned out.

                        .to("direct:processDerivativesThumbnailImage")
                        .to("direct:processDerivativesPDF2SWF")

                        // Delete the temporary file. Note: This approach is Unix specific.
                        .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}")
                            // We also want to proactively tell monitoring
                        .end()
                    .endChoice()
                    .otherwise()
                        // If the PDF is some other mime type, just log a warning?
                        .log(WARN, CT_LOG_NAME, "${id} Derivatives: Not a PDF. MIME: ${headers.dsMIME}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished PDF processing.");


        from("direct:processDerivativesPDF2SWF").routeId("DerivativesProcessPDF2SWF")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started PDF2SWF processing ...")

                // Create a Flash derivative using pdf2swf for the Flexpaper viewer.
                // Unfortunately pdf2swf cannot accept a pipe as an input stream source, hence we use a temporary file.
                .recipientList().simple("exec:pdf2swf?args=${header.CamelFileNameProduced} -o ${header.CamelFileNameProduced}.swf")

                .choice()
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setBody().simple("${header.CamelFileNameProduced}.swf")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=OBJ.swf&type=application/x-shockwave-flash&group=M&dsLabel=Flexpaper&versionable=false")

                        // Delete the temporary SWF conversion file. Note: This approach is Unix specific.
                        // The conversion will fail if the picture is too complex.  The current decision is to log the failure and just force the use of the PDF only.
                        .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}.swf")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.swf")
                            // We also want to proactively tell monitoring
                        .end()
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to convert PDF to SWF. PID: ${headers.CamelFedoraPid}  ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished PDF2SWF processing.");


        from("direct:processDerivativesFITS").routeId("DerivativesProcessFITS")
                .onException(SocketException.class)
                    .useExponentialBackOff()
                    .backOffMultiplier(2)
                    .redeliveryDelay(1000)
                    .maximumRedeliveries(200)
                    .retryAttemptedLogLevel(WARN)
                    .continued(true)
                    .log(WARN, CT_LOG_NAME, "[${routeId}] :: FITS web service request failed!!! Error reported: ${exception.message}").id("derivativesFITSServiceException")
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started FITS processing ...")

                // Create a FITS derivative using Harvard FITS.
                .setHeader("CamelFileName").simple("${header.CamelFileAbsolutePath}")

                // Ensure that we actually have a 200 response from the fits web service as this header could have been set upstream by another http request and that we are able to use a POST rather than a GET accidentally.
                // Not sure this is needed.
                .setBody().simple("null")
                .removeHeaders("CamelHttpMethod|CamelHttpResponseCode")

                // Get the FITS analysis of the file.
                .to("direct:getFITSReport")

                .log(DEBUG, CT_LOG_NAME, "${id} FITS Web Service Response Body:\n${body}")

                .choice()
                    // If FITS processing succeeded? Store a FITS datastream on the FDO.
                    .when().simple("${header.CamelHttpResponseCode} == 200")
                        .setHeader("dsMIME").xpath("/fits:fits/fits:identification/fits:identity[1]/@mimetype", String.class, ns)
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: FITS MIME: ${headers.dsMIME}")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Exec FITS. BODY: ${body}")
                        .convertBodyTo(String.class)
                        .to("fedora:addDatastream?name=FITS&type=text/xml&dsLabel=FITS%20Generated%20Image%20Metadata&group=X&versionable=false")
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: FITS processing failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelHttpResponseCode}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Finished Derivatives FITS processing.");


        from("direct:processDerivativesAudio").routeId("DerivativesProcessAudio")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Audio processing ...")

                // Get the MIME type from the datastream profile.
                // We should try the getDatastream more than once if it fails.
                .to("fedora://getDatastream?dsId=OBJ&exchangePattern=InOut")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. BODY: ${body}")

                .setHeader("dsMIME").xpath("/fsmgmt:datastreamProfile/fsmgmt:dsMIME/text() ", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Datastream Metadata. MIME: ${header.dsMIME}")

                // Get the Audio from the FDO.
                // We should try the getDatastreamDissemination more than once if it fails.
                .to("fedora://getDatastreamDissemination?dsId=OBJ&exchangePattern=InOut")

                .choice()
                    // If the OBJ datastream contains an mp3?
                    .when().simple("${header.dsMIME} == 'audio/mpeg'")
                        // Just copy it.
                        .to("fedora:addDatastream?name=PROXY_MP3&type=audio/mpeg&dsLabel=PROXY_MP3&group=M&versionable=false")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: No processing required copying mp3 directly to PROXY_MP3.")
                    .endChoice()
                    .when().simple("${header.dsMIME} == 'audio/vnd.wave' || ${header.dsMIME} == 'audio/vnd.wav' || ${header.dsMIME} == 'audio/x-wav' || ${header.dsMIME} == 'audio/vorbis' || ${header.dsMIME} == 'audio/x-ms-wma' || ${header.dsMIME} == 'audio/x-aac' || ${header.dsMIME} == 'audio/x-aiff'")

                        // Make an MP3 derivative.
                        // Create an audio derivative using the lame encoder.
                        .recipientList().simple("exec:lame?args=-V5 --vbr-new - -")

                        .choice()
                            // If lame processing succeeded? Store a PROXY_MP3 datastream on the FDO.
                            .when().simple("${headers.CamelExecExitValue} == 0")
                                .delay(1000)
                                .to("fedora:addDatastream?name=PROXY_MP3&type=audio/mpeg&dsLabel=PROXY_MP3&group=M&versionable=false")
                                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Adding converted audio as mp3 to PROXY_MP3.")
                            .endChoice()
                            .otherwise()
                                .log(ERROR, CT_LOG_NAME, "${id} Derivatives: LAME processing failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                            .endChoice()
//                        .end()
                    .endChoice()
                    .otherwise()
                        .log(WARN, CT_LOG_NAME, "${id} An unsupported audio file type was found: ${header.dsMIME}.")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Finished Derivatives Audio processing.");


        from("direct:processDerivativesThumbnailator").routeId("DerivativesProcessThumbnailator")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Thumbnailator processing ...")

                //Create a thumbnail derivative using the Thumbnailator library.
                //.to("thumbnailator:image?keepRatio=false&size=(200,150)")
                //.to("fedora:addDatastream?name=TN&type=image/jpg&group=M&dsLabel=Thumbnail&versionable=false")

                .doTry()
                    .to("thumbnailator:image?keepRatio=true&size=(200,150)")
                    .to("fedora:addDatastream?name=TN&type=image/jpeg&group=M&dsLabel=Thumbnail&versionable=false")
                .doCatch(IIOException.class)
                    .log(WARN, CT_LOG_NAME, "${id} Derivatives: Cannot create thumbnail image corrupted.")
                .endDoTry()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished Thumbnailator processing.");


        from("direct:processDerivativesThumbnailImage").routeId("DerivativesProcessThumbnailImage")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Thumbnail Image processing ...")

                // Making a call to camel:exec is less reliable and portable then using the Thumbnailator library. However, ImageMagick can create Thumbnails for formats that Thumbnailator cannot, for this case primarily PDF to JPG.  This route uses ImageMagick directly to create the thumbnail. The alternative is an intermediate JPG which takes a second step to accomplish.

                // Create a thumbnail derivative using ImageMagick.
                // The arg tokenizer strips of the plus for profile. Workaround is to wrap all the args in RAW().
                // exec:convert?args=RAW(-[0] -thumbnail 200x150 -colorspace RGB +profile * jpg:-)
                // Convert only the first page since the PDF may have multiple pages.
                // exec:convert?args= -[0] -thumbnail 200x150 -colorspace RGB jpg:-
                .recipientList().simple("exec:convert?args= -[0] -thumbnail 200x150 -colorspace RGB jpg:${header.CamelFileNameProduced}.jpg")

                .choice()
                    // If the conversion succeeded? Store the thumbnail image datastream on the FDO.
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setBody().simple("${header.CamelFileNameProduced}.jpg")
                        .to("reader:file")
                        .to("fedora:addDatastream?name=TN&type=image/jpeg&group=M&dsLabel=Thumbnail&versionable=false")

                        // Delete the temporary JPG conversion file. Note: This approach is Unix specific.
                        .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}.jpg")

                        .filter(simple("${headers.CamelExecExitValue} != 0"))
                            .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete working file. Filename: ${headers.CamelFileNameProduced}.jpg")
                            // We also want to proactively tell monitoring.
                        .end()

                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Unable to create thumbnail image. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Finished Thumbnail Image processing.");


        /*=========================== STARTING VIDEO ROUTES ================================*/
        from("direct:processDerivativesVideo").routeId("DerivativesProcessVideo")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Video processing ...")

                //Get the MIME type from the datastream profile.
                //We should try the getDatastream more than once if it fails.
                .to("fedora://getDatastream?dsId=OBJ&exchangePattern=InOut")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Video Datastream Metadata. BODY: ${body}")

                .setHeader("dsMIME").xpath("/fsmgmt:datastreamProfile/fsmgmt:dsMIME/text() ", String.class, ns)

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Video Datastream Metadata. MIME: ${header.dsMIME}")

                //Get the Video from the FDO.
                //We should try the getDatastreamDissemination more than once if it fails.
                .to("fedora://getDatastreamDissemination?dsId=OBJ&exchangePattern=InOut")
                .log(DEBUG, CT_LOG_NAME, "Got The Video OBJ from Fedora.......")

                .toD("file:{{staging.dir}}/")

                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Staged video file name: ${header.CamelFileNameProduced}")

                .setBody().simple("${headers.CamelFileNameProduced}")

                .choice()
                    // If the OBJ datastream contains a supported video format : OGG, MP4, MOV, QT, M4V, AVI, MKV
                    .when().simple("${header.dsMIME} == 'video/mp4'")
                        .setHeader("videoInput").simple("${headers.CamelFileNameProduced}")
                        .setHeader("CamelHttpMethod").simple("POST")

                        // Just copy it and create thumbnail.
                        .toD("{{si.fedora.host}}/objects/${header.pid}/datastreams/MP4?mimeType=video/mp4&dsLabel=MP4&controlGroup=M&versionable=false&dsLocation=file:{{staging.dir}}/${header.videoInput}&authMethod=Basic&authUsername={{si.fedora.user}}&authPassword={{si.fedora.password}}")

                        .to("direct:processDerivativesVideoThumbnailTimePosition")
                        .to("direct:processDerivativesVideoThumbnail")

                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: No Video processing required copying mp4 directly to MP4.")
                    .endChoice()
                    .when().simple("${header.dsMIME} == 'video/m4v' || ${header.dsMIME} == 'video/quicktime' || ${header.dsMIME} == 'video/avi' || ${header.dsMIME} == 'video/x-msvideo' || ${header.dsMIME} == 'video/ogg' || ${header.dsMIME} == 'video/x-m4v' || ${header.dsMIME} == 'video/x-matroska'")

                        // Make an MP4 derivative and TN.
                        // Create a video derivative and thumbnail using ffmpeg.
                        .to("direct:processDerivativesVideoFFMPEG")

                        .setHeader("CamelHttpMethod").simple("POST")

                        .setBody(simple(""))

                        .toD("{{si.fedora.host}}/objects/${header.pid}/datastreams/MP4?mimeType=video/mp4&dsLabel=MP4&controlGroup=M&versionable=false&dsLocation=file:{{staging.dir}}/${header.videoInput}&authMethod=Basic&authUsername={{si.fedora.user}}&authPassword={{si.fedora.password}}")

                        .to("direct:processDerivativesVideoThumbnailTimePosition")
                        .to("direct:processDerivativesVideoThumbnail")
                    .endChoice()
                    .when().simple("${header.dsMIME} == 'audio/mp4' || ${header.dsMIME} == 'audio/ogg' || ${header.dsMIME} == 'audio/vorbis' || ${header.dsMIME} == 'audio/m4a'")

                        // Make an MP4 derivative and TN.
                        // Create a audio derivative using ffmpeg and MP4.
                        .to("direct:processDerivativesVideoFFMPEG")

                        .setHeader("CamelHttpMethod").simple("POST")

                        .setBody(simple(""))

                        .toD("{{si.fedora.host}}/objects/${header.pid}/datastreams/MP4?mimeType=video/mp4&dsLabel=MP4&controlGroup=M&versionable=false&dsLocation=file:{{staging.dir}}/${header.videoInput}&authMethod=Basic&authUsername={{si.fedora.user}}&authPassword={{si.fedora.password}}")

                        // Create a audio thumbnail TN.
                        .setBody().simple("config/video-thumbnails/audio-video.png")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=TN&type=video/jpg&dsLabel=TN&group=M&versionable=false")
                    .endChoice()
                    .otherwise()
                        // mime type unknown set thumbnail TN to unknown video
                        .setBody().simple("config/video-thumbnails/video_unknown.png")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=TN&type=video/jpg&dsLabel=TN&group=M&versionable=false")

                        .log(WARN, CT_LOG_NAME, "${id} An unsupported video file type was found: ${header.dsMIME}.")
                    .endChoice()
                .end()

                // Delete the temporary thumbnail file. Note: This approach is Unix specific.
                .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}-TN.jpg")

                .filter(simple("${headers.CamelExecExitValue} != 0"))
                    .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete Thumbnail working file. Filename: ${headers.CamelFileNameProduced}-TN.jpg4")
                    // We also want to proactively tell monitoring
                .end()

                // Delete the temporary mp4 file. Note: This approach is Unix specific.
                .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}.mp4")

                .filter(simple("${headers.CamelExecExitValue} != 0"))
                    .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete video working file. Filename: ${headers.CamelFileNameProduced}.mp4")
                    // We also want to proactively tell monitoring
                .end()

                // Delete the temporary staging file. Note: This approach is Unix specific.
                .recipientList().simple("exec:rm?args=-f ${header.CamelFileNameProduced}")

                .filter(simple("${headers.CamelExecExitValue} != 0"))
                    .log(WARN, CT_LOG_NAME, "${id} Derivatives: Unable to delete video working file. Filename: ${headers.CamelFileNameProduced}")
                    // We also want to proactively tell monitoring
                .end()
                
                .log(DEBUG, CT_LOG_NAME, "${id} Finished Derivatives Video processing.");


        from("direct:processDerivativesVideoFFMPEG").routeId("DerivativesProcessVideoFFMPEG")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Video FFMPEG processing ...")

                // Make an MP4 derivative.
                // Create a video derivative using ffmpeg.

                // this ffmpeg incantation causes issues when using the nux dextop version of ffmpeg since it uses the aac audio codec however will work with the custom built ffmpeg rpm which uses libfdk_aac audio codec
                //.recipientList().simple("exec:ffmpeg?args=-nostats -nostdin -i ${header.CamelFileNameProduced} -f mp4 -vcodec libx264 -preset medium -acodec libfdk_aac -ab 128k -ac 2 -async 1 ${header.CamelFileNameProduced}.mp4")

                // ffmpeg incantation for nux dextop version of ffmpeg that uses the aac audio codec
                .recipientList().simple("exec:ffmpeg?args=-nostats -nostdin -i ${header.CamelFileNameProduced} -f mp4 -vcodec libx264 -preset medium -acodec aac -ab 128k -ac 2 -async 1  -strict -2 ${header.CamelFileNameProduced}.mp4")

                .choice()
                    // If ffmpeg processing succeeded? Store a MP4 datastream on the FDO.
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setHeader("videoInput").simple("${headers.CamelFileNameProduced}.mp4")
                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Converted Video to mp4.")
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: FFMPEG processing failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Finished Derivatives Video FFMPEG processing.");


        from("direct:processDerivativesVideoThumbnailTimePosition").routeId("DerivativesProcessVideoThumbnailTimePosition")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Video thumbnail time position processing ...")

                .recipientList().simple("exec:ffprobe?args=-v error -select_streams v:0 -show_entries stream=duration -of compact=print_section=0:nokey=1 ${header.videoInput}&useStderrOnEmptyStdout=true")
                .convertBodyTo(String.class, "utf-8")

                .transform().simple("${body.replace('\n', ' / 2\n')}") // the newline after the 2 needs to be there for the /usr/bin/bc exec command

                // If ffprobe processing succeeded?
                // Then find the middle of the video using /usr/bin/bc and Store video time position for thumbnail in header.
                .choice()
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .to("exec:/usr/bin/bc?args=")
                        .convertBodyTo(String.class, "utf-8")
                        .transform().simple("${body.replace('\n', '\"\"')}")

                        // If ffprobe and /usr/bin/bc processing succeeded?
                        // Store video time position for thumbnail in header.
                        .choice()
                            .when().simple("${headers.CamelExecExitValue} == 0")
                                .setHeader("videoThumbnailTimePosition").simple("${body}")
                                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Adding video time position for thumbnail at ${header.videoThumbnailTimePosition}sec. to header.")
                            .endChoice()
                            .otherwise()
                                .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Video thumbnail time position /usr/bin/bc calculation failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                            .endChoice()
//                        .end()
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Video thumbnail time position processing failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()
                .log(DEBUG, CT_LOG_NAME, "${id} Finished Derivatives Video thumbnail time position processing.");


        from("direct:processDerivativesVideoThumbnail").routeId("DerivativesProcessVideoThumbnail")
                .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Started Video Thumbnail processing...")
                // Make an TN datastream.
                // Create a video thumbnail using ffmpeg.

                // alt command that picks the best image for a thumbnail "ffmpeg -i - -vf thumbnail -frames:v 1 -"
                //.recipientList().simple("exec:ffmpeg?args=-ss ${header.videoThumbnailTimePosition} -i ${header.CamelFileNameProduced}.mp4 -vf thumbnail -frames:v 1 ${header.CamelFileNameProduced}-TN.jpg&useStderrOnEmptyStdout=true")
                .recipientList().simple("exec:ffmpeg?args=-ss ${header.videoThumbnailTimePosition} -i ${header.videoInput} -vcodec mjpeg -vframes 1 -an -f rawvideo ${header.CamelFileNameProduced}-TN.jpg")

                // If ffmpeg thumbnail processing succeeded? Store an MP4 datastream on the FDO.
                .choice()
                    .when().simple("${headers.CamelExecExitValue} == 0")
                        .setBody().simple("${header.CamelFileNameProduced}-TN.jpg")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=TN&type=video/jpg&dsLabel=TN&group=M&versionable=false")

                        .log(DEBUG, CT_LOG_NAME, "${id} Derivatives: Adding Video Thumbnail To TN.")
                    .endChoice()
                    .otherwise()
                        // creating video thumbnail failed set the thumbnail to the default video thumbnail
                        .setBody().simple("config/video-thumbnails/video.png")

                        .to("reader:file")
                        .to("fedora:addDatastream?name=TN&type=video/jpg&dsLabel=TN&group=M&versionable=false")

                        .log(ERROR, CT_LOG_NAME, "${id} Derivatives: Video Thumbnail Processing Failed. PID: ${headers.CamelFedoraPid}  Error Code: ${headers.CamelExecExitValue}")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id} Finished Video Thumbnail processing.");


        from("direct:isFaceBlur").routeId("DerivativesIsFaceBlur")

                .setHeader("CamelHttpMethod").simple("GET")

                // Get Parent Datastreams
                .toD("{{si.fedora.host}}/objects/${header.isAdministeredBy}/datastreams?format=xml").id("getParentDs")

                // TODO: also check parent RELS-EXT for <fedora-model:hasModel rdf:resource="info:fedora/si:cameraTrapCModel")
                .choice()
                    // see if the parent object has a ct manifest
                    .when().xpath("boolean(//@dsid='MANIFEST')", ns)
                        .setHeader("CamelHttpMethod").simple("GET")

                        // Get the object label from object profile xml
                        .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}?format=xml").id("getObjectXMl")

                        .setHeader("dsLabel").xpath("/objectProfile:objectProfile/objectProfile:objLabel/text() ", String.class, ns)

                        // Get the manifest to check SpeciesScientificName
                        .toD("{{si.fedora.host}}/objects/${header.isAdministeredBy}/datastreams/MANIFEST/content").id("getParentManifest")

                        .setHeader("blurRequired").simple("false")
                        .setHeader("isBlurred").simple("false")

                        // use same fedora user as CT routes so jms msg will be filtered otherwise a loop will occur
                        .setHeader("Authorization").groovy("\"Basic \" + \"{{si.fedora.user}}:{{si.fedora.password}}\".bytes.encodeBase64().toString()")

                        // filter images that have SpeciesScientificName's that require face blur processing
                        .filter().xpath("//ImageSequence[Image[ImageId/text() = $in:dsLabel]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.wi.speciesScientificNameFaceBlur.filter'), text())] != ''", Boolean.class, ns)

                            .log(INFO, CT_LOG_NAME, "${id}: Found image that may contain a face... Face Processing Required...")
                            .setHeader("blurRequired").simple("true")
                            .setBody().simple("${header.CamelFileNameProduced}")

                            .setHeader("BlurSourceFile").simple("${header.CamelFileNameProduced}")

                            .to("reader:file")

                            .toD("exec:python?args={{si.ct.wi.faceBlur.script}} {{si.ct.wi.faceBlur.blur_value}} {{si.ct.wi.faceBlur.classifier}}").id("execPythonFaceblur")

                            .convertBodyTo(byte[].class)

                            .choice()
                                .when().simple("${header.CamelExecExitValue} != '0'")
                                    .log(ERROR, CT_LOG_NAME, "${id}: Face Blur Error!!! Use original image...\nCamelExecStderr:\n${header.CamelExecStderr}")
                                .endChoice()
                                .otherwise()
                                    .setHeader("isBlurred").simple("true")

                                    // Save the blurred image to staging
                                    .toD("file:{{staging.dir}}?fileName=${header.CamelFileName}_blur").id("saveFaceBlurOutputToStaging")

                                    // copy exif tags from the original image to the blurred image in staging
                                    .toD("exec:{{exiftool.path}}?args= -TagsFromFile ${header.BlurSourceFile} ${header.CamelFileNameProduced} -overwrite_original").id("exiftool")
                                .endChoice()
                            .end()

                            .setBody().simple("${header.CamelFileNameProduced}")

                            // put the blurred image with copied exif tags back on body
                            .to("reader:file").id("readExifBlurImageResource")

                            .setHeader("CamelHttpMethod").simple("PUT")

                            // replace the OBJ with the faceBlur image adding a log message
                            .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/OBJ?mimeType=image/jpeg&logMessage=faceBlurred").id("replaceOBJ")
                        .end()

                        .setHeader("SitePID").simple("${header.isAdministeredBy}")
                        .setHeader("CamelHttpMethod").simple("POST")

                        .to("velocity:file:config/templates/WI-SidoraTemplate.vsl")

                        .toD("{{si.fedora.host}}/objects/${header.CamelFedoraPid}/datastreams/SIDORA?mimeType=text/xml&dsLabel=SIDORA&group=X").id("addSidoraDS")
                    .endChoice()
                    .otherwise()
                        .log(INFO, CT_LOG_NAME, "${id}: No Face Blur Processing Required.")
                    .endChoice()
                .end()
                .setBody().simple("${header.CamelFileNameProduced}")

                .to("reader:file")
                .removeHeader("Authorization");

    }
}
