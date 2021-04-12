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

package edu.si.services.sidora.cameratrap.routes;

import edu.si.services.beans.fitsservlet.FITSServletRouteBuilder;
import edu.si.services.beans.velocityToolsHandler.VelocityToolsHandler;
import edu.si.services.camel.fcrepo.FcrepoConfiguration;
import edu.si.services.fedorarepo.FedoraObjectNotFoundException;
import edu.si.services.fedorarepo.aggregators.PidAggregationStrategy;
import edu.si.services.sidora.cameratrap.*;
import edu.si.services.sidora.cameratrap.processors.DeploymentPackageDataCleanUpProcessor;
import edu.si.services.sidora.cameratrap.processors.InFlightConceptCheckProcessor;
import edu.si.services.sidora.cameratrap.validation.CameraTrapValidationMessage;
import edu.si.services.sidora.cameratrap.validation.CameraTrapValidationMessageAggregationStrategy;
import edu.si.services.sidora.cameratrap.validation.DeploymentPackageValidator;
import edu.si.services.sidora.cameratrap.validation.PostIngestionValidator;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.apache.camel.support.builder.Namespaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.camel.LoggingLevel.*;

/**
 * @author jbirkhimer
 */
@Component
public class UnifiedCTIngestRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "si.ct.id")
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
//        bindToRegistry("inFlightConceptCheckProcessor", inFlightConceptCheckProcessor);

        Namespaces ns = new Namespaces();
        ns.add("objDatastreams", "http://www.fedora.info/definitions/1/0/access/");
        ns.add("findObjects", "http://www.fedora.info/definitions/1/0/types/");
        ns.add("ri", "http://www.w3.org/2005/sparql-results#");
        ns.add("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        ns.add("fedora", "info:fedora/fedora-system:def/relations-external#");
        ns.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("eac", "urn:isbn:1-931666-33-4");
        ns.add("mods", "http://www.loc.gov/mods/v3");
        ns.add("svrl", "http://purl.oclc.org/dsdl/svrl");

//        errorHandler(noErrorHandler());

        onException(ConnectException.class, HttpOperationFailedException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.ct.connEx.redeliveryDelay}}")
                .maximumRedeliveries("{{min.connectEx.redeliveries}}")
                .retryAttemptedLogLevel(WARN)
                .log(DEBUG, CT_LOG_NAME, "[${routeId}] :: Error reported: ${exception.message}").id("logConnectException")
                .end();

        onException(FileNotFoundException.class)
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.ct.FNF.redeliveryDelay}}")
                .maximumRedeliveries(5)
                .retryAttemptedLogLevel(WARN)
                .end();

        onException(FedoraObjectNotFoundException.class)
                .onRedeliveryRef("inFlightConceptCheckProcessor")
                .useOriginalBody()
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .redeliveryDelay("{{si.ct.inflight.redeliveryDelay}}")
                .maximumRedeliveries(3)
                .retryAttemptedLogLevel(WARN)
                .retriesExhaustedLogLevel(WARN)
                .logExhaustedMessageHistory(false)
                .logContinued(true)
                .continued(true)
                .end();


        from("file:{{si.ct.uscbi.process.dir.path}}?delay={{si.ct.file.poll.delay}}&moveFailed={{si.ct.uscbi.process.error.dir.path}}&move={{si.ct.uscbi.process.done.dir.path}}&maxMessagesPerPoll=1").routeId("UnifiedCameraTrapStartProcessing")
                //from("file:{{si.ct.uscbi.process.dir.path}}?delay=9000&moveFailed=${bean:dataDirRollback.dataDirRollback(*, {{si.ct.uscbi.process.error.dir.path}}/${file:onlyname})}&move={{si.ct.uscbi.process.done.dir.path}}&maxMessagesPerPoll=1")

                // this onCompletion block will only be executed when the exchange is done being routed 
                // this callback is always triggered even if the exchange failed 
                .onCompletion()
                    //.process("deploymentPackageDataCleanUpProcessor")
                    .process(new DeploymentPackageDataCleanUpProcessor())
                    .log(INFO, CT_LOG_NAME, "${id}: Camera Trap deployment package data clean up removed ${header.deploymentDataDir} ...")
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Starting Camera Trap processing [ ${header.CamelFileNameOnly} ]...")

                // Needed for the Velocity tools. 
                .setHeader("esc").method(VelocityToolsHandler.class, "getVelocityEscapeTool")

                //If there is an issue with staging large archives we can can 'slow' down the route with delay below.
                .delay(6000)

                .log(INFO, CT_LOG_NAME, "${id}: Starting Extraction")
                .to("extractor:extract?location={{si.ct.uscbi.data.dir.path}}")
                .log(INFO, CT_LOG_NAME, "${id}: Finished Extraction")

                .setHeader("deploymentPackageId").simple("${header.CamelFileParent}")

                .setHeader("deploymentDataDir").simple("${header.CamelFileAbsolutePath}")
                .log(WARN, CT_LOG_NAME, "${id}: AbsolutePath: ${header.CamelFileAbsolutePath}")

                .threads(3, 5, "ctThreads").id("ctThreads")

                //If there is an issue with staging large archives we can can 'slow' down the route with delay below.
                .delay(1000)

                .transform().simple("deployment_manifest.xml")

                .to("reader:file?type=text")

                .log(WARN, CT_LOG_NAME, "${id}: Body: ${body}")

                // Validate against the XSD. 
                .to("validator:file:config/schemas/Unified_eMammalDeploymentManifest.xsd").id("xsdValidation")

                // Schematron validation. 
                .to("schematron:file:config/schemas/Unified_eMammalDeploymentManifest.sch").id("schematronValidation")
                .log(INFO, CT_LOG_NAME, "${id}: Schematron Validation Status: ${header.CamelSchematronValidationStatus}")
                .choice()
                    .when().simple("${header.CamelSchematronValidationStatus} == 'FAILED'")
                        .log(WARN, CT_LOG_NAME, "${id}: Schematron Validation Status: ${header.CamelSchematronValidationStatus}")
                        .log(WARN, CT_LOG_NAME, "${id}: Schematron Validation Report:\n ${header.CamelSchematronValidationReport}")

                        .setBody().xpath("//svrl:failed-assert/svrl:text/text()", List.class, ns, "CamelSchematronValidationReport")

                        .log(ERROR, CT_LOG_NAME, "${id}: Schematron Validation Error(s):\n ${body}")

                        .throwException(IllegalArgumentException.class, "Schematron validation failed.")
                    .endChoice()
                    .otherwise()
                        .log(INFO, CT_LOG_NAME, "${id}: Schematron validation succeeded.")
                    .endChoice()
                .end()

                .setHeader("ManifestXML").simple("${body}")

                // Validate image count and check filenames. 
                .to("direct:validatePackage")

                .log(DEBUG, CT_LOG_NAME, "${id}: Ingest dry-run flag: {{si.ct.uscbi.dryrun}}")

                .choice()
                    .when()
                        .simple("{{si.ct.uscbi.dryrun}} == true")
                        .log(INFO, CT_LOG_NAME, "${id}: Ingestion dryrun mode is enabled.. skipping the actual ingestion process")
                    .endChoice()
                    .otherwise()
                        // Process the deployment
                        .to("direct:processPackage")

                        // Delay some time for Fedora to catch up with RI sync before starting post validation
                        .log(INFO, CT_LOG_NAME, "${id}: Delaying for moment before starting the post ingest validation ...")
                        .delay(6000)

                        // Validate image count and check filenames.
                        .to("direct:validateIngest")

                        // Send message to EdanIds route for processing
                        .setHeader("addEdanIds").simple("true")

                        // header used for enabling test url value so that we can create new edan records for testing
                        // url = url_test_randomUUID
                        .setHeader("test_edan_url").simple("{{enable.edan.test}}")

                        .to("activemq:queue:{{edanIds.ct.queue}}").id("edanIdsActiveMQ")

                        .to("activemq:queue:{{sidoraCTSolr.queue}}").id("sidoraSolrActiveMQ")

                        .to("activemq:queue:{{sidora.wi.ct.queue}}").id("sidoraWIActiveMQ")
                    .endChoice()
                .end()
                .log(INFO, CT_LOG_NAME, "${id}: Finished Camera Trap processing! [ ${header.deploymentPackageId} ]");



        from("direct:validatePackage").routeId("UnifiedCameraTrapValidatePackage")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Package validation ...")

                .setHeader("ResourceFileType").simple(".JPG, .JPEG", String.class)
                .setHeader("ResourceCount").xpath("count(//ImageFileName)", Integer.class, ns)

                // AdjustedResourceCount is the number of resources that don't correspond to an empty sequence
                .setHeader("AdjustedResourceCount").xpath("count(//ImageSequence[not(ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.wi.emptySequence.filter'), text())])]/Image/ImageFileName)", Integer.class, ns)

                //.to("bean:deploymentPkgValidator?method=validateResourceCount")
                .bean(DeploymentPackageValidator.class, "validateResourceCount")

                //  Keeping the validator result in the header to allow the rest of the validation to go through and raise exception at the end of route.
                .setHeader("ValidateResourceCountResult").simple("${body}", Integer.class)

                .split().xpath("//ImageFileName/text()", String.class, ns, "ManifestXML")
                    .log(DEBUG, CT_LOG_NAME, "${id}: Split Resource: ${body}")

                    .setHeader("ImageFileName").simple("${body}", String.class)

                    .setHeader("ImageSequenceID").xpath("//ImageFileName[text()=$ImageFileName]/parent::Image/parent::ImageSequence/ImageSequenceId/text()", String.class, ns, "ManifestXML")

                    .log(DEBUG, CT_LOG_NAME, "${id}: ImageSequenceID: ${header.ImageSequenceID}")

                    // This throws a java.io.FileNotFoundException if the file does not exist.
                    .to("reader:file")

                    .log(DEBUG, CT_LOG_NAME, "${id}: CamelSplitSize: ${header.CamelSplitSize} CamelSplitIndex: ${header.CamelSplitIndex}")
                    // We should check the image count to see if it exactly matches the image file count.
                .end()

                // This throws custom exception when the resource count validator was unsuccessful.
                .choice()
                    .when(simple("${header.ValidateResourceCountResult} == '0'"))
                        .throwException(DeploymentPackageException.class, "The resource counts do not match: extra resource(s) found in the file system than manifest")
                    .endChoice()
                    .when(simple("${header.ValidateResourceCountResult} == '-1'"))
                        .throwException(DeploymentPackageException.class, "The resource counts do not match: less resource(s) found in the file system than manifest: ValidateResourceCountResult: ${header.ValidateResourceCountResult}")
                    .endChoice()
                .end()
                .log(INFO, CT_LOG_NAME, "${id}: Finished Package validation.");



        from("direct:validateIngest").routeId("UnifiedCameraTrapValidateIngest")

                .log(INFO, CT_LOG_NAME, "${id}: Starting Post validation ...")

                .setHeader("validationErrors").simple("validationErrors")

                .to("direct:validatePostIngestResourceCount")

                // validate datastreams exists for various object types
                .to("direct:validateDatastreams")

                // Validate Ingest Datastream Metadata Fields
                .to("direct:validateDatastreamFields")

                .log(INFO, CT_LOG_NAME, "${id}: Finished Post validation");



        from("direct:validatePostIngestResourceCount").routeId("UnifiedCameraTrapValidatePostIngestResourceCount")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Post Ingestion Resource Count validations...")

                // RELS-EXT Resource reference count for validation.  The number includes the observation resource objs
                .setHeader("RelsExtResourceCount").xpath("count(/rdf:RDF/rdf:Description/fedora:hasResource)", String.class, ns)

                .to("direct:validatePostResourceCount")

                .setHeader("CamelFedoraPid").simple("${headers.SitePID}")
                .to("fedora://getDatastreamDissemination?dsId=RELS-EXT&exchangePattern=InOut")

                .split()
                    .tokenize("//fedora:hasResource")
                    .streaming()

                    // Stashing the resource object PID in the header in case of RI search redelivery attempts for later use
                    .setHeader("ValidationResourceObjectPID").xpath("//fedora:hasResource/@rdf:resource/substring-after(., 'info:fedora/')", String.class, ns) //TODO: saxon true (verify this is working)

                    // This api will query Fedora relational db and PID is the only indexed field.  There may be performance impact if the query search includes more than PID
                    .toD("fcrepo:objects?query=pid%7E${header.ValidationResourceObjectPID}&pid=true&resultFormat=xml")

                    .setBody().xpath("boolean(//findObjects:objectFields/findObjects:pid/text())", Boolean.class, ns) //TODO: saxon true (verify this is working)

                    .choice()
                        .when().simple("${body} == false")
                            //.to("bean:cameraTrapValidationMessage?method=createValidationMessage(${header.deploymentPackageId}, ${header.ValidationResourceObjectPID}, 'Resource Object not found from Fedora Repository', ${body})")
                            .bean(CameraTrapValidationMessage.class, "createValidationMessage(${header.deploymentPackageId}, ${header.ValidationResourceObjectPID}, 'Resource Object not found from Fedora Repository', ${bodyAs(Boolean)})")
                            .to("direct:validationErrorMessageAggregationStrategy")
                            .log(WARN, CT_LOG_NAME, "${id}: Resource Object not found from Fedora Repository")
                        .endChoice()
                        .otherwise()
                            //.to("bean:cameraTrapValidationMessage?method=createValidationMessage(${header.deploymentPackageId}, ${header.ValidationResourceObjectPID}, 'Resource Object found from Fedora Repository', ${body})")
                            .bean(CameraTrapValidationMessage.class, "createValidationMessage(${header.deploymentPackageId}, ${header.ValidationResourceObjectPID}, 'Resource Object found from Fedora Repository', ${bodyAs(Boolean)})")
                            .to("direct:validationErrorMessageAggregationStrategy")
                            .log(DEBUG, CT_LOG_NAME, "${id}: Resource Object found from Fedora Repository")
                        .endChoice()
                    .end()

                    .log(INFO, CT_LOG_NAME, "${id}: CamelSplitSize: ${header.CamelSplitSize} CamelSplitIndex: ${header.CamelSplitIndex}")
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished Post Ingestion Resource Count validations.");



        from("direct:validateDatastreams").routeId("UnifiedCameraTrapValidateDatastreams")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Datastream validations...")

                // Validate Project Object datastreams
                .setHeader("ValidationDatastreamObjectType").simple("Project")
                .setHeader("ValidationDatastreamObjectPID").simple("${headers.ProjectPID}")
                .setHeader("DatastreamTypesCheck").simple("DC, EAC-CPF, RELS-EXT", String.class)
                .to("direct:validateObjectDatastreams")

                // TODO: Validation for Sub-Project 
                // Validate SubProject Object datastreams 
                .choice()
                    .when().header("SubProjectPID")
                        .setHeader("ValidationDatastreamObjectType").simple("SubProject")
                        .setHeader("ValidationDatastreamObjectPID").simple("${headers.SubProjectPID}")
                        .setHeader("DatastreamTypesCheck").simple("DC, EAC-CPF, RELS-EXT", String.class)
                        .to("direct:validateObjectDatastreams")
                    .endChoice()
                .end()

                // Validate Plot Object datastreams 
                .choice()
                    .when()
                    .header("PlotPID")
                        .setHeader("ValidationDatastreamObjectType").simple("Plot")
                        .setHeader("ValidationDatastreamObjectPID").simple("${headers.PlotPID}")
                        .setHeader("DatastreamTypesCheck").simple("DC, FGDC-CTPlot, RELS-EXT", String.class)
                        .to("direct:validateObjectDatastreams")
                    .endChoice()
                .end()

                // Validate Deployment Object datastreams 
                .setHeader("ValidationDatastreamObjectType").simple("Deployment")
                .setHeader("ValidationDatastreamObjectPID").simple("${headers.SitePID}")
                .setHeader("DatastreamTypesCheck").simple("DC, MANIFEST, FGDC, RELS-EXT", String.class)
                .to("direct:validateObjectDatastreams")

                // Validate Image Resource Object datastreams 
                .setHeader("ValidationDatastreamObjectType").simple("Image Resource")
                .setHeader("ValidationDatastreamObjectPID").simple("${headers.ImageResourcePID}")
                .setHeader("DatastreamTypesCheck").simple("DC, OBJ, TN, RELS-EXT, FITS, MODS", String.class)

                .choice()
                    .when()
                        .simple("${headers.ImageResourcePID}")
                        .to("direct:validateObjectDatastreams")
                    .endChoice()
                .end()

                // Validate Researcher Observation Object datastreams 
                .setHeader("ValidationDatastreamObjectType").simple("Researcher Observation")
                .setHeader("ValidationDatastreamObjectPID").simple("${headers.ResearcherObservationPID}")
                .setHeader("DatastreamTypesCheck").simple("DC, OBJ, CSV, RELS-EXT, FGDC", String.class)
                .to("direct:validateObjectDatastreams")

                // Validate Volunteer Observation Object datastreams 
                .choice()
                    .when().header("VolunteerObservationPID")
                        .setHeader("ValidationDatastreamObjectType").simple("Volunteer Observation")
                        .setHeader("ValidationDatastreamObjectPID").simple("${headers.VolunteerObservationPID}")
                        .setHeader("DatastreamTypesCheck").simple("DC, OBJ, CSV, RELS-EXT, FGDC", String.class)
                        .to("direct:validateObjectDatastreams")
                    .endChoice()
                .end()

                // Validate Image Observation Object datastreams 
                .choice()
                    .when().header("ImageObservationPID")
                        .setHeader("ValidationDatastreamObjectType").simple("Image Observation")
                        .setHeader("ValidationDatastreamObjectPID").simple("${headers.ImageObservationPID}")
                        .setHeader("DatastreamTypesCheck").simple("DC, OBJ, CSV, RELS-EXT, FGDC", String.class)
                        .to("direct:validateObjectDatastreams")
                    .endChoice()
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished Datastream validations.");



        from("direct:validateObjectDatastreams").routeId("UnifiedCameraTrapValidateObjectDatastreams")
                .log(INFO, CT_LOG_NAME, "${id}: Starting ${header.ValidationDatastreamObjectType} Object Datastream validations...")

                .setHeader("CamelHttpMethod").simple("GET")

                .toD("fcrepo:objects/${header.ValidationDatastreamObjectPID}/datastreams?format=xml")

                .choice()
                    .when().simple("${header.CamelHttpResponseCode} == 200")

                        .setHeader("FedoraDatastreamIDsFound").xpath("string-join(//objDatastreams:datastream/@dsid, ',')", String.class, ns)

                        //.to("bean:postIngestionValidator?method=validateDatastreamExists")
                        .bean(PostIngestionValidator.class, "validateDatastreamExists")

                        .filter(simple("${body} == false"))
                            //.to("bean:cameraTrapValidationMessage?method=createValidationMessage(${header.deploymentPackageId}, '${header.ValidationDatastreamObjectType} Object Datastreams validation failed', ${body})")
                            .bean(CameraTrapValidationMessage.class, "createValidationMessage(${header.deploymentPackageId}, '${header.ValidationDatastreamObjectType} Object Datastreams validation failed', ${bodyAs(Boolean)})")
                            .to("direct:validationErrorMessageAggregationStrategy")
                            .log(WARN, CT_LOG_NAME, "${id}: ${header.ValidationDatastreamObjectType} Object Datastreams validation failed: ${body}")
                        .end()

                    .endChoice()
                    .otherwise()
                        //.to("bean:cameraTrapValidationMessage?method=createValidationMessage(${header.deploymentPackageId}, '${header.ValidationDatastreamObjectType} Object with PID: ${header.ValidationDatastreamObjectPID} not found in Fedora.. skipping datastreams validation', false)")
                        .bean(CameraTrapValidationMessage.class, "createValidationMessage(${header.deploymentPackageId}, '${header.ValidationDatastreamObjectType} Object with PID: ${header.ValidationDatastreamObjectPID} not found in Fedora.. skipping datastreams validation', false)")
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .log(WARN, CT_LOG_NAME, "${id}: ${header.ValidationDatastreamObjectType} Object with PID: ${header.ValidationDatastreamObjectPID} not found in Fedora.. skipping datastreams validation")
                    .endChoice()
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished ${header.ValidationDatastreamObjectType} Object Datastream validations.");



        from("direct:processPackage").routeId("UnifiedCameraTrapProcessPackage")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Package processing...")

                .to("direct:processParents")
                .to("direct:processSite")

                .log(INFO, CT_LOG_NAME, "${id}: Finished Package processing.");



        from("direct:processSite").routeId("UnifiedCameraTrapProcessSite")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Site processing... ")

                // Stash the parent PID which could be a Plot or a Sub-project.
                .setHeader("ParentPID").simple("${header.CamelFedoraPid}")
                .setHeader("DeploymentCorrelationId").xpath("//CameraDeploymentID", String.class, ns, "ManifestXML")
                .setHeader("CamelFedoraLabel").xpath("//CameraSiteName", String.class, ns, "ManifestXML")

                .log(DEBUG, CT_LOG_NAME, "${id}: Site: Alternate Id: ${header.DeploymentCorrelationId} Label: ${header.CamelFedoraLabel}  Parent PID: ${header.CamelFedoraPid}")

                .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}")

                // Get the DC datastream for the site 
                .to("fedora:getDatastreamDissemination?dsId=DC&exchangePattern=InOut")
                .convertBodyTo(String.class)

                //Update the DC datastream with with dc:identifier for the Alternate ID
                .to("xslt-saxon:file:config/xslt/addAltId2DC.xsl").id("xsltSiteSIdoraConcept2DC")
                .to("fedora:addDatastream?name=DC&type=text/xml&group=X").id("addSiteDC")

                .log(DEBUG, CT_LOG_NAME, "${id}: Add Relation: Parent PID: ${header.ParentPID} Child PID: ${header.CamelFedoraPid}")
                .to("fedora:hasConcept?parentPid=${header.ParentPID}&childPid=${header.CamelFedoraPid}")
                .log(DEBUG, CT_LOG_NAME, "${id}: Add Relation: Status: ${header.CamelFedoraStatus}")

                // For new deployments we need to reload the parent tree node in workbench
                .setHeader("workbenchNodePid").simple("${header.ParentPID}")

                .filter().simple("{{enable.workbench.reload.pid.route}} == true")
                    .to("direct:workbenchReloadPid").id("siteWorkbenchReloadPid")
                .end()

                .to("direct:getCameraMakeModel")
                .setBody().simple("${header.ManifestXML}")

                .multicast()
                    .to("direct:addManifestDataStream")
                    .to("direct:addFGDCDataStream")
                    .to("direct:processResources")
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished Site processing.");



        from("direct:getCameraMakeModel").routeId("UnifiedCameraTrapGetCameraMakeModel")
                .onException(SocketException.class)
                    .useExponentialBackOff()
                    .backOffMultiplier(2)
                    .redeliveryDelay("{{si.ct.connEx.redeliveryDelay}}")
                    .maximumRedeliveries("{{min.socketEx.redeliveries}}")
                    .logRetryAttempted(true)
                    .continued(true)
                    .log(WARN, CT_LOG_NAME, "[${routeId}] :: FITS web service request failed!!! Error reported: ${exception.message}").id("getCameraMakeModelFitsServiceException")
                .end()

                    /*.setHeader("imageName").xpath("//ImageSequence[1]/Image[1]/ImageFileName/text()", String.class, ns, "ManifestXML")
                    .log(INFO, CT_LOG_NAME, "${id}: Getting CameraMakeModel from image: ${header.imageName}")*/

                .setHeader("imageName").xpath("//ImageSequence[1]/Image[1]/ImageFileName/text()", String.class, ns, "ManifestXML")
                .setHeader("CamelFileName").simple("${header.deploymentDataDir}/${header.imageName}")

                .log(INFO, CT_LOG_NAME, "${id}: Getting CameraMakeModel from image: ${header.imageName}")

                /* Ensure that we actually have a 200 response from the fits web service as this header could have
                been set upstream by another http request and that we are able to use a POST rather than a GET accidentally.*/

                // Not sure this is needed.
                .setBody(simple("${null}"))

                .removeHeaders("CamelHttpMethod|CamelHttpResponseCode")

                // Get the FITS analysis of the file.
                .to("direct:getFITSReport")

                // <convertBodyTo type="java.lang.String" charset="UTF-8"/> 

                .log(DEBUG, CT_LOG_NAME, "${id} FITS Output:\n${body}")

                .choice()
                    .when().simple("${header.CamelHttpResponseCode} == 200")
                        .setHeader("cameraMake").xpath("/fits:fits/fits:metadata/fits:image/fits:digitalCameraManufacturer/text()", String.class, ns)
                        .setHeader("cameraModel").xpath("/fits:fits/fits:metadata/fits:image/fits:digitalCameraModelName/text()", String.class, ns)

                        .log(INFO, CT_LOG_NAME, "${id}: Site FGDC Camera Make: ${header.cameraMake}, Model: ${header.cameraModel}")
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id}: FITS processing failed for ${routeId}")
                    .endChoice()
                .end()

                .removeHeader("imageName")
                .log(INFO, CT_LOG_NAME, "${id}: Finished Getting Camera Make/Model...");



        from("direct:addManifestDataStream").routeId("UnifiedCameraTrapAddManifestDataStream")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Manifest processing ...")

                .to("fedora:addDatastream?name=MANIFEST&type=text/xml&group=M&dsLabel=MANIFEST")

                .log(INFO, CT_LOG_NAME, "${id}: Finished Manifest processing.");



        from("direct:addFGDCDataStream").routeId("UnifiedCameraTrapAddFGDCDataStream")
                .log(INFO, CT_LOG_NAME, "${id}: Starting FGDC processing ...")

                .to("xslt-saxon:file:config/xslt/ManifestDeployment.xsl")
                .to("fedora:addDatastream?name=FGDC&type=text/xml&group=M&dsLabel=FGDC Record")

                .log(INFO, CT_LOG_NAME, "${id}: Finished FGDC processing.");



        from("direct:processResources").routeId("UnifiedCameraTrapProcessResources")
                .log(INFO, CT_LOG_NAME, "${id}: Starting resource processing ...")

                .setHeader("SitePID").simple("${header.CamelFedoraPid}")
                .setHeader("SiteId").simple("${header.DeploymentCorrelationId}")
                .setHeader("SiteName").simple("${header.CamelFedoraLabel}")
                .setHeader("ImageCount").xpath("count(//ImageFileName)", String.class, ns)
                .log(INFO, CT_LOG_NAME, "${id}: Site PID: ${header.SitePID}  Image Count: ${header.ImageCount}")

                .setHeader("skippedImageCount").simple("0", Integer.class)

                // There may be several kinds of resources with zero or more instances.
                .split()
                    .xpath("//ImageSequence[not(ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.wi.emptySequence.filter'), text())])]/Image/ImageFileName/text()", "ManifestXML")
                    .aggregationStrategy(new PidAggregationStrategy())

                        // Filter out resources that match an empty sequence from split
                        .log(INFO, CT_LOG_NAME, "${id}: Split Resource: ${body}")
                        .setBody(simple("${body}", String.class))

                        .to("direct:addImageResource")

                        .log(DEBUG, CT_LOG_NAME, "${id}: Created Image Resource: ${body}.")
                        .log(INFO, CT_LOG_NAME, "${id}: CamelSplitSize: ${header.CamelSplitSize} CamelSplitIndex: ${header.CamelSplitIndex}")
                .end()

                // There is only one observation resource of each kind and all the observations are aggregated.

                // Researcher is required for Unified CT Ingest.
                .to("direct:addResearcherObservationResource")

                // Volunteer is optional for Unified CT Ingest.
                .filter().xpath("boolean(//VolunteerIdentifications/Identification/IUCNId/text()[1])", String.class, ns, "ManifestXML")
                    .to("direct:addVolunteerObservationResource")
                .end()

                // Image is optional for Unified CT Ingest.
                .filter().xpath("boolean(//ImageIdentifications/Identification/IUCNId/text()[1])", String.class, ns, "ManifestXML")
                    .to("direct:addImageObservationResource")
                .end()

                // Add the RELS-EXT datastream in one operation to gain about 70% of the total efficiency.
                .to("velocity:file:config/templates/CTSiteTemplate.vsl")
                .toD("fedora:addDatastream?pid=${header.SitePID}&name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                .log(INFO, CT_LOG_NAME, "${id}: Finished resource processing.");



        from("direct:addImageResource").routeId("UnifiedCameraTrapAddImageResource").noTracing()
                .log(INFO, CT_LOG_NAME, "${id}: Started Image processing ...")

                // Spelled this way for the MODS XSLT parameter.
                .setHeader("imageid").xpath("//ImageFileName[text()=$in:body]/parent::Image/ImageId/text()", String.class, ns, "ManifestXML")

                // Image Sequence ID used in MODS datastream. 
                .setHeader("ImageSequenceID").xpath("//ImageFileName[text()=$in:body]/parent::Image/parent::ImageSequence/ImageSequenceId/text()", String.class, ns, "ManifestXML")

                // Used for MODS validation 
                .setHeader("modsImageSequenceId").xpath("//ImageFileName[text()=$in:body]/parent::Image/parent::ImageSequence/ImageSequenceId/text()", String.class, ns, "ManifestXML")

                .setHeader("ImageSequenceIndex").xpath("//ImageFileName[text()=$in:body]/parent::Image/ImageOrder/text()", Integer.class, ns, "ManifestXML")

                .filter().simple("${header.ImageSequenceIndex} == 0")
                    // Stop processing this deployment.
                    .log(WARN, CT_LOG_NAME, "${id}: Image sequence does not exist.")
                    .throwException(IllegalArgumentException.class, "Image sequence does not exist.")
                .end()

                .setHeader("ImageSequenceCount").xpath("count(//Image[../ImageSequenceId=$ImageSequenceID])", Integer.class, ns, "ManifestXML")

                .log(DEBUG, CT_LOG_NAME, "${id}: Label: ${body}  imageid: ${header.imageid}  ImageSequenceIndex: ${header.ImageSequenceIndex}")
                .log(DEBUG, CT_LOG_NAME, "${id}: Label: ${body}  ImageSequenceID: ${header.ImageSequenceID}  ImageSequenceCount: ${header.ImageSequenceCount}")

                .to("reader:file").id("readImageResource")

                .log(DEBUG, CT_LOG_NAME, "Image read")

                .toD("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}&label=${header.imageid}").id("createResourcePID")
                .toD("fedora:addDatastream?name=OBJ&type=image/jpeg&group=M&dsLabel=${header.imageid}&versionable=true").id("addResourceOriginalVersion")

                .setHeader("blurRequired").simple("false")
                .setHeader("isBlurred").simple("false")

                // Check SpeciesScientificName for Homo sapien, Camera Trapper, Homo sapiens and blur out faces 
                // Any failure during Face Blur will revert back to the original image 
                .filter().xpath("//ImageSequence[Image[ImageId/text() = $in:imageid]]/ResearcherIdentifications/Identification/SpeciesScientificName[contains(function:properties('si.ct.wi.speciesScientificNameFaceBlur.filter'), text())] != ''", Boolean.class, ns, "ManifestXML").id("speciesScientificNameFaceBlurChoice")

                        .log(INFO, CT_LOG_NAME, "${id}: Found image that may contain a face...")

                        .setHeader("CamelExecCommandLogLevel", simple("DEBUG"))

                        .setHeader("blurRequired").simple("true")
                        .toD("exec:python?args={{si.ct.wi.faceBlur.script}} {{si.ct.wi.faceBlur.blur_value}} {{si.ct.wi.faceBlur.classifier}}").id("execPythonFaceblur")
                        .convertBodyTo(byte[].class)

                        .choice()
                            .when().simple("${header.CamelExecExitValue} != 0")
                                .log(ERROR, CT_LOG_NAME, "${id}: Face Blur Error on ${header.imageid}!!! Use original image...\nCamelExecStderr:\n${header.CamelExecStderr}")
                                .setBody(simple("${header.CamelFileAbsolutePath}"))
                                .to("reader:file").id("blurErrorUseOriginalImage")
                            .endChoice()
                            .otherwise()
                                .setHeader("isBlurred").simple("true")

                                // Save the blurred image to staging
                                .toD("file:{{processing.dir.base.path}}/staging?fileName=${header.CamelFileName}").id("saveFaceBlurOutputToStaging")

                                // copy exif tags from the original image to the blurred image in staging
                                .toD("exec:{{exiftool.path}}?args=-TagsFromFile ${header.CamelFileAbsolutePath} ${header.CamelFileNameProduced} -overwrite_original").id("execExiftool")

                                .filter().simple("${header.CamelExecExitValue} != 0")
                                    .log(ERROR, CT_LOG_NAME, "${id}: ExifTool Error Copying Image Metadata for ${header.imageid}!!!\nCamelExecStderr:\n${header.CamelExecStderr}")
                                .end()

                                .setBody(simple("${header.CamelFileNameProduced}"))
                                // put the blurred image with copied exif tags back on body
                                .to("reader:file").id("readOrigBlurImageResource")
                                //.to("reader:file").id("readImageMetadataCopyFile")
                                .toD("fedora:addDatastream?name=OBJ&type=image/jpeg&group=M&dsLabel=${header.imageid}&versionable=true").id("addResourceBlurVersion")
                            .endChoice()
                        .end()
                .end()

                .multicast()
                    .to("direct:createThumbnail").id("addResourceCreateThumbnail")
                    // to("direct:createArchivalImage")
                    // We may also want a DISPLAY datastream.
                .end()

                .to("velocity:file:config/templates/CTImageResourceTemplate.vsl")
                .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")
                .to("direct:addFITSDataStream")
                .to("direct:addMODSDataStream")

                .to("velocity:file:config/templates/WI-SidoraTemplate.vsl")
                .to("fedora:addDatastream?name=SIDORA&type=text/xml&group=X&dsLabel=SIDORA&versionable=true").id("addSidoraDS")

                /*We only want to validate one ImageSequenceId so use a Filter on the CamelSplitIndex from the split in the processResources calling the addMODSDatastream to get the PID for the first MODS datastream being created */

                .filter().simple("${header.CamelSplitIndex} <= 0")
                    .setHeader("ImageResourcePID").simple("${header.CamelFedoraPid}")
                    .log(INFO, CT_LOG_NAME, "${id}: Setting ImageResourcePID: PID: ${header.CamelFedoraPid} | CamelSplitIndex: ${header.CamelSplitIndex}")
                .end()

                // The current route only handles JPG.
                .choice()
                    // If the image is a JPEG? Do nothing.
                    .when().simple("${header.dsMIME} == 'image/jpg' || ${header.dsMIME} == 'image/jpeg' || ${header.dsMIME} == 'image/jpe'")
                        .log(DEBUG, CT_LOG_NAME, "${id}: UnifiedCameraTrapAddImageResource: Found JPEG.")
                        // Future: Make a JPEG2000 archival image and store it in the MASTER datastream.
                    .endChoice()
                        // Just warn for now.
                        .otherwise()
                        .log(WARN, CT_LOG_NAME, "${id}: UnifiedCameraTrapAddImageResource: Found non-JPEG Image.")
                    .endChoice()
                .end()

                // Delete the temporary blur image for exif copying. Note: This approach is Unix specific.
                .recipientList().simple("exec:rm?args= -f ${header.CamelFileNameProduced}")

                .choice()
                    .when().simple("${header.CamelExecExitValue} != 0")
                        // We also want to proactively tell monitoring
                        .log(WARN, CT_LOG_NAME, "${id} ${routeId}: Unable to delete temp blurred file. Filename: ${headers.CamelFileNameProduced}")
                    .endChoice()
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished Image processing.");



        from("direct:addResearcherObservationResource").routeId("UnifiedCameraTrapAddResearcherObservationResource")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Researcher Observation data processing ...")

                // For Researcher observations.
                .setBody().simple("${header.ManifestXML}")
                .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}&label=Researcher Observations")

                .log(DEBUG, CT_LOG_NAME, "${id}: Research Observation Resource PID: ${header.CamelFedoraPid}")

                .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.CamelFedoraPid}")

                .log(DEBUG, CT_LOG_NAME, "${id}: Aggregation: ${header.PIDAggregation}")

                .to("xslt:file:config/xslt/ResearcherObservation.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: Researcher Observations: \n${body}")

                .transform().xpath("//researcher/text()", String.class, ns)
