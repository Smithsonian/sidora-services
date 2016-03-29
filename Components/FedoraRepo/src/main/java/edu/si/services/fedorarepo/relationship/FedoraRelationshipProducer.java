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

package edu.si.services.fedorarepo.relationship;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import edu.si.services.fedorarepo.Headers;
import edu.si.services.fedorarepo.base.FedoraProducer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

/**
 *
 * @author jshingler
 */
public class FedoraRelationshipProducer extends FedoraProducer
{

    private final FedoraRelationshipEndpoint endpoint;
    private final Pattern simplePattern = Pattern.compile("\\$\\{(?<input>.+)\\}$");

    public FedoraRelationshipProducer(FedoraRelationshipEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {

        String predicate = this.endpoint.getPredicate();
        if (predicate == null || predicate.isEmpty())
        {
            throw new RuntimeException("fedora:relationship producer requires a predicate");
        }

        Message in = exchange.getIn();

        String parentPid = getPid(this.endpoint.getParentPid(), in);

        if (parentPid == null)
        {
            throw new RuntimeException("fedora:relationship producer could not find parent concepts PID");
        }

        String childPid = getPid(this.endpoint.getChildPid(), in);

        if (childPid == null)
        {
            throw new RuntimeException("fedora:relationship producer could not find child concepts PID");
        }

        FedoraResponse response = FedoraClient.addRelationship(addNamespace(parentPid)).predicate(predicate).object(addNamespace(childPid)).execute();

        Map<String, Object> headers = in.getHeaders();
        headers.put(Headers.STATUS, response.getStatus());

        Message out = exchange.getOut();
        out.setHeaders(headers);
        out.setBody(in.getBody());

    }

    private String addNamespace(String value)
    {
        String namespace = this.endpoint.getNameSpace();
        if (namespace == null)
        {
            namespace = "";
        }
        else if (!namespace.endsWith("/"))
        {
            namespace += "/";
        }

        return String.format("%s%s", namespace, value);
    }

    private String getPid(String pid, Message msg)
    {
        if (pid == null)
        {
            return null;
        }
        else if (pid.startsWith("${"))
        {
            Matcher m = simplePattern.matcher(pid);
            if (m.matches())
            {
                String input = m.group("input");
                if (input.startsWith("header."))
                {
                    input = input.replace("header.", "");

                    return msg.getHeader(input, String.class);
                }
                else if (input.equalsIgnoreCase("body"))
                {
                    return msg.getBody(String.class);
                }
                else
                {
                    return null;
                }
            }
            else
            {
                //TODO: Add log
                return null;
            }
        }
        else
        {
            return pid;
        }
    }

}
