
package com.asoroka.sidora.tabularmetadata.web;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;

import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;

import edu.si.codebook.Codebook;

@Path("/")
public class TabularMetadataGeneratorEndpoint {

    @Inject
    private TabularMetadataGenerator generator;

    private static final Logger log = getLogger(TabularMetadataGeneratorEndpoint.class);

    @GET
    @Path("/")
    @Produces("text/xml")
    public Codebook get(@QueryParam("url") final URL url) throws IOException {
        return new Codebook().setMetadata(generator.getMetadata(url));
    }
}