//                .convertBodyTo(String.class)

                .toD("fedora:addDatastream?name=OBJ&type=text/csv&group=M&dsLabel=Researcher Observations&versionable=true")
                .to("fedora:addDatastream?name=CSV&type=text/csv&group=M&dsLabel=CSV&versionable=true")
                .to("velocity:file:config/templates/CTDatasetResourceTemplate.vsl")
                .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                // This is a quick fix but it is not elegant to fetch the manifest again to transform it.
                .setBody().simple("${header.ManifestXML}")

                .to("xslt-saxon:file:config/xslt/ManifestResearcherObservation.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: FGDC XML -\n${body}")

                .to("fedora:addDatastream?name=FGDC&type=text/xml&group=X&dsLabel=FGDC Record")

                .setHeader("ResourceCount").simple("${header.ResourceCount}++")
                .setHeader("AdjustedResourceCount").simple("${header.AdjustedResourceCount}++")
                .setHeader("ResearcherObservationPID").simple("${header.CamelFedoraPid}")

                .log(INFO, CT_LOG_NAME, "${id}: Finished Researcher Observation data processing.");




        from("direct:addVolunteerObservationResource").routeId("UnifiedCameraTrapAddVolunteerObservationResource")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Volunteer Observation data processing ...")

                // For Volunteer observations.
                .setBody().simple("${header.ManifestXML}")

                .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}&label=Volunteer Observations")

                .log(DEBUG, CT_LOG_NAME, "${id}: Volunteer Observation Resource PID: ${header.CamelFedoraPid}")

                .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.CamelFedoraPid}")

                .log(DEBUG, CT_LOG_NAME, "${id}: Aggregation: ${header.PIDAggregation}")

                .setBody().simple("${header.ManifestXML}")

                .to("xslt:file:config/xslt/VolunteerObservation.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: Volunteer Observations: \n${body}")

                .transform().xpath("//volunteer/text()", String.class, ns)
