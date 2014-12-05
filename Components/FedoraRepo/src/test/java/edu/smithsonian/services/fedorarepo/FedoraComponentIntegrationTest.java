package edu.smithsonian.services.fedorarepo;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.EndpointInject;
import org.apache.camel.Message;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

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

    private static boolean connected = false;

    @BeforeClass
    public static void beforeClass()
    {
        String fileName = System.getProperty("fedoraUrl");
        if (fileName == null || fileName.isEmpty())
        {
            fileName = "http://localhost:8080/fedora";
            String msg = "Could not find 'fedoraUrl'. Defaulting to " + fileName;
            Logger.getLogger(FedoraComponentIntegrationTest.class.getName()).log(Level.WARNING, msg);
        }//end if
        else
        {
            //This is not very robust. But assuming that future developers won't be completely crazy
            String temp = fileName.toLowerCase();
            if (!temp.startsWith("http://"))
            {
                fileName = "http://" + fileName;
            }//end if
        }//end else

        try
        {
            new URL(fileName).getContent();

            connected = true;
        }
        catch (Exception ex)
        {
            String msg = String.format("Could not connect to Fedora at %s! Skipping test.", fileName);
            Logger.getLogger(FedoraComponentIntegrationTest.class.getName()).log(Level.WARNING, msg);
        }

    }

    @Before
    public void beforeTest()
    {
        Assume.assumeTrue(connected);
    }

}
