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

import edu.si.services.sidora.cameratrap.validation.CameraTrapValidationMessage;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * The CameraTrap Route Builder contains some of the CameraTrap Camel routes such as the post validation logic in Java DSL
 * to execute during the CameraTrap Deployment Package ingestion process.  The current CameraTrap routes are in
 * both Camel Java DSL and XML DSL, with most of the ingestion logic in the XML DSL.
 *
 * @author parkjohn
 */
@Component
public class CameraTrapRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "si.ct.id")
    static private String CT_LOG_NAME;


    /**
     * Configure the Camel routing rules for the Camera Trap Deployment Package Ingestion Process.
     */
    @Override
    public void configure() {

        //CameraTrapValidatePostResourceCount route for RELS-EXT resource reference count validation
        from("direct:validatePostResourceCount")
            .routeId("CameraTrapValidatePostResourceCount")
            .choice()
                //compare adjusted resource count (count sans empty images) with number of resources in the RELS_EXT
                //TODO: Revisit this should we use the ResourceCount or AdjustedResourceCount ???
                .when(header("ResourceCount").isEqualTo(header("RelsExtResourceCount")))
                    .log(LoggingLevel.INFO, CT_LOG_NAME, "${id}: Post Resource Count validation passed")
                    .id("ValidatePostResourceCountWhenBlock")
                .otherwise()
                    .log(LoggingLevel.WARN, CT_LOG_NAME, "${id}: Post Resource Count validation failed")
                    //.to("bean:cameraTrapValidationMessage?method=createValidationMessage(${header.deploymentPackageId}, 'Post Resource Count validation failed. Expected ${header.ResourceCount} but found ${header.RelsExtResourceCount}', false)").id("postResourceCountValidationFailed")
                    .bean(CameraTrapValidationMessage.class, "createValidationMessage(${header.deploymentPackageId}, 'Post Resource Count validation failed. Expected ${header.ResourceCount} but found ${header.RelsExtResourceCount}', false)").id("postResourceCountValidationFailed")
                    .to("direct:validationErrorMessageAggregationStrategy")
            .endChoice();

    }
}