//                .convertBodyTo(String.class)

                .toD("fedora:addDatastream?name=OBJ&type=text/csv&group=M&dsLabel=Volunteer Observations&versionable=true")
                .to("fedora:addDatastream?name=CSV&type=text/csv&group=M&dsLabel=CSV&versionable=true")
                .to("velocity:file:config/templates/CTDatasetResourceTemplate.vsl")
                .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                // This is a quick fix but it is not elegant to fetch the manifest again to transform it.
                .setBody().simple("${header.ManifestXML}")

                .to("xslt-saxon:file:config/xslt/ManifestVolunteerObservation.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: FGDC XML -\n${body}")

                .to("fedora:addDatastream?name=FGDC&type=text/xml&group=X&dsLabel=FGDC Record")

                .setHeader("ResourceCount").simple("${header.ResourceCount}++")
                .setHeader("AdjustedResourceCount").simple("${header.AdjustedResourceCount}++")
                .setHeader("VolunteerObservationPID").simple("${header.CamelFedoraPid}")

                .log(INFO, CT_LOG_NAME, "${id}: Finished Volunteer Observation data processing.");



        from("direct:addImageObservationResource").routeId("UnifiedCameraTrapAddImageObservationResource")
                .log(INFO, CT_LOG_NAME, "${id}: Starting Image Observation data processing ...")

                // For Image observations. 
                .setBody().simple("${header.ManifestXML}")

                .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}&label=Image Observations")

                .log(DEBUG, CT_LOG_NAME, "${id}: Image Observation Resource PID: ${header.CamelFedoraPid}")

                .setHeader("PIDAggregation").simple("${header.PIDAggregation},${header.CamelFedoraPid}")

                .log(DEBUG, CT_LOG_NAME, "${id}: Aggregation: ${header.PIDAggregation}")

                .to("xslt:file:config/xslt/ImageObservation.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: Image Observations: \n${body}")

                .transform().xpath("//image/text()", String.class, ns)
