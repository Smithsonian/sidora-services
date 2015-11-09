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

package edu.si.services.fedorarepo;

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
