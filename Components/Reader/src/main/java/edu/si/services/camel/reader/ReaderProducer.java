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
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.services.camel.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Reader producer.
 *
 * @author ddavis
 * @author jshingler
 * @version 1.0
 */
public class ReaderProducer extends DefaultProducer
{
    private static final Logger LOG = LoggerFactory.getLogger(ReaderProducer.class);

    private ReaderEndpoint endpoint;

    public ReaderProducer(ReaderEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        String body = exchange.getIn().getBody(String.class);
        Map<String, Object> headers = exchange.getIn().getHeaders();

        File file = findFile(body, headers);
        Map<String, Object> updateHeaders = updateHeaders(file, headers);

        Message out = exchange.getOut();
        if ("text".equalsIgnoreCase(this.endpoint.getType()))
        {
            //Scanner din = new Scanner(file, "UTF-8").useDelimiter("\\Z");
            //out.setBody(din.next(), String.class);
            //din.close();

            Path filePath = file.toPath();
            byte[] encoded = Files.readAllBytes(filePath);
            out.setBody(new String(encoded, StandardCharsets.UTF_8), String.class);
        }
        else
        {
            // Change to GenericFile...
            out.setBody(file, File.class);
        }

        out.setHeaders(updateHeaders);
    }

    private File findFile(String body, Map<String, Object> headers) throws FileNotFoundException
    {
        File file = new File(body);
        if (!file.exists())
        {
            Object path = headers.get("CamelFileAbsolutePath");
            if (path != null && (path instanceof String))
            {
                File parent = new File((String) path);
                if (parent.isFile())
                {
                    parent = parent.getParentFile();
                }//end if

                file = new File(parent, body);
            }//end if
        }//end if

        if (!file.exists())
        {
            throw new FileNotFoundException("Could not find file " + body);
        }
        return file;
    }

    private Map<String, Object> updateHeaders(File file, Map<String, Object> oldHeaders)
    {
        Map<String, Object> headers = new HashMap<String, Object>(oldHeaders);

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
        headers.put("CamelFileParent", file.getParentFile().getName());

        return headers;
    }

}// end ReadProducer.class