//                .convertBodyTo(String.class)

                .toD("fedora:addDatastream?name=OBJ&type=text/csv&group=M&dsLabel=Image Observations&versionable=true")
                .to("fedora:addDatastream?name=CSV&type=text/csv&group=M&dsLabel=CSV&versionable=true")
                .to("velocity:file:config/templates/CTDatasetResourceTemplate.vsl")
                .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                // This is a quick fix but it is not elegant to fetch the manifest again to transform it. 
                .setBody().simple("${header.ManifestXML}")

                .to("xslt-saxon:file:config/xslt/ManifestImageObservation.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: FGDC XML -\n${body}")

                .to("fedora:addDatastream?name=FGDC&type=text/xml&group=X&dsLabel=FGDC Record")

                .setHeader("ResourceCount").simple("${header.ResourceCount}++")
                .setHeader("AdjustedResourceCount").simple("${header.AdjustedResourceCount}++")
                .setHeader("ImageObservationPID").simple("${header.CamelFedoraPid}")

                .log(INFO, CT_LOG_NAME, "${id}: Finished Image Observation data processing.");

        from("direct:addFITSDataStream").routeId("UnifiedCameraTrapAddFITSDataStream")
                .onException(SocketException.class)
                    .useExponentialBackOff()
                    .backOffMultiplier(2)
                    .redeliveryDelay("{{si.ct.connEx.redeliveryDelay}}")
                    .maximumRedeliveries("{{min.socketEx.redeliveries}}")
                    .logRetryAttempted(true)
                    .retryAttemptedLogLevel(WARN)
                    .continued(true)
                    .log(WARN, CT_LOG_NAME, "[${routeId}] :: FITS web service request failed!!! Error reported: ${exception.message}").id("fitsServiceException")
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Started processing FITS ...")

                // Create a FITS derivative using Harvard FITS. 
                .setHeader("CamelFileName").simple("${header.CamelFileAbsolutePath}")

                /* Ensure that we actually have a 200 response from the fits web service as this header could have been set upstream by another http request and that we are able to use a POST rather than a GET accidentally. */

                // Not sure this is needed.
                .setBody(simple("${null}"))
                .removeHeaders("CamelHttpMethod|CamelHttpResponseCode")

                // Get the FITS analysis of the file.
                .to("direct:getFITSReport").id("UnifiedCameraTrapAddFITSDataStream_getFITSReport")

                // <convertBodyTo type="java.lang.String" charset="UTF-8"/> 

                .log(DEBUG, CT_LOG_NAME, "${id} FITS Output:\n${body}")

                .choice()
                    // If FITS processing succeeded? Store a FITS datastream on the FDO.
                    .when().simple("${header.CamelHttpResponseCode} == 200")
                        .setHeader("dsMIME").xpath("/fits:fits/fits:identification/fits:identity[1]/@mimetype", String.class, ns)
                        .log(DEBUG, CT_LOG_NAME, "${id}: FITS MIME: ${headers.dsMIME}")
                        .log(DEBUG, CT_LOG_NAME, "${id}: FITS. BODY: ${body}")
                        .to("fedora:addDatastream?name=FITS&type=text/xml&dsLabel=FITS Generated Image Metadata&group=X&versionable=false").id("fitsAddDatastream")
                    .endChoice()
                    .otherwise()
                        .log(ERROR, CT_LOG_NAME, "${id}: FITS processing failed for PID: ${header.CamelFedoraPid}")
                    .endChoice()
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished processing FITS.");




        from("direct:addMODSDataStream").routeId("UnifiedCameraTrapAddMODSDataStream")
                .log(INFO, CT_LOG_NAME, "${id}: Started processing MODS ...")

                // Get the FITS XML and use XPATH to get the created date from it. 
                /*.setHeader("FITSCreatedDate").xpath("//fits:fileinfo/fits:created[@toolname='Exiftool']", String.class, ns)
                .log(INFO, CT_LOG_NAME, "${id}: MODS FITS Created Date: ${header.FITSCreatedDate}")*/

                // TODO: The Image ID has .JPG appended and we may want to make this consistent.
                // Add a MODS datastream for concept metadata.
                .setBody().simple("${header.ManifestXML}")

                .to("xslt-saxon:file:config/xslt/ManifestImage.xsl")

                .log(DEBUG, CT_LOG_NAME, "${id}: MODS XML -\n${body}")

                .to("fedora:addDatastream?name=MODS&type=text/xml&group=X&dsLabel=MODS Record")

                .log(INFO, CT_LOG_NAME, "${id}: Finished processing MODS.");



        from("direct:createThumbnail").routeId("UnifiedCameraTrapCreateThumbnailImage")
                /*.onException(IIOException.class)
                    .maximumRedeliveries(0)
                    .continued(true)
                    .stop()
                .end()*/

                .log(INFO, CT_LOG_NAME, "${id}: Started creating thumbnail ...")

                .doTry()
                    .to("thumbnailator:image?keepRatio=true&size=(200,150)")
                    .to("fedora:addDatastream?name=TN&type=image/jpeg&group=M&dsLabel=Thumbnail&versionable=false")
                .doCatch(javax.imageio.IIOException.class)
                    .log(WARN, CT_LOG_NAME, "${id}: Cannot create thumbnail image corrupted.")
                .endDoTry()

                .log(INFO, CT_LOG_NAME, "${id}: Finished creating thumbnail.");




        from("direct:createArchivalImage").routeId("UnifiedCameraTrapCreateArchivalImage")
                .log(INFO, CT_LOG_NAME, "${id}: Started creating archival image ...")

                /*
                NOTE:This is planned to be a JPEG 2000 but we are not doing it yet.
                .to("thumbnailator:image?quality=80%&size=(2048,1536)")
                .to("file://UnifiedCameraTrapOutput/resample")
                */

                .log(INFO, CT_LOG_NAME, "${id}: Finished creating archival image.");



        from("direct:processParents").routeId("UnifiedCameraTrapProcessParents")
                .log(INFO, CT_LOG_NAME, "${id}: Started processing parents ...")

                .setBody().simple("{{si.ct.root}}")

                // Check if the root exists, else quit.
                .to("direct:findObjectByPIDPredicate")

                    .choice()
                        .when(simple("${body} == 'true'"))
                        .log(DEBUG, CT_LOG_NAME, "${id}: Root object exists.")
                        .setHeader("CamelFedoraPid").simple("{{si.ct.root}}")

                        // Add the parents if needed.
                        // Project is required, Project is required
                        .to("direct:processProject")

                        // Add Sub-Project if needed, Sub-Project is required
                        .filter().xpath("boolean(//SubProjectName/text()[1]) or boolean(//SubProjectId/text()[1])", String.class, ns, "ManifestXML")
                            .to("direct:processSubproject")
                        .end()

                        // Add Plot if needed, Plot is optional
                        .filter().xpath("boolean(//PlotName/text()[1])", String.class, ns, "ManifestXML")
                            .to("direct:processPlot")
                        .end()

                        // Clean stashed header that could potentially lock for inflight check during post validations
                        .removeHeader("DeploymentCorrelationId")
                    .endChoice()
                    .otherwise()
                        // Stop processing this deployment.
                        .log(WARN, CT_LOG_NAME, "${id}: Root object does not exist.")
                        .throwException(IllegalArgumentException.class, "Root object does not exist.")
                    .endChoice()
                .end()

                .log(INFO, CT_LOG_NAME, "${id}: Finished processing parents.");



        from("direct:findObject").routeId("UnifiedCameraTrapFindObject").errorHandler(noErrorHandler())
                .log(INFO, CT_LOG_NAME, "${id}: Started find object by ${header.findObjectBy}...")
                .log(INFO, CT_LOG_NAME, "${id}: Find: ${body}  Parent: ${header.CamelFedoraPid}")

                /*There is a bug in Camel that causes replaceAll to fail so I am using Groovy for now.We
                will want to remove this so we don 't have to include the Groovy feature just for this.
                The bug is fixed in newer versions of Camel.
                */

                .transform().groovy("request.body.replace(\"'\", \"\\'\")")

                .log(WARN, CT_LOG_NAME, "${id}: Replace body: ${body}")

                .choice()
                    .when().simple("${header.findObjectBy} == 'AltId'")
                        // Query for Alternate Id
                        .setBody().simple("SELECT ?o FROM <info:edu.si.fedora#ri> WHERE { ?o <http://purl.org/dc/elements/1.1/identifier> \"${body}\" . <info:fedora/${header.CamelFedoraPid}> <info:fedora/fedora-system:def/relations-external#hasConcept> ?o . }").id("whenFindByAltId")
                    .endChoice()
                    .when().simple("${header.findObjectBy} == 'Name'")
                        // The query needs to also check directly along the parent axis since names are not unique.
                        .setBody().simple("SELECT ?o FROM <info:edu.si.fedora#ri> WHERE { ?o <info:fedora/fedora-system:def/model#label> \"${body}\" . <info:fedora/${header.CamelFedoraPid}> <info:fedora/fedora-system:def/relations-external#hasConcept> ?o . }").id("whenFindByName")
                    .endChoice()
                .end()

                // Converting the query above to URL encoded string to send over HTTP with GET method. 
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")

                .log(DEBUG, CT_LOG_NAME, "${id}: Project Parent: ${body}")

                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")
                .setHeader("CamelHttpUri").simple("{{si.fuseki.endpoint}}")

                .toD("http://fusekiEndpointUri?headerFilterStrategy=#fusekiHttpHeaderFilterStrategy").id("findObjectFusekiHttpCall")
                .convertBodyTo(String.class)

                .log(DEBUG, CT_LOG_NAME, "${id}: Query Result: ${body}")

                // Count the results. If greater than one warn about duplicate parents. 
                .filter().xpath("count(//ri:sparql/ri:results/ri:result) > 1", Boolean.class, ns)
                    .log(WARN, CT_LOG_NAME, "${id}: Warning: Duplicate parents.")
                .end()

                .setBody().xpath("substring-after(//ri:sparql/ri:results/ri:result[1]/ri:binding/ri:uri, '/')", String.class, ns)

                .filter(simple("${body} == ''"))

                        .choice()
                            .when(simple("${header.findObjectBy} == 'AltId'"))
                                // Throw custom exception to trigger re-try attempts on search
                                .throwException(FedoraObjectNotFoundException.class, "The fedora object not found for Alternate ID '${header.DeploymentCorrelationId}'")
                            .endChoice()
                            .when(simple("${header.findObjectBy} == 'Name'"))
                                // Throw custom exception to trigger re-try attempts on search
                                .throwException(FedoraObjectNotFoundException.class, "The fedora object not found for string name '${header.CamelFedoraLabel}'")
                            .endChoice()
                        .end()

                .end()
                .log(INFO, CT_LOG_NAME, "${id}: Finished find object.");



        from("direct:findObjectByPIDPredicate").routeId("UnifiedCameraTrapFindObjectByPIDPredicate").errorHandler(noErrorHandler())
                .log(INFO, CT_LOG_NAME, "${id}: Started find object by PID ...")

                // Return true in the body if the object exists for the PID, false if not.
                // ASK FROM <#ri>
                .setBody().simple("ASK FROM <info:edu.si.fedora#ri> {<info:fedora/${body}> ?p ?o. }")

                // Converting the query above to URL encoded string to send over HTTP with GET method.
                .setBody().groovy("\"query=\" + URLEncoder.encode(request.getBody(String.class));")

                .log(INFO, CT_LOG_NAME, "${id}: Find Query: ${body}")

                .setHeader("CamelHttpMethod").constant("GET")
                .setHeader("CamelHttpQuery").simple("output=xml&${body}")
                .setHeader("CamelHttpUri").simple("{{si.fuseki.endpoint}}")

                .toD("http://fusekiEndpointUri?headerFilterStrategy=#fusekiHttpHeaderFilterStrategy").id("findObjectByPIDFusekiHttpCall")
                .convertBodyTo(String.class)

                .log(DEBUG, CT_LOG_NAME, "${id}: Find Query Result: ${body}")

                .setBody().xpath("//ri:boolean/text()", String.class, ns)

                .log(INFO, CT_LOG_NAME, "${id} | ${routeId} | body = ${body}")

                .filter(simple("${body} == false"))
                    // Throw custom exception to trigger re-try attempts on search
                    .throwException(FedoraObjectNotFoundException.class, "The fedora object not found")
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id}: Find Object By PID: ${body}.")
                .log(INFO, CT_LOG_NAME, "${id}: Finished find object by PID.");



        from("direct:processPlot").routeId("UnifiedCameraTrapProcessPlot")
                .log(INFO, CT_LOG_NAME, "${id}: Started processing plot ...")

                // Look for the plot (its optional).
                .setBody().xpath("concat(//SubProjectName/text(), ':', //PlotName/text())", String.class, ns, "ManifestXML")
                .setHeader("CamelFedoraLabel").simple("${body}")

                // stash parent identifier to be used for the in-flight parent process checks. 
                .setHeader("DeploymentCorrelationId").simple("${body}")

                .log(DEBUG, CT_LOG_NAME, "${id}: Plot: Label: ${body}  Parent PID: ${header.CamelFedoraPid}")

                .setHeader("findObjectBy").simple("Name")

                .to("direct:findObject")

                .choice()
                    .when().simple("${body} == ''")
                        .log(DEBUG, CT_LOG_NAME, "${id}: Plot does not exist")

                        // Stash the sub-project PID.
                        .setHeader("ParentPID").simple("${header.CamelFedoraPid}")

                        // Create UCT plot and add it as a sub-concept of the sub-project
                        .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}")

                        // Add a minimal RELS-EXT to the project
                        .to("velocity:file:config/templates/CTPlotTemplate.vsl")

                        .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                        // Add Relation from parent UCT sub-project to the child UCT plot
                        .to("fedora:hasConcept?parentPid=${header.ParentPID}&childPid=${header.CamelFedoraPid}")

                        // Add an FGDC-CTPlot datastream for concept metadata.
                        .setBody().simple("${header.ManifestXML}")

                        .to("xslt-saxon:file:config/xslt/ManifestPlot.xsl")

                        .to("fedora:addDatastream?name=FGDC-CTPlot&type=text/xml&group=X&dsLabel=FGDC-CTPlot Record&versionable=true")

                        // For new plots we need to reload the parent tree node in workbench
                        .setHeader("workbenchNodePid").simple("${header.ParentPID}")
                            .filter().simple("{{enable.workbench.reload.pid.route}} == true")
                            .to("direct:workbenchReloadPid").id("plotWorkbenchReloadPid")
                        .end()

                    .endChoice()
                    .otherwise()
                        // Return the existing plot as the current object.
                        .log(DEBUG, CT_LOG_NAME, "${id}: Plot already exists: ${body}.")
                        .setHeader("CamelFedoraPid").simple("${body}")
                    .endChoice()
                .end()

                .setHeader("PlotPID").simple("${header.CamelFedoraPid}")
                .setHeader("PlotId").simple("${header.DeploymentCorrelationId}")
                .setHeader("PlotName").simple("${header.CamelFedoraLabel}")

                .log(INFO, CT_LOG_NAME, "${id}: Finished processing plot.");



        from("direct:processSubproject").routeId("UnifiedCameraTrapProcessSubproject")
                .log(INFO, CT_LOG_NAME, "${id}: Started processing subproject ...")

                // Look for the sub-project Alternate Id.
                // Label used for fedora object and in-flight checks
                .setHeader("CamelFedoraLabel").xpath("//SubProjectName", String.class, ns, "ManifestXML")

                // SubProject identifier used for AltId and in-flight checks. 
                .setHeader("DeploymentCorrelationId").xpath("//SubProjectId", String.class, ns, "ManifestXML")

                .log(DEBUG, CT_LOG_NAME, "${id}: Sub-project: Alternate Id: ${header.DeploymentCorrelationId} | Label: ${header.CamelFedoraLabel} | Parent PID: ${header.CamelFedoraPid}")

                .setHeader("findObjectBy").simple("AltId")

                .setBody().simple("${header.DeploymentCorrelationId}")

                // First look for the SubProject using Alternate Id
                .to("direct:findObject")

                    .choice()
                    .when().simple("${body} == ''")
                    .log(DEBUG, CT_LOG_NAME, "${id}: Sub-project does not exist for Alternate Id: ${header.DeploymentCorrelationId}")

                    .setHeader("findObjectBy").simple("Name")

                    .setBody().simple("${header.CamelFedoraLabel}")

                    // Look for the SubProject again using string name
                    .to("direct:findObject")

                        .choice()
                            .when().simple("${body} == ''")

                                .log(DEBUG, CT_LOG_NAME, "${id}: Sub-project does not exist for Alternate Id: ${header.DeploymentCorrelationId} or String Name: ${header.CamelFedoraLabel}")

                                // Stash the Project PID.
                                .setHeader("ParentPID").simple("${header.CamelFedoraPid}")

                                // Create UCT sub-project and add it as a sub-concept of the project.
                                .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}")

                                // Add a minimal RELS-EXT to the sub-project
                                .to("velocity:file:config/templates/CTProjectTemplate.vsl")
                                .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                                // Add Relation from parent UCT project to the child UCT sub-project
                                .to("fedora:hasConcept?parentPid=${header.ParentPID}&childPid=${header.CamelFedoraPid}")

                                // Add an EAC-CPF datastream for concept metadata.
                                .setBody().simple("${header.ManifestXML}")
                                .to("xslt-saxon:file:config/xslt/ManifestSubproject.xsl")
                                .to("fedora:addDatastream?name=EAC-CPF&type=text/xml&group=X&dsLabel=EAC-CPF Record&versionable=true")

                                //Update the DC datastream with with dc:identifier for the Alternate ID
                                /*.setBody().simple("${header.ManifestXML}")
                                .to("xslt-saxon:file:config/xslt/Manifest2AltIdDC_identifier.xsl").id("xsltProjectSIdoraConcept2DC")
                                .to("fedora:addDatastream?name=DC&type=text/xml&group=X")*/

                                // Get the DC datastream for the project
                                .to("fedora:getDatastreamDissemination?dsId=DC&exchangePattern=InOut")
                                .convertBodyTo(String.class)

                                //Update the DC datastream with with dc:identifier for the Alternate ID
                                .to("xslt-saxon:file:config/xslt/addAltId2DC.xsl").id("xsltSubprojectSIdoraConcept2DC")
                                .to("fedora:addDatastream?name=DC&type=text/xml&group=X").id("addSubprojectDC")

                                // For new plots we need to reload the parent tree node in workbench
                                .setHeader("workbenchNodePid").simple("${header.ParentPID}")
                                .filter().simple("{{enable.workbench.reload.pid.route}} == true")
                                    .to("direct:workbenchReloadPid").id("subProjectWorkbenchReloadPid")
                                .end()

                            .endChoice()
                            .otherwise()
                                // Return the existing project as the current object.
                                .log(DEBUG, CT_LOG_NAME, "${id}: Sub-project already exists for String Name / Label: ${header.CamelFedoraLabel} with PID: ${body}.")
                                .setHeader("CamelFedoraPid").simple("${body}")
                            .endChoice()
