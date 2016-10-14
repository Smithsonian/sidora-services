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

package edu.si.services.sidora.rest.batch;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.message.Attachment;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URL;

/**
 * @author jbirkhimer
 */

@Path("/")
public class BatchResources {

    @POST
    @Path(value = "/batch/process/addResourceObjects/{parentId}")
    @Produces("text/xml")
    public Response batchProcessAddResourceObjectsRequest(@PathParam("parentId") String parentId,
                                                          @QueryParam("title") String title,
                                                          @Multipart(value = "resourceZipFileURL", type = "text/plain") String resourceZipFileURL,
                                                          @Multipart(value = "metadataFileURL", type = "text/plain") String metadataFileURL,
                                                          @Multipart(value = "contentModel") String contentModel,
                                                          @Multipart(value = "resourceOwner") String resourceOwner) {
        return null;
    }

    @GET
    @Path(value = "/batch/process/addResourceObjects/{parentId}/{correlationId}")
    @Produces("text/xml")
    public Response batchProcessAddResourceObjectsRequestStatus(@PathParam("parentId") String parentId, @PathParam("correlationId") String correlationID) {
        return null;
    }

    /*@POST
    @Path("/{id}")
    public Response multipartPostWithParametersAndPayload(
            @QueryParam("query") String abc, @PathParam("id") String id,
            @Multipart(value = "part1", type = "image/jpeg") DataHandler dh1,
            @Multipart(value = "part2", type = "image/jpeg") DataHandler dh2,
            @Multipart(value = "body", type = "text/xml") Object request) {
        return null;
    }*/


}
