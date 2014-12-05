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
package edu.smithsonian.services.fedorarepo.ingest;

import edu.smithsonian.services.fedorarepo.FedoraComponent;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriParam;

/**
 *
 * @author jshingler
 */
public class FedoraIngestEnpoint extends DefaultEndpoint
{

    protected boolean createType;

    @UriParam
    private String pid;
    @UriParam
    private String label;
    @UriParam
    private String log;
    @UriParam
    private String namespace;
    @UriParam
    private String owner;

    //TODO: Finish Injest with all options...
//    private String format;
//    private boolean ignoreMime;

    public FedoraIngestEnpoint()
    {
        this.createType = false;
    }

    public FedoraIngestEnpoint(String uri, FedoraComponent component)
    {
        this(uri, component, false);
    }

    public FedoraIngestEnpoint(String uri, FedoraComponent component, boolean isCreateType)
    {
        super(uri, component);
        this.createType = isCreateType;
    }
    @Override
    public Producer createProducer() throws Exception
    {
        return new FedoraIngestProducer(this, createType);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception
    {
        throw new UnsupportedOperationException("fedora:ingest cannot start a route");
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    public String getPid()
    {
        return pid;
    }

    public void setPid(String pid)
    {
        this.pid = pid;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public String getLog()
    {
        return log;
    }

    public void setLog(String log)
    {
        this.log = log;
    }

}
