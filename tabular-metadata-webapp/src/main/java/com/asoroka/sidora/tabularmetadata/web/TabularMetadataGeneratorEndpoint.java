
package com.asoroka.sidora.tabularmetadata.web;

import static edu.si.codebook.Codebook.codebook;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;

import edu.si.codebook.Codebook;

/**
 * A simple HTTP endpoint for tabular metadata generation.
 * 
 * @author ajs6f
 */
@Path("/")
public class TabularMetadataGeneratorEndpoint {

    @Inject
    private TabularMetadataGenerator generator;

    @GET
    @Path("/")
    @Produces("text/xml")
    public Codebook get(@QueryParam("url") final URL url) throws IOException {
        return codebook(generator.getMetadata(url));
    }
}
