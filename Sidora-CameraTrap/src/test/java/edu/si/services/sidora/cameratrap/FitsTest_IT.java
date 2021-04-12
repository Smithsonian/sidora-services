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

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author jbirkhimer
 */
@Disabled
@CamelSpringBootTest
@SpringBootTest(
        properties = {
        "logging.file.path=target/logs",
        "processing.dir.base.path=${user.dir}/target",
        "si.ct.uscbi.enableS3Routes=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class FitsTest_IT {

    private static final Logger log = LoggerFactory.getLogger(FitsTest_IT.class);

    @Autowired
    CamelContext context;

    //@EndpointInject(value = "direct:addFITSDataStream")
    @Autowired
    ProducerTemplate template;

    private static String TEST_FILENAME = new File("src/test/resources/BBB_9425.NEF").getAbsolutePath();

    /**
     * Testing AddFITSDataStream Route
     *
     * @throws Exception
     */
    @Test
    public void testFitsAddDatastreamRoute() throws Exception {

        // The mock endpoint we are sending to for assertions.
        MockEndpoint mockResult = context.getEndpoint("mock:result", MockEndpoint.class);
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("dsMIME", "image/x-nikon-nef");

        // AdviceWith the routes as needed for this test.
        AdviceWith.adviceWith(context, "UnifiedCameraTrapAddFITSDataStream", false, a ->
                a.weaveById("fitsAddDatastream").replace().log(LoggingLevel.INFO, "Skipping add datastream to Fedora").to("mock:result")
        );

        // The endpoint we want to start from with the exchange body and headers we want
        template.requestBodyAndHeader("direct:addFITSDataStream", "test", "CamelFileAbsolutePath", TEST_FILENAME);

        mockResult.assertIsSatisfied();

        // Check the result
        Message result = mockResult.getExchanges().get(0).getIn();
        assertEquals("image/x-nikon-nef", result.getHeader("dsMIME"));

    }
}
