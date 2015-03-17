/*
 * Copyright 2015 Smithsonian Institution.  
 *
 * Permission is granted to use, copy, modify,
 * and distribute this software and its documentation for educational, research
 * and non-profit purposes, without fee and without a signed licensing
 * agreement, provided that this notice, including the following two paragraphs,
 * appear in all copies, modifications and distributions.  For commercial
 * licensing, contact the Office of the Chief Information Officer, Smithsonian
 * Institution, 380 Herndon Parkway, MRC 1010, Herndon, VA. 20170, 202-633-5256.
 *  
 * This software and accompanying documentation is supplied "as is" without
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
