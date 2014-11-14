package edu.smithsonian.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.response.GetObjectProfileResponse;
import edu.smithsonian.services.fedorarepo.aggregators.PidAggregationStrategy;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.EndpointInject;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FedoraComponentIntegrationTest extends CamelTestSupport
{

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint mockEnpoint;

    private Message getMockMessage()
    {
        return getMockMessage(0);
    }

    private Message getMockMessage(int idx)
    {
        return this.mockEnpoint.getExchanges().get(idx).getIn();
    }

    @Test
    public void testGetNextPid() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        template.sendBody("direct:testPid", "Integration Test");

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        assertNotNull("PID Should not be null", pid);

        String body = msg.getBody(String.class);
        assertEquals("Body should be the same", "Integration Test", body);
    }

    @Test
    public void testGetNextPidWithNamespace() throws Exception
    {
//        MockEndpoint mock = getMockEndpoint("mock:result");
        mockEnpoint.expectedMinimumMessageCount(1);

        template.sendBody("direct:testPidWithNamespace", "Integration Test");

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();
        String pid = msg.getHeader("CamelFedoraPid", String.class);

        assertNotNull("PID should not be null", pid);
        assertTrue("PID namespace should start with 'namespaceTest'.", pid.startsWith("namespaceTest"));

        String body = msg.getBody(String.class);
        assertEquals("Body should be the same", "Integration Test", body);

    }

    @Test
    public void testCreateWithPid() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        template.sendBody("direct:testCreateWithPid", null);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testCreate() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        String expectedBody = "Test Create Body";
        template.sendBody("direct:testCreate", expectedBody);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        assertEquals("Message Body should be", expectedBody, msg.getBody(String.class));
        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testCreateWithParams() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(1);

        template.sendBody("direct:testCreateParams", null);

        assertMockEndpointsSatisfied();

        Message msg = this.getMockMessage();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        assertTrue("PID should have namespace 'namespaceTest'", pid.startsWith("namespaceTest:"));

        GetObjectProfileResponse profile = FedoraClient.getObjectProfile(pid).execute();
        assertEquals("Owner wasn't properly set", "Test Owner", profile.getOwnerId());
        assertEquals("Label wasn't set properly", "This is a label 2 test", profile.getLabel());

        String foxml = FedoraClient.getObjectXML(pid).execute().getEntity(String.class);
        String logMsg = "Logging action 4 create/ingest";
        assertStringContains(foxml, logMsg);

        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testCreateHeaders() throws Exception
    {
        mockEnpoint.expectedMessageCount(2);

        String expectedOwner = "ActualOwner";

        template.sendBodyAndHeader("direct:testCreateHeaders", "Test", "CamelFedoraOwner", expectedOwner);

        assertMockEndpointsSatisfied();

        String pid = getMockMessage().getHeader("CamelFedoraPid", String.class);

        try
        {
            GetObjectProfileResponse profile = FedoraClient.getObjectProfile(pid).execute();
            assertEquals("Owner wasn't properly set", expectedOwner, profile.getOwnerId());
        }
        catch (FedoraClientException fedoraClientException)
        {
            fail("PID was not set by header");
        }
    }

    @Test
    public void testCreateOverwriteHeaders() throws Exception
    {
        mockEnpoint.expectedMessageCount(1);

        String badOwner = "Incorrect Owner";
        String badPid = "bad:pid";

        Map<String, Object> inHeaders = new HashMap<String, Object>();
        inHeaders.put("CamelFedoraOwner", badOwner);
        inHeaders.put("CamelFedoraPid", badPid);

        template.sendBodyAndHeaders("direct:testCreateOverwriteHeaders", null, inHeaders);

        assertMockEndpointsSatisfied();

        String pid = getMockMessage().getHeader("CamelFedoraPid", String.class);
        assertNotNull("PID should have been set", pid);
        assertNotEquals("PID was not overwritten", badPid, pid);

        try
        {
            GetObjectProfileResponse profile = FedoraClient.getObjectProfile(pid).execute();
            assertEquals("Owner wasn't properly set", "URI Set Owner", profile.getOwnerId());
        }
        catch (FedoraClientException fedoraClientException)
        {
            fail("Could not find Fedora object from PID");
        }

    }

    @Test
    public void testDatastream() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testCreate", null);
        Message in = mock.getExchanges().get(0).getIn();

        InputStream input = this.getClass().getResourceAsStream("/test-image.jpg");
        template.sendBodyAndHeaders("direct:testDatastream", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testDataStreamNoPid() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testCreate", null);
        Message in = mock.getExchanges().get(0).getIn();

        InputStream input = this.getClass().getResourceAsStream("/test-image.jpg");
        template.sendBodyAndHeaders("direct:testDatastreamNoPid", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testDataStreamString() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testCreate", null);
        Message in = mock.getExchanges().get(0).getIn();

        String input = "Testing,input,CSV##More,test,";
        template.sendBodyAndHeaders("direct:testDatastreamCSV", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);

        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testPidAggregator() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(4);

        StringBuilder sb = new StringBuilder("<testing>");
        sb.append("\t<test>test1</test>");
        sb.append("\t<test>test2</test>");
        sb.append("\t<test>test3</test>");
        sb.append("</testing>");

        template.sendBody("direct:testAggregator", sb.toString());

        assertMockEndpointsSatisfied();

        String expectedPids = null;

        for (int i = 0; i < 3; i++)
        {
            Message in = mock.getExchanges().get(i).getIn();
            String pid = in.getHeader("CamelFedoraPid", String.class);
            assertNotNull("PID should not be null", pid);

            if (expectedPids == null)
            {
                expectedPids = pid.trim();
            }//end if
            else
            {
                expectedPids += "," + pid.trim();
            }//end else

            String body = in.getBody(String.class);
            assertEquals("Test body should equal test#", body, String.format("test%d", i + 1));
        }//end for

        Message in = mock.getExchanges().get(3).getIn();

        String actualPids = in.getBody(String.class);

        assertNotNull("Aggregator body shouldn't be null", actualPids);
        assertArrayEquals("PIDs should be equal", expectedPids.split(","), actualPids.split(","));

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testPid")
                        .to("fedora:nextPid")
                        .to("mock:result");
                from("direct:testPidWithNamespace")
                        .to("fedora:nextPid?namespace=namespaceTest")
                        .to("mock:result");
                from("direct:testCreateWithPid")
                        .to("fedora:nextPID")
                        .recipientList(simple("fedora:create?pid=${header.CamelFedoraPid}"))
                        .to("mock:result");

                from("direct:testCreateParams")
                        .to("fedora:create?label=This is a label 2 test&owner=Test Owner&namespace=namespaceTest&log=Logging action 4 create/ingest")
                        .to("mock:result");
                from("direct:testCreate")
                        .to("fedora:create")
                        .to("mock:result");
                from("direct:testCreateHeaders")
                        .to("fedora:nextPid")
                        .to("mock:result")
                        .to("fedora:create")
                        .to("mock:result");
                from("direct:testCreateOverwriteHeaders")
                        .to("fedora:create?owner=URI Set Owner&pid=null")
                        .to("mock:result");

                from("direct:testDatastream")
                        .recipientList(simple("fedora:datastream?pid=${header.CamelFedoraPid}&name=OBJ&type=image/jpeg&group=M"))
                        .to("mock:result");
                from("direct:testDatastreamNoPid")
                        .recipientList(simple("fedora:datastream?name=OBJ&type=image/jpeg&group=M"))
                        .to("mock:result");
                from("direct:testDatastreamCSV")
                        .recipientList(simple("fedora:datastream?name=OBJ&type=text/csv&group=M"))
                        .to("mock:result");

                from("direct:testAggregator")
                        .split(xpath("//test/text()"), new PidAggregationStrategy())
                        .to("fedora:nextPid")
                        .to("mock:result")
                        .end()
                        .to("mock:result");
            }
        };
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
