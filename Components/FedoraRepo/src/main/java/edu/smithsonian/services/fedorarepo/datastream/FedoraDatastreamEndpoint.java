/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo.datastream;

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
public class FedoraDatastreamEndpoint extends DefaultEndpoint
{

    @UriParam
    private String pid;
    @UriParam
    private String name;
    @UriParam
    private String type;
    @UriParam
    private String group;
    @UriParam
    private String label;
    @UriParam
    private String location;
    @UriParam
    private String state;
    @UriParam
    private String format;
    @UriParam
    private String log;
    @UriParam
    private boolean versionable;

//    @UriParam private List<String> altIDs;
    //TODO: Add checksum verification(?)
    public FedoraDatastreamEndpoint()
    {
    }

    public FedoraDatastreamEndpoint(String uri, FedoraComponent component)
    {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception
    {
        return new FedoraDatastreamProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception
    {
        throw new UnsupportedOperationException("fedora:datastream cannot start a route");
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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public String getLog()
    {
        return log;
    }

    public void setLog(String log)
    {
        this.log = log;
    }

    public boolean isVersionable()
    {
        return versionable;
    }

    public void setVersionable(boolean versionable)
    {
        this.versionable = versionable;
    }
}
