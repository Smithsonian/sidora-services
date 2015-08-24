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
