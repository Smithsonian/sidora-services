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

package edu.si.services.camel.fcrepo;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

/**
 * Unit testing for the FcrepoComponent class.
 *
 * @author parkjohn
 */
@RunWith(MockitoJUnitRunner.class)
public class FcrepoComponentTest {

    private static final String TEST_ENDPOINT_URI = "fcrepo:foo/endpoint";

    private static final Map<String, Object> EMPTY_MAP = emptyMap();

    @Mock
    private CamelContext mockContext;

    /**
     * Test createEndpoint method from the component
     *
     * @throws Exception
     */
    @Test
    public void testCreateEndpoint() throws Exception {
        final FcrepoComponent testComponent = new FcrepoComponent(mockContext);
        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);
        assertEquals(mockContext, testEndpoint.getCamelContext());
        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());
    }

}