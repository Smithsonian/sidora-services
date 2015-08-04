/*
 * Copyright 2015 Smithsonian Institution.
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
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package com.jbirkhimer.sidora.tabularmetadata.smx.blueprint;

import com.asoroka.sidora.excel2tabular.ExcelToTabular;
import com.asoroka.sidora.tabularmetadata.TabularMetadataGenerator;
import edu.si.codebook.Codebook;
import edu.si.codebook.MultiSheet;
import org.apache.cxf.interceptor.Fault;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static edu.si.codebook.Codebook.codebook;
import static edu.si.codebook.MultiSheet.multisheet;

/**
 * Created by Jason Birkhimer on 7/14/2015.
 */
@Path("/")
public class TabularMetadataGeneratorEndpoint {

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
    public Codebook getCodebook(@QueryParam("url") final URL url) throws IOException, URISyntaxException {
        String fileExt = url.getFile().toLowerCase();
        if (!fileExt.endsWith(".csv") && !fileExt.endsWith(".xls") && !fileExt.endsWith(".xlsx")) {
            Fault fault = new Fault(new Exception("File Not Valid"));
            fault.setStatusCode(400);
            throw fault;
        } else if (fileExt.endsWith(".xls") || fileExt.endsWith(".xlsx")) {
            final URL xlsUrl = translator.process(url).get(0).toURI().toURL();
            return codebook(generator.getMetadata(xlsUrl));
        }

        return codebook(generator.getMetadata(url));
    }

    /*@GET
    @Path("/csv")
    @Produces("text/xml")
    public Codebook getCodebookCSV(@QueryParam("url") final URL url) throws IOException {
        return codebook(generator.getMetadata(url));
    }

    @GET
    @Path("/excel")
    @Produces("text/xml")
    public Codebook getCodebookExcel(@QueryParam("url") final URL url) throws IOException {

        final URL result;
        result = translator.process(url).get(0).toURI().toURL();

        return codebook(generator.getMetadata(result));
    }

    @GET
    @Path("/excel/multi-sheet")
    @Produces("text/xml")
    public MultiSheet getCodebookMultiSheetExcel(@QueryParam("url") final URL url) throws IOException {
        Codebook result;
        //log.debug("Retrieving from URL: {}", url);
        final List<File> files = translator.process(url);
        int sheetNumber = 1;
        final List<Codebook> attachments = new ArrayList<>(files.size());
        for (final File file : files) {
            generator = new TabularMetadataGenerator();
            result = codebook(generator.getMetadata(file.toURI().toURL()));
            attachments.add(result);
        }
        return multisheet(attachments);
    }*/



}
