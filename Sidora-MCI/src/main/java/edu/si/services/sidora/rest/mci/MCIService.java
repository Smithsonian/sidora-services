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

package edu.si.services.sidora.rest.mci;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import javax.activation.DataHandler;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * @author jbirkhimer
 */
@Path("/")
public class MCIService {

    /**
     *
     * @param mciProject
     * @param ownerId
     * @param option
     * @return
     */
    @POST
    @Path(value = "/mci/{ownerId}/addProject")
    @Produces({"application/xml","application/json"})
    //@Consumes("application/xml")
    @Consumes("*/*")
    public Response addProject(String mciProject,
            @PathParam("ownerId") String ownerId,
            @QueryParam("option") String option) {
        return null;
    }

    /**
     *
     * @param ownerId
     * @param option
     * @param mciProjectString
     * @return
     */
    @POST
    @Path(value = "/mci/{ownerId}/addProject")
    @Produces({"application/xml","application/json"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addProjectMultipart(
            @PathParam("ownerId") String ownerId,
            @QueryParam("option") String option,
            @Multipart(value = "mciProject", type = "application/xml") String mciProject/*,
            @Multipart(value = "mciProjectDataHandler", type = "application/xml") DataHandler mciProjectDataHandler,
            @Multipart(value = "mciProjectAttachment", type = "application/xml") Attachment mciProjectAttachment*/) {
        return null;
    }
}
