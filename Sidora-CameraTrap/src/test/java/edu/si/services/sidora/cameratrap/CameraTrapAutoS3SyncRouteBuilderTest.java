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

package edu.si.services.sidora.cameratrap;

import edu.si.services.sidora.cameratrap.routes.CameraTrapAutoS3SyncRouteBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


@CamelSpringBootTest
@SpringBootTest(classes = {CamelAutoConfiguration.class, CameraTrapAutoS3SyncRouteBuilderTest.class},
        properties = {"logging.file.path=target/logs", "si.ct.uscbi.enableS3Routes=false"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@UseAdviceWith
public class CameraTrapAutoS3SyncRouteBuilderTest extends CameraTrapAutoS3SyncRouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(AwsS3PollAndUploadRouteTest.class);

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    RoutesBuilder routesBuilder = new RouteBuilder() {
        @Override
        public void configure() throws Exception {
            if (enableS3Routes) {
                from("timer:enableS3RoutesTest?fixedRate=true&repeatCount=5&period=5000").routeId("enableS3RoutesTest")
                        .log("testing...")
                        .to("mock:result");
            } else if (enableS3Routes == null){
                throw new IllegalArgumentException("si.ct.uscbi.enableS3Routes property must be set");
            }
        }
    };

    @Test
    public void testEnableS3RoutesGetterMatching() {
        enableS3Routes = true;
        Assertions.assertEquals(true, isEnableS3Routes());
    }

    @Test
    public void testEnableS3RoutesGetterNotMatching() {
        enableS3Routes = true;
        assertNotEquals(false, isEnableS3Routes());
    }

    @Test
    public void testEnableS3RoutesException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            enableS3Routes = null;
            context.addRoutes(routesBuilder);
        });

        assertTrue(exception instanceof NullPointerException);

    }

}