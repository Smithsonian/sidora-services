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

import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel routes in Java DSL for SCBI camera trap pipelines to automatically pulling down deployment
 * packages from the AWS S3 bucket and put the ingested/error deployments back on the S3.  These routes have been
 * separated out from the main routes in the XML or the CameraTrapRouteBuilder so that it can be included only where it's needed (i.e. SCBI routes)
 *
 * @author parkjohn
 */
public class CameraTrapAutoS3SyncRouteBuilder extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(CameraTrapAutoS3SyncRouteBuilder.class);

    @PropertyInject(value = "si.ct.id")
    static private String CT_LOG_NAME;

    @PropertyInject(value = "si.ct.non.ssd.dir.path")
    private String nonSsdDirPath;

    @PropertyInject(value = "si.ct.uscbi.process.dir.path")
    private String processDirPath;

    @PropertyInject(value = "si.ct.uscbi.stage.dir.path")
    private String stageDirPath;

    @PropertyInject(value = "si.ct.uscbi.s3.approved.bucketName")
    private String s3ApprovedBucketName;

    @PropertyInject(value = "si.ct.uscbi.s3.ingested.bucketName")
    private String s3IngestedBucketName;

    @PropertyInject(value = "si.ct.uscbi.s3.rejected.bucketName")
    private String s3RejectedBucketName;

    private final Boolean enableS3Routes;

    public CameraTrapAutoS3SyncRouteBuilder(Boolean enableS3Routes) {

        if (enableS3Routes == null){
            throw new IllegalArgumentException("si.ct.uscbi.enableS3Routes property must be set");
        }
        this.enableS3Routes = enableS3Routes;
    }

    /**
     * Configure the Camel routing rules for the Camera Trap Deployment automatic sync with AWS S3 buckets for SCBI Deployments workflow.
     */
    @Override
    public void configure() {

        //register aws-s3 routes only if it's enabled
        if (enableS3Routes){
            //Route for automatic pulldown for SCBI deployment packages from AWS S3 approved bucket
            from("aws-s3://" + s3ApprovedBucketName + "?amazonS3Client=#amazonS3Client" +
                    "&delay={{si.ct.file.pollDelay}}" +
                    "&maxMessagesPerPoll={{si.ct.file.maxMessagesPerPoll}}" +
                    "&deleteAfterRead=true")
                    .routeId("CameraTrapDeploymentsPulldownFromS3")
                    //auto pulldown route is disabled by default.  enable from hawtio or karaf console as necessary
                    .noAutoStartup()
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapDeploymentsPulldownFromS3: Starting AWS S3 Automatic Pulldown for Processing...")
                    .to("file:"+stageDirPath+"?fileName=${header.CamelAwsS3Key}" +
                            "&delay={{si.ct.file.pollDelay}}" +
                            "&doneFileName=${file:name}.done")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapDeploymentsPulldownFromS3: Finished AWS S3 Automatic Pulldown on file: ${header.CamelAwsS3Key}");


            //Route for moving "ready" deployments with the .done marker file from the staging directory to the processing directory for ingestion.
            from("file:"+stageDirPath+"?delay={{si.ct.file.pollDelay}}" +
                    "&doneFileName=${file:name}.done" +
                    "&maxMessagesPerPoll={{si.ct.file.maxMessagesPerPoll}}" +
                    "&filter=#deploymentPackageProcessFilter" +
                    "&delete=true")
                    .routeId("CameraTrapDeploymentsPrepareStageToProcess")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapDeploymentsPrepareStageToProcess: Starting Move Operation from Stage to Process Directory " +
                            "on Ready Deployments...")
                    .to("file:"+processDirPath+"?moveFailed=Error" +
                            "&delay={{si.ct.file.pollDelay}}" +
                            "&maxMessagesPerPoll={{si.ct.file.maxMessagesPerPoll}}")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapDeploymentsPrepareStageToProcess: Finished Move Operation " +
                            "on file: ${header.CamelFileName} from Stage to Process directory.");


            //Route for copying ingested deployments to AWS S3 ingested bucket
            from("file:"+nonSsdDirPath+"/uscbi/Done?delay={{si.ct.file.pollDelay}}" +
                    "&maxMessagesPerPoll={{si.ct.file.maxMessagesPerPoll}}" +
                    "&move={{si.ct.external.upload.success.dir}}" +
                    "&moveFailed={{si.ct.external.upload.error.dir}}")
                    .routeId("CameraTrapCopyIngestedDeploymentsToS3")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapCopyIngestedDeploymentsToS3: Starting AWS S3 upload for Ingested Deployments...")
                    .setHeader("CamelAwsS3Key", simple("${header.CamelFileName}"))
                    .setHeader("CamelAwsS3ContentLength", simple("${header.CamelFileLength}"))
                    .to("aws-s3://" + s3IngestedBucketName + "?amazonS3Client=#amazonS3Client")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapCopyIngestedDeploymentsToS3: Finished AWS S3 upload for file: ${header.CamelFileName}");


            //Route for copying problematic deployments to AWS S3 rejected bucket
            from("file:"+nonSsdDirPath+"/uscbi/Error_UnifiedCameraTrap?delay={{si.ct.file.pollDelay}}" +
                    "&maxMessagesPerPoll={{si.ct.file.maxMessagesPerPoll}}" +
                    "&move={{si.ct.external.upload.success.dir}}" +
                    "&moveFailed={{si.ct.external.upload.error.dir}}")
                    .routeId("CameraTrapCopyErrorDeploymentsToS3")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapCopyErrorDeploymentsToS3: Starting AWS S3 upload for Error Deployments...")
                    .setHeader("CamelAwsS3Key", simple("${header.CamelFileName}"))
                    .setHeader("CamelAwsS3ContentLength", simple("${header.CamelFileLength}"))
                    .to("aws-s3://" + s3RejectedBucketName + "?amazonS3Client=#amazonS3Client")
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: CameraTrapCopyErrorDeploymentsToS3: Finished AWS S3 upload for file: ${header.CamelFileName}");

        }


    }

    public Boolean getEnableS3Routes() {
        return enableS3Routes;
    }
}