//                        .end()
                    .endChoice()
                    .otherwise()
                        // Return the existing sub-project as the current object.
                        .log(DEBUG, CT_LOG_NAME, "${id}: Sub-project already exists for Alternate Id: ${header.DeploymentCorrelationId} with PID: ${body}.")
                        .setHeader("CamelFedoraPid").simple("${body}")
                    .endChoice()
                .end()

                .setHeader("SubProjectPID").simple("${header.CamelFedoraPid}")
                .setHeader("SubProjectId").simple("${header.DeploymentCorrelationId}")
                .setHeader("SubProjectName").simple("${header.CamelFedoraLabel}")

                .log(INFO, CT_LOG_NAME, "${id}: Finished processing subproject.");



        from("direct:processProject").routeId("UnifiedCameraTrapProcessProject")
                .log(INFO, CT_LOG_NAME, "${id}: Started processing project ...")

                // Look for the project.
                // Label used for fedora object and in-flight checks
                .setHeader("CamelFedoraLabel").xpath("//ProjectName", String.class, ns, "ManifestXML")

                // Project identifier used for AltId and in-flight checks. 
                .setHeader("DeploymentCorrelationId").xpath("//ProjectId", String.class, ns, "ManifestXML")

                .log(DEBUG, CT_LOG_NAME, "${id}: Project: Alternate Id: ${header.DeploymentCorrelationId} | Label: ${header.CamelFedoraLabel} | Parent PID: ${header.CamelFedoraPid}")

                .setHeader("findObjectBy").simple("AltId")
                .setBody().simple("${header.DeploymentCorrelationId}")

                // First look for the Project using Alternate Id
                .to("direct:findObject").id("findObjectProjectAltId")

                .choice()
                    .when().simple("${body} == ''")

                        .log(DEBUG, CT_LOG_NAME, "${id}: Project does not exist for Alternate Id: ${header.DeploymentCorrelationId}.")

                        .setHeader("findObjectBy").simple("Name")

                        .setBody().simple("${header.CamelFedoraLabel}")

                        // Look for the Project again using string name
                        .to("direct:findObject").id("findObjectProjectName")

                        .choice()
                            .when().simple("${body} == ''")

                                .log(INFO, CT_LOG_NAME, "${id}: Project does not exist for Alternate Id: ${header.DeploymentCorrelationId} or String Name: ${header.CamelFedoraLabel}")
                                // Stash the CT Root PID.
                                .setHeader("ParentPID").simple("${header.CamelFedoraPid}")

                                // Create UCT project and add it as a sub-concept of the Camera Trap root object.
                                .to("fedora:create?pid=null&owner={{si.ct.owner}}&namespace={{si.ct.namespace}}")

                                // Add a minimal RELS-EXT to the project.
                                .to("velocity:file:config/templates/CTProjectTemplate.vsl")
                                .to("fedora:addDatastream?name=RELS-EXT&group=X&dsLabel=RDF Statements about this object&versionable=true")

                                // Add Relation from parent UCT root object to the child UCT project.
                                .to("fedora:hasConcept?parentPid={{si.ct.root}}&childPid=${header.CamelFedoraPid}")

                                // Add an EAC-CPF datastream for concept metadata.
                                .setBody().simple("${header.ManifestXML}")
                                .to("xslt-saxon:file:config/xslt/ManifestProject.xsl")

                                .to("fedora:addDatastream?name=EAC-CPF&type=text/xml&group=X&dsLabel=EAC-CPF&versionable=true")

                                //Update the DC datastream with with dc:identifier for the Alternate ID
                                /*.setBody().simple("${header.ManifestXML}")
                                .to(xslt-saxon:file:config/xslt/Manifest2AltIdDC_identifier.xsl" id="xsltProjectSIdoraConcept2DC")
                                .to("fedora:addDatastream?name=DC&type=text/xml&group=X")*/

                                // Get the DC datastream for the project
                                .to("fedora:getDatastreamDissemination?dsId=DC&exchangePattern=InOut")
                                .convertBodyTo(String.class)

                                //Update the DC datastream with with dc:identifier for the Alternate ID
                                .to("xslt-saxon:file:config/xslt/addAltId2DC.xsl").id("xsltProjectSIdoraConcept2DC")
                                .to("fedora:addDatastream?name=DC&type=text/xml&group=X").id("addProjectDC")

                                // For new plots we need to reload the parent tree node in workbench
                                .setHeader("workbenchNodePid").simple("${header.ParentPID}")

                                .filter().simple("{{enable.workbench.reload.pid.route}} == true")
                                    .to("direct:workbenchReloadPid").id("projectWorkbenchReloadPid")
                                .end()

                            .endChoice()
                            .otherwise()
                                // Return the existing project as the current object.
                                .log(DEBUG, CT_LOG_NAME, "${id}: Project already exists for String Name / Label: ${header.CamelFedoraLabel} with PID: ${body}.")
                                .setHeader("CamelFedoraPid").simple("${body}")
                            .endChoice()
