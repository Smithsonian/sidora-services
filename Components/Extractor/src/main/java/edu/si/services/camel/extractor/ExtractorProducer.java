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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Extractor producer.
 *
 * @author jshingler
 * @version 1.0
 */
public class ExtractorProducer extends DefaultProducer
{
    private static final Logger LOG = LoggerFactory.getLogger(ExtractorProducer.class);
    private final ExtractorEndpoint endpoint;
    private boolean isZipArchive;

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
        File outFolder = new File(this.endpoint.getLocation());

        //Create a string array of filename and extension(s)
        String[] split = inBody.getName().split("\\.");
        if (split.length < 2)
        {
            LOG.error("Improperly formatted file not. No, file extension found for " + inBody.getName());
            throw new Exception();
        }

        //FIXME: Change to stream extraction so that can get the name of compressed
        //      folder. Right not determining the folder that is withing the archive
        //      is a complete hack!!! It works and handles the edge cases but can
        //      be more robust.
        Archiver archiver = getArchiver(split);

        if (archiver.getFilenameExtension().equalsIgnoreCase(".zip")) {
            outFolder = new File(outFolder, split[0]);
            isZipArchive = true;
            log.debug("New Zip outFolder = {}", outFolder.getPath());
        }

        //List of original files
        List<File> org;

        //If the outFolder exists store the list of files before extracting the archive
        if (outFolder.exists())
        {
            org = Arrays.asList(outFolder.listFiles());
        }
        else
        {
            org = Collections.emptyList();
        }

        //Extract the archive
        archiver.extract(inBody, outFolder);

        //Store the list of files in the outFolder after extracting the archive
        List<File> mod = new ArrayList<File>(Arrays.asList(outFolder.listFiles()));

        //Remove the original files from before extracting the archive from the list of files after extracting the archive
        mod.removeAll(org);

        File file = outFolder;

        //If list of modified files is empty after removing the list of original files ???
        if (mod.isEmpty())
        {
            /**
             * TODO - Need a better test to see if the directory is there or not and handle the zip archive accordingly
             * the current zip archives only contain the deployment-manifest.xml and jpg images
             * while the other archives formats contain the deployment_manifest.xml and jpg images withing a directory
             *
             * deploymentPkg.zip
             *      - deployment_manifest.xml
             *      - image1.jpg
             *      - imageX.jpg
             *
             *  deploymentPkg.tar(.gz)
             *      - deploymentPkg_Dir
             *          - deployment_manifest.xml
             *          - image1.jpg
             *          - imageX.jpg
             *
             *  The code has been updated to correctly extract the current zip archives that we are getting without the directory
             *  However, we should have a better test to see if the directory is there or not and handle the zip archive accordingly
             *
             */
            File temp = (isZipArchive ? outFolder : new File(outFolder, split[0]));
            //File temp = new File(outFolder, split[0]);

            if (temp.exists())
            {
                file = temp;
            }
        }
        else if (mod.size() == 1)
        {
            file = mod.get(0);
        }

        Map<String, Object> headers = in.getHeaders();

        String parent = null;
        if (file.getParentFile() != null)
        {
            parent = file.getParentFile().getName();
        }

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

    private Archiver getArchiver(String[] extention)
    {
        if (extention.length == 3)
        {
            return ArchiverFactory.createArchiver(extention[1], extention[2]);
        }
        else
        {
            return ArchiverFactory.createArchiver(extention[1]);
        }
    }

}
