/* 
 * Copyright 2014 Smithsonian Institution.  
 *
 * Permission is granted to use, copy, modify,
 * and distribute this software and its documentation for educational, research
 * and non-profit purposes, without fee and without a signed licensing
 * agreement, provided that this notice, including the following two paragraphs,
 * appear in all copies, modifications and distributions.  For commercial
 * licensing, contact the Office of the Chief Information Officer, Smithsonian
 * Institution, 380 Herndon Parkway, MRC 1010, Herndon, VA. 20170, 202-633-5256.
 *  
 * This software and accompanying documentation is supplied â€œas isâ€� without
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
 */
package edu.smithsonian.services.camel.extractor;

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

    public ExtractorProducer(ExtractorEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) throws Exception
    {
        Message in = exchange.getIn();
        File inBody = in.getBody(File.class);

        File outFolder = new File(this.endpoint.getLocation());
        List<File> org;
        if (outFolder.exists())
        {
            org = Arrays.asList(outFolder.listFiles());
        }
        else
        {
            org = (List<File>) Collections.EMPTY_LIST;
        }
        String[] split = inBody.getName().split("\\.");
        if (split.length < 2)
        {
            throw new Exception();
        }

        Archiver archiver = getArchiver(split);
        archiver.extract(inBody, outFolder);

        List<File> mod = new ArrayList<File>(Arrays.asList(outFolder.listFiles()));

        mod.removeAll(org);

        File file = outFolder;

        if (mod.size() == 1)
        {
            file = mod.get(0);
        }

        Map<String, Object> headers = in.getHeaders();

        String parent = "";
        if (file.getParentFile() != null)
        {
            parent = file.getParentFile().getName();
        }
//      FIXME: Use Camel GenericFile to correctly populate these fields!!!
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