//                        .end()

                    .endChoice()
                    .otherwise()
                        // Return the existing project as the current object.
                        .log(DEBUG, CT_LOG_NAME, "${id}: Project already exists for Alternate Id: ${header.DeploymentCorrelationId} with PID: ${body}.")
                        .setHeader("CamelFedoraPid").simple("${body}")
                    .endChoice()
                .end()

                .setHeader("ProjectPID").simple("${header.CamelFedoraPid}")
                .setHeader("ProjectId").simple("${header.DeploymentCorrelationId}")
                .setHeader("ProjectName").simple("${header.CamelFedoraLabel}")

                .log(INFO, CT_LOG_NAME, "${id}: Finished processing project.");



        // Validate Ingest Datastream Metadata Fields
        from("direct:validateDatastreamFields").routeId("UnifiedCameraTrapValidateDatastreamFields")
                .log(INFO, CT_LOG_NAME, "${id}: ValidateDatastreamFields: Starting Datastream Fields Validation ...")

                .to("direct:validate_EAC-CPF_Datastream")

                .to("direct:validate_FGDC_Datastream")

                .choice()
                    .when().simple("${header.ImageResourcePID}")
                        .to("direct:validate_MODS_Datastream")
                    .endChoice()
                    .otherwise()
                        .log(DEBUG, CT_LOG_NAME, "${id}: No Image Resource PID, skipping MODS Validation")
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id}: Validated MODS DATASTREAM")

                // Start Researcher CSV Validation
                .setHeader("CamelFedoraPid").simple("${header.ResearcherObservationPID}")
                .to("direct:ValidateCSVFields")

                /*
                If Volunteer or Image Observations exist Researcher Validation Errors will be sent to the aggregator
                before continuing to the next validation.
                If Volunteer or Image Observations DO NOT exist the validationComplete header is set and the
                Researcher Observation Errors are sent to the aggregator for final validation error message aggregation
                */

                // Start Volunteer CSV Validation
                .choice()
                    .when().header("VolunteerObservationPID")
                        // Send the Validation Error Message to the aggregator if any
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .setHeader("CamelFedoraPid").simple("${header.VolunteerObservationPID}")
                        .to("direct:ValidateCSVFields")
                    .endChoice()
                .end()

                // Start Image CSV Validation 
                .choice()
                    .when().header("ImageObservationPID")
                        // Send the Validation Error Message to the aggregator if any
                        .to("direct:validationErrorMessageAggregationStrategy")
                        .setHeader("CamelFedoraPid").simple("${header.ImageObservationPID}")
                        .to("direct:ValidateCSVFields")
                    .endChoice()
                .end()

                // Notify the aggregator to complete aggregation 
                .setHeader("validationComplete").simple("true")

                .to("direct:validationErrorMessageAggregationStrategy")

                .log(INFO, CT_LOG_NAME, "${id}: ValidateDatastreamFields: Datastream Fields Validation Complete...");



        // EAC-CPF Datastream Validation Route
        from("direct:validate_EAC-CPF_Datastream").routeId("UnifiedCameraTrapValidate_EAC-CPF_Datastream")

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateDatastreamFields: Starting EAC-CPF Datastream Validation ...")

                // Start EAC-CPF Validation
                .setHeader("CamelFedoraPid").simple("${header.ProjectPID}")

                // Get the EAC-CPF datastream from the project parent
                .to("fedora://getDatastreamDissemination?dsId=EAC-CPF&exchangePattern=InOut")
                .convertBodyTo(String.class)

                // Store the current datastream to be used in the validation bean
                .setHeader("datastreamValidationXML").simple("${body}")

                // the comma separated list of xpath's and field names to be validated 
                /*Validation xpaths for newer manifest (see jira ticket SID-618 )
                .setBody().simple("
                    EAC-CPF Latitude, //eac:eac-cpf/eac:cpfDescription/eac:description/eac:place/eac:placeEntry/@latitude, //CameraTrapDeployment/ActualLatitude/text()
                    EAC-CPF Longitude, //eac:eac-cpf/eac:cpfDescription/eac:description/eac:place/eac:placeEntry/@longitude, //CameraTrapDeployment/ActualLongitude/text()
                    EAC-CPF PublishDate, //eac:eac-cpf/eac:control/eac:localControl/eac:date, //*[@PublishDate]
                    EAC-CPF ProjectDataAccessandUseConstraints, //eac:eac-cpf/eac:cpfDescription/eac:description/eac:functions/eac:function/eac:descriptiveNote/eac:p, //*[@ProjectDataAccessandUseConstraints]
                ")
                */

                // the piped separated list of xpath's and field names to be validated
                .setBody().simple("EAC-CPF ProjectName|//eac:nameEntry[1]/eac:part/text()|//ProjectName/text()")

                .to("direct:ValidateDatastreamFieldList")

                .to("direct:validationErrorMessageAggregationStrategy");



        // FGDC Datastream Validation Route
        from("direct:validate_FGDC_Datastream").routeId("UnifiedCameraTrapValidate_FGDC_Datastream")

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateDatastreamFields: Starting FGDC Datastream Validation ...")

                // Start FGDC Validation
                .setHeader("CamelFedoraPid").simple("${header.SitePID}")

                // Get the FGDC datastream from the project parent
                .to("fedora://getDatastreamDissemination?dsId=FGDC&exchangePattern=InOut").id("getFGDCDatastream")
                .convertBodyTo(String.class)

                // Store the current datastream to be used in the validation bean
                .setHeader("datastreamValidationXML").simple("${body}")

                // the piped separated list of xpath's and field names to be validated
                .setBody().simple("FGDC CameraDeploymentID|//citeinfo/othercit/text()|//CameraDeploymentID/text()")
                //.setBody().simple("FGDC CameraDeploymentID, //metadata/idinfo/citation/citeinfo/othercit/text(), //CameraTrapDeployment/CameraDeploymentID/text()")
                //.setBody().simple("FGDC Bait, //metadata/dataqual/lineage/method[1]/methodid/methkey/text(), //CameraTrapDeployment/Bait/text()")
                //.setBody().simple("FGDC Feature, //metadata/dataqual/lineage/method[2]/methodid/methkey/text(), //CameraTrapDeployment/Feature/text()")

                .to("direct:ValidateDatastreamFieldList")
                .to("direct:validationErrorMessageAggregationStrategy").id("aggregateFGDC");



        // MODS Datastream Validation Route
        from("direct:validate_MODS_Datastream").routeId("UnifiedCameraTrapValidate_MODS_Datastream")

                .log(INFO, CT_LOG_NAME, "${id}: ValidateDatastreamFields: Starting MODS Datastream Validation ...")

                // Start MODS Validation
                // get the PID that we stored using the filter in the addMODSDatastream route
                .setHeader("CamelFedoraPid").simple("${header.ImageResourcePID}")

                .log(DEBUG, CT_LOG_NAME, "${id}: Set header for CamelFedoraPid ${header.ImageResourcePID}")

                // Get the MODS datastream
                .to("fedora://getDatastreamDissemination?dsId=MODS&exchangePattern=InOut").id("getMODSDatastream")

                .log(DEBUG, CT_LOG_NAME, "${id}: MODS datastream retrieved")

                .convertBodyTo(String.class)

                .log(DEBUG, CT_LOG_NAME, "${id}: MODS datastream converted to a java string")

                // Store the current datastream  to be used in the validation bean 
                .setHeader("datastreamValidationXML").simple("${body}")

                .log(DEBUG, CT_LOG_NAME, "${id}: Storing the current datastream to be used in the validation bean")

                // the piped separated list of xpath's and field names to be validated
                .setBody().simple("MODS ImageSequenceId|//mods:relatedItem/mods:identifier/text()|//ImageSequenceId[text()='${header.modsImageSequenceId}']/text()")
                //.setBody().simple("MODS ImageSequenceId|//mods:relatedItem/mods:identifier/text()|//ImageSequence[1]/ImageSequenceId[1]/text()")
                //.setBody().simple("MODS ImageSequenceId, //mods:mods/mods:relatedItem[1]/mods:identifier[1], //CameraTrapDeployment/ImageSequence[1]/ImageSequenceId[1]/text()")

                .log(INFO, CT_LOG_NAME, "${id}: Body set")

                .to("direct:ValidateDatastreamFieldList")
                .to("direct:validationErrorMessageAggregationStrategy").id("aggregateMODS");



        // Validate the provided datastream and field list
        from("direct:ValidateDatastreamFieldList").routeId("UnifiedCameraTrapValidateDatastreamFieldList")
                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateDatastreamFieldList: Starting Validation of Datastream Metadata Fields List...")

                .split()
                    .tokenize("\r\n|\n", true) //TODO: <tokenize token = "\r\n|\n" xml = "false" trim = "true" / >
                    .aggregationStrategy(new CameraTrapValidationMessageAggregationStrategy())
                    .streaming()

                        // Use a bean for validation because xpathbuilder is need
                        //.to("bean:postIngestionValidator?method=validateField")
                        .bean(PostIngestionValidator.class, "validateField")
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateDatastreamFieldList: Validation of Datastream Fields List Complete...");



        // Validate CSV Fields
        from("direct:ValidateCSVFields").routeId("UnifiedCameraTrapValidateCSVFields")

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateCSVFields: Starting Observation CSV Validation...")

                .to("fedora://getDatastreamDissemination?dsId=CSV&exchangePattern=InOut")
                .convertBodyTo(String.class)

                .unmarshal().csv()

                // Get Researcher, Volunteer, or Image from the CSV
                .setHeader("Observer").simple("${body[0][0]}Identifications", String.class)

                // Count the number of observation records in the csv 
                .setHeader("CSVObservationCount").simple("${body.size()}", Integer.class)

                // Validate each CSV observation matches the Manifest 
                // Store the first ImageSequenceId to validate the CSV was created 
                .setHeader("CSVImageSeqID").simple("${body[0][2]}", String.class)

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateCSVFields: ${header.Observer} | CSVObservationCount = ${header.CSVObservationCount} | CSVImageSeqID = ${header.CSVImageSeqID}")

                .setHeader("validationResult").xpath("boolean((//ImageSequenceId/text() = $in:CSVImageSeqID) and (count(//*[name() = $in:Observer]/Identification) = $in:CSVObservationCount))", Boolean.class, ns, "ManifestXML")

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateCSVFields: ${header.Observer} validationResult: ${header.validationResult} ")

                .choice()
                    // Check that the ImageSequenceID exists and observation counts matches the manifest
                    .when().simple("${header.validationResult} == false")

                        // The CSV was not generated create validation error message
                        .setBody().simple("${header.Observer} CSV: Validation Failed!")

                        .log(WARN, CT_LOG_NAME, "${id}: ValidateCSVFields: ${body}")

                        //.to("bean:cameraTrapValidationMessage?method=createValidationMessage(${header.deploymentPackageId}, ${body}, false)")
                        .bean(CameraTrapValidationMessage.class, "createValidationMessage(${header.deploymentPackageId}, ${body}, false)")
                    .endChoice()
                    .otherwise()
                        .setBody(simple(""))
                    .endChoice()
                .end()

                .log(DEBUG, CT_LOG_NAME, "${id}: ValidateCSVFields: ${header.Observer} CSV Validation Complete...");



        // Validation Error Message Aggregation Strategy
        from("direct:validationErrorMessageAggregationStrategy").routeId("UnifiedCameraTrapValidationErrorMessageAggregationStrategy")
                .aggregate(simple("${header.validationErrors}"), new CameraTrapValidationMessageAggregationStrategy())
                    .eagerCheckCompletion()
                    .completionPredicate(simple("${header.validationComplete} == 'true'"))

                    .choice()
                        .when().simple("${body.size} == 0")
                            .log(INFO, CT_LOG_NAME, "${id}: Successful Ingest: DeploymentPackage=${header.deploymentPackageId}, DeploymentId=${header.SiteId}, DeploymentName=${header.SiteName}, ProjectID=${header.ProjectId}, ProjectName=${header.ProjectName}, SubprojectID=${header.SubprojectId}, SubprojectName=${header.SubprojectName}, ResourceCount=${header.ResourceCount}, AdjustedResourceCount=${header.AdjustedResourceCount}")
                        .endChoice()
                        .otherwise()
                            .log(ERROR, CT_LOG_NAME, "${id}: Ingest Validation Errors for: DeploymentPackage=${header.deploymentPackageId}, DeploymentId=${header.SiteId}, DeploymentName=${header.SiteName}, ProjectID=${header.ProjectId}, ProjectName=${header.ProjectName}, SubprojectID=${header.SubprojectId}, SubprojectName=${header.SubprojectName}, ResourceCount=${header.ResourceCount}, AdjustedResourceCount=${header.AdjustedResourceCount}\nValidation Errors:\n${body}")
                        .endChoice()
                    .end();



        from("timer://checkConceptFromRI?fixedRate=true&period={{si.ct.checkConceptFromRI.period}}").routeId("UnifiedCameraTrapInFlightConceptStatusPolling")
                .autoStartup(getContext().resolvePropertyPlaceholders("{{autostartup.checkConceptFromRI.route}}"))

                .log(INFO, CT_LOG_NAME, "${id}: InFlightConceptStatusPolling: Starting In-Flight Concept Process status check...")

                // retrieve the collection of correlation identifiers from the storage
                .to("bean:cameraTrapStaticStore?method=getInFlightCorrelationIds()")
