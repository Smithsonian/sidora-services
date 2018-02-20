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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Class that creates EDAN server endpoints for testing purposes
 *
 * @author jbirkhimer
 */
@Path("/")
public class EdanTestService {

    private static final Logger LOG = LoggerFactory.getLogger(EdanTestService.class);

    /**
     * Endpoint that accepts any path
     * @return
     */
    @GET
    @Path(value = "/{s:.*}")
    public Response edanTest() {
        LOG.info("Edan Test Server Received A Request");

        Response.ResponseBuilder response = Response.ok("EDAN RESPONSE");

        return response.build();
    }

    /**
     * Endpoint that accepts any path
     * @return
     */
    @POST
    @Path(value = "/{s:.*}")
    public Response workbenckCurlTest() {
        LOG.info("Workbench Test Server Received A Request");

        Response.ResponseBuilder response = Response.ok("WORKBENCH TEST RESPONSE");

        return response.build();
    }

    /**
     *
     * @param edanJson
     * @return
     */
    @GET
    @Path(value = "/addEdanTest")
    public Response addEdanTest(@QueryParam("content") final String edanJson) {
        LOG.info("Edan Test Server Received:\n" + edanJson);

        Response.ResponseBuilder response = Response.ok(edanJson);

        return response.build();
    }

}
