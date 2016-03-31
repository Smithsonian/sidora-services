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

package edu.si.sidora.tabularmetadata.smx.blueprint;

import edu.si.sidora.excel2tabular.ExcelToTabular;
import edu.si.codebook.Codebook;
import edu.si.sidora.tabularmetadata.TabularMetadataGenerator;
import org.apache.cxf.interceptor.Fault;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static edu.si.codebook.Codebook.codebook;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author A. Soroka
 * @author Jason Birkhimer
 *
 */
@Path("/")
public class TabularMetadataGeneratorEndpoint {

    private static final Logger log = getLogger(TabularMetadataGeneratorEndpoint.class);

    @Inject
    private ExcelToTabular translator;

    @Inject
    private TabularMetadataGenerator generator;

    public TabularMetadataGeneratorEndpoint() {
        generator = new TabularMetadataGenerator();
        translator = new ExcelToTabular();
    }

    @GET
    @Path("/")
    @Produces("text/xml")
    public Codebook getCodebook(@QueryParam("url") final URL url, @QueryParam("headers") final boolean hasHeaders) throws IOException, URISyntaxException {

        log.info("Parsing Started...");
        log.debug("QueryParams :: url: {}, hasHeaders: {}", url, hasHeaders);

        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        URL urlDecoded = new URL(uri.toASCIIString());
        String fileExt = urlDecoded.getFile().toLowerCase();

        if (!fileExt.endsWith(".csv") && !fileExt.endsWith(".xls") && !fileExt.endsWith(".xlsx")) {

            log.info("Parsing Failed! Invalid File Type '{}'!", fileExt);

            Fault fault = new Fault(new Exception("File '" + urlDecoded.getFile() + "' Not A Valid File Type!" ));
            fault.setStatusCode(400);

            log.info("Parsing Finished...");

            throw fault;

        } else if (fileExt.endsWith(".xls") || fileExt.endsWith(".xlsx")) {

            log.info("Parsing Excel file {}", urlDecoded.getFile());

            URL xlsUrl = translator.process(urlDecoded).get(0).toURI().toURL();

            log.info("Parsing Finished...");

            return codebook(generator.getMetadata(xlsUrl, hasHeaders));
        }

        log.info("Parsing CSV file {}", urlDecoded.getFile());
        log.info("Parsing Finished...");

        return codebook(generator.getMetadata(urlDecoded, hasHeaders));
    }
}