//                .bean(CameraTrapStaticStore.class, "getInFlightCorrelationIds")

                .split().simple("${body.keySet}")

                    // retrieve the correlation information from the storage; such as the deploymentPackageId, correlation label etc
                    .to("bean:cameraTrapStaticStore?method=getCorrelationInformationById(${body})")
//                    .bean(CameraTrapStaticStore.class, "getCorrelationInformationById(${body})")

                    // stash correlation identifier so we can remove it from the storage if the concept is found in the RI
                    .setHeader("TempCorrelationId").simple("${body.correlationId}")
                    .setHeader("DeploymentCorrelationId").simple("${body.correlationId}")
                    .setHeader("CamelFedoraLabel").simple("${body.correlationLabel}")

                    // set required headers and body for the find object operation to function
                    .setHeader("CamelFedoraPid").simple("${body.parentObjectPid}")
                    .setHeader("deploymentPackageId").simple("${body.deploymentPackageId}")

                    .log(INFO, CT_LOG_NAME, "${id}: InFlightConceptStatusPolling: Alternate Id: ${header.DeploymentCorrelationId} Label: ${header.CamelFedoraLabel}  Parent PID: ${header.CamelFedoraPid}")

                    .setHeader("findObjectBy").simple("AltId")

                    .setBody().simple("${header.DeploymentCorrelationId}")

                    // First look for the Project using Alternate Id
                    .to("direct:findObject")

                    .choice()
                        .when().simple("${body} != ''")
                            .log(WARN, CT_LOG_NAME, "${id}: InFlightConceptStatusPolling: Concept object: ${header.TempCorrelationId} found from RI.. removing from the data structure...")
                            .to("bean:cameraTrapStaticStore?method=removeCorrelationId(${header.TempCorrelationId})")
