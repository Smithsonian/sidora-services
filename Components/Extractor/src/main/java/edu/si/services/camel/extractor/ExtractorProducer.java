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
import org.apache.commons.io.FilenameUtils;
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
    private StringBuilder errorMsg;

    public ExtractorProducer(ExtractorEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        Message in = exchange.getIn();
        File archiveFile = in.getBody(File.class);
        LOG.debug("Archive file to extract: {}", archiveFile.getName());

        String archiveFileBaseName = FilenameUtils.getBaseName(archiveFile.getName());
        String archiveFileExt = FilenameUtils.getExtension(archiveFile.getName());
        LOG.debug("Found archive file baseName={}, ext= {}", archiveFileBaseName, archiveFileExt);

        if (archiveFileExt == null && archiveFileExt.isEmpty())
        {   errorMsg = new StringBuilder();
            errorMsg.append("Improperly formatted file. No, file extension found for " + archiveFile.getName());
            LOG.error(errorMsg.toString());
            throw new ExtractorException(errorMsg.toString());
        }

        //Set location to extract to
        File cameraTrapDataOutputDir = new File(this.endpoint.getLocation(), archiveFileBaseName);

        //The ArchiveFactory can detect archive types based on file extensions and hand you the correct Archiver.
        Archiver archiver = ArchiverFactory.createArchiver(archiveFile);
        LOG.debug("Archive type: {}", archiver.getFilenameExtension());

        //Stream the archive rather then extracting directly to the file system.
        ArchiveStream archiveStream = archiver.stream(archiveFile);
        ArchiveEntry entry;

        //Extract each archive entry and check for the existence of a compressed folder otherwise create one
        while ((entry = archiveStream.getNextEntry()) != null) {
            LOG.debug("ArchiveStream Current Entry Name = {}, isDirectory = {}", entry.getName(), entry.isDirectory());

            //Check for a compressed folder
            if (entry.isDirectory()) {
                errorMsg = new StringBuilder();
                errorMsg.append("Extracting archive '" + archiveFile.getName() + "' failed! ");
                errorMsg.append("Directory '" + entry.getName() + "' found in archive!");

                log.error(errorMsg.toString());
                throw new ExtractorException(errorMsg.toString());
            } else {
                entry.extract(cameraTrapDataOutputDir); //extract the file
            }
        }

        archiveStream.close();

        LOG.debug("File path to be returned on exchange body: {}", cameraTrapDataOutputDir);

        String parent = null;

        if (cameraTrapDataOutputDir.getParentFile() != null)
        {
            parent = cameraTrapDataOutputDir.getParentFile().getName();
            LOG.debug("CamelFileParent: {}", cameraTrapDataOutputDir.getParentFile());
        }

        Map<String, Object> headers = in.getHeaders();

        headers.put("CamelFileLength", cameraTrapDataOutputDir.length());
        headers.put("CamelFileLastModified", cameraTrapDataOutputDir.lastModified());
        headers.put("CamelFileNameOnly", cameraTrapDataOutputDir.getName());
        headers.put("CamelFileNameConsumed", cameraTrapDataOutputDir.getName());
        headers.put("CamelFileName", cameraTrapDataOutputDir.getName());
        headers.put("CamelFileRelativePath", cameraTrapDataOutputDir.getPath());
        headers.put("CamelFilePath", cameraTrapDataOutputDir.getPath());
        headers.put("CamelFileAbsolutePath", cameraTrapDataOutputDir.getAbsolutePath());
        headers.put("CamelFileAbsolute", false);
        headers.put("CamelFileParent", parent);

        LOG.debug("Headers: {}", headers);

        exchange.getOut().setBody(cameraTrapDataOutputDir, File.class);

        exchange.getOut().setHeaders(headers);
    }
}
