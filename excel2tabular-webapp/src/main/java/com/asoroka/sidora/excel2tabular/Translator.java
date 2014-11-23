
package com.asoroka.sidora.excel2tabular;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;

@Path("/")
@Provider
public class Translator {

    @Inject
    private ExcelToTabular translator;

    private static final Logger log = getLogger(Translator.class);

    @GET
    @Path("/")
    @Produces("multipart/mixed")
    public MultipartBody get(@QueryParam("url") final URL url) {
        log.debug("Retrieving from URL: {}", url);
        final List<File> files = translator.process(url);
        int sheetNumber = 1;
        final List<Attachment> attachments = new ArrayList<>(files.size());
        for (final File file : files) {
            attachments.add(new Attachment("Sheet:" + sheetNumber++, "text/csv", file));
        }
        return new MultipartBody(attachments, true);
    }
}