//                            .bean(CameraTrapStaticStore.class, "removeCorrelationId(${header.TempCorrelationId})")
                        .endChoice()
                        .when().simple("${body} == ''")
                            .log(WARN, CT_LOG_NAME, "${id}: InFlightConceptStatusPolling: Project does not exist for Alternate Id: ${header.TempCorrelationId}.")

                            .setHeader("findObjectBy").simple("Name")

                            .setBody().simple("${header.CamelFedoraLabel}")

                            // if concept is found in RI, we can safely remove from the storage to release the wait lock
                            .to("direct:findObject")

                            .filter().simple("${body} != ''")
                                .log(WARN, CT_LOG_NAME, "${id}: InFlightConceptStatusPolling: Concept object: ${header.TempCorrelationId} found from RI.. removing from the data structure...")
                                .to("bean:cameraTrapStaticStore?method=removeCorrelationId(${header.TempCorrelationId})")
//                                .bean(CameraTrapStaticStore.class, "removeCorrelationId(${header.TempCorrelationId})")
                            .end()

                        .endChoice()
                    .end()

                .end()

                .log(INFO, CT_LOG_NAME, "${id}: InFlightConceptStatusPolling: Finished In-Flight Concept Process status check.");



        from("direct:workbenchReloadPid").routeId("UnifiedCameraTrapReloadWorkbenchConceptTreePid")
                .onException(ConnectException.class, HttpOperationFailedException.class)
                    .useExponentialBackOff()
                    .backOffMultiplier(2)
                    .redeliveryDelay("{{si.ct.connEx.redeliveryDelay}}")
                    .maximumRedeliveries("{{min.connectEx.redeliveries}}")
                    .retryAttemptedLogLevel(WARN)
                    .continued(true)
                    .log(WARN, CT_LOG_NAME, "[${routeId}] :: Could Not Refresh Workbench Tree Node for ${header.workbenchNodePid}! Error reported: ${exception.message}").id("workbenchConnectException")
                .end()

                .log(INFO, CT_LOG_NAME, "${id} ${routeId}: Starting Workbench Clear Cache for Research Project Tree Concept PID Request...")
                .log(INFO, CT_LOG_NAME, "${id}: Workbench Reload Pid = ${header.workbenchNodePid}")
                .log(DEBUG, CT_LOG_NAME, "${id}: Workbench Reload Body = ${body}")

                .setHeader("CamelHttpMethod").simple("GET")

                // remove problem headers as the headerFilterStrategy only prevents from adding to the http headers and the camel http component will still use camel headers listed in the headerFilterStrategy to build the request url such as CamelHttpQuery
                .removeHeaders("CamelHttpQuery|CamelHttpResponseCode|CamelHttpResponseText")

                .setBody(simple(""))

                .setHeader("CamelHttpUri").simple("{{camel.workbench.clear.cache.url}}/${header.workbenchNodePid}")

                .toD("http://workbenchClearCache?headerFilterStrategy=#wbHttpHeaderFilterStrategy").id("workbenchReloadPid")
                .convertBodyTo(String.class)

                .choice()
                    .when().simple("${body} contains 'Clearing cache for:${header.workbenchNodePid}' && ${header.CamelHttpResponseCode} == 200")
                        .log(INFO, CT_LOG_NAME, "${id} ${routeId}: Workbench Clear Cache Success for PID: ${header.workbenchNodePid} | Response Code: ${header.CamelHttpResponseCode}, Response: ${header.CamelHttpResponseText}, Body: ${body}")
                    .endChoice()
                    .otherwise()
                        .throwException(ConnectException.class, "Workbench Clear Cache Failed for PID: ${header.workbenchNodePid} | Response Code: ${header.CamelHttpResponseCode}, Response Text: ${header.CamelHttpResponseText}, Response Body: ${body}")
                    .endChoice()
                .end()

                .log(INFO, CT_LOG_NAME, "${id} ${routeId}: Finished Workbench Clear Cache for PID...");

    }
}
