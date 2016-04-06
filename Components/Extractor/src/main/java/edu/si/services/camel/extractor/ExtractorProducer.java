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

package edu.si.services.camel.extractor;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.rauschig.jarchivelib.ArchiveEntry;
import org.rauschig.jarchivelib.ArchiveStream;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * The Extractor producer.
 *
 * @author jshingler
 * @author jbirkhimer
 * @version 1.0
 */
public class ExtractorProducer extends DefaultProducer
{
    private static final Logger LOG = LoggerFactory.getLogger(ExtractorProducer.class);
    private final ExtractorEndpoint endpoint;

    public ExtractorProducer(ExtractorEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        Message in = exchange.getIn();
        File inBody = in.getBody(File.class);

        //Location to extract to
        File outFolder = new File(this.endpoint.getLocation());

        //File path for the exchange body
        File file = null;

        //Create a string array of filename and extension(s)
        String[] split = inBody.getName().split("\\.");
        if (split.length < 2)
        {
            LOG.error("Improperly formatted file. No, file extension found for " + inBody.getName());
            throw new Exception();
        }

        LOG.debug("inBody archive file name and extension(s): {}", split);

        //The ArchiveFactory can detect archive types based on file extensions and hand you the correct Archiver.
        Archiver archiver = ArchiverFactory.createArchiver(inBody);
        LOG.debug("Archive type: {}", archiver.getFilenameExtension());

        //Stream the archive rather then extracting directly to the file system.
        ArchiveStream archiveStream = archiver.stream(inBody);
        ArchiveEntry entry;

        String compressedFolder = null;

        //Extract each archive entry and check for the existence of a compressed folder otherwise create one
        while ((entry = archiveStream.getNextEntry()) != null) {
            LOG.debug("ArchiveStream Current Entry Name = {}, isDirectory = {}", entry.getName(), entry.isDirectory());

            //Check for a compressed folder
            if (entry.isDirectory()) {
                log.debug("Found Directory '{}' in archive!", entry.getName());
                compressedFolder = entry.getName(); //Get the name of the compressed folder
                file = new File(outFolder, compressedFolder); //update the file path to be returned on exchange body
            } else if (compressedFolder == null) {
                compressedFolder = split[0]; //Set the missing compressed folder name
                log.warn("No directory found in archive, Set directory '{}' to extract to!", compressedFolder);
                outFolder = new File(outFolder, compressedFolder); //Update the outFolder location to extract files to
                file = outFolder; //update the file path to be returned on exchange body
            }
            entry.extract(outFolder); //extract the file
        }

        archiveStream.close();

        LOG.debug("File path to be returned on exchange body: {}", file);

        String parent = null;

        if (file.getParentFile() != null)
        {
            parent = file.getParentFile().getName();
            LOG.debug("CamelFileParent: {}", file.getParentFile());
        }

        Map<String, Object> headers = in.getHeaders();

        headers.put("CamelFileLength", file.length());
        headers.put("CamelFileLastModified", file.lastModified());
        headers.put("CamelFileNameOnly", file.getName());
        headers.put("CamelFileNameConsumed", file.getName());
        headers.put("CamelFileName", file.getName());
        headers.put("CamelFileRelativePath", file.getPath());
        headers.put("CamelFilePath", file.getPath());
        headers.put("CamelFileAbsolutePath", file.getAbsolutePath());
        headers.put("CamelFileAbsolute", false);
        headers.put("CamelFileParent", parent);

        exchange.getOut().setBody(file, File.class);

        exchange.getOut().setHeaders(headers);
    }
}
