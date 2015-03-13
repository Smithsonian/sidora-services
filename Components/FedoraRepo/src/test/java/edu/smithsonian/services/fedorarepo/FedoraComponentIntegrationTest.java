package edu.smithsonian.services.fedorarepo;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Message;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.test.junit4.CamelTestSupport;

public abstract class FedoraComponentIntegrationTest extends CamelTestSupport
{
    @EndpointInject(uri = "mock:result")
    protected MockEndpoint mockEnpoint;

    protected Message getMockMessage()
    {
        return getMockMessage(0);
    }

    protected Message getMockMessage(int idx)
    {
        return this.mockEnpoint.getExchanges().get(idx).getIn();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception
    {
        CamelContext context = super.createCamelContext();
        PropertiesComponent prop = context.getComponent("properties", PropertiesComponent.class);
        prop.setLocation("classpath:test.properties");

        try
        {
            System.setProperty("si.fedora.host", context.resolvePropertyPlaceholders("{{si.fedora.host}}"));
            System.setProperty("si.fedora.user", context.resolvePropertyPlaceholders("{{si.fedora.user}}"));
            System.setProperty("si.fedora.password", context.resolvePropertyPlaceholders("{{si.fedora.password}}"));
        }
        catch (Exception e)
        {
            System.out.println("FedoraComponentIntegrationTest: Fedora connection properties not set");
            throw e;
        }

        return context;
    }
}
