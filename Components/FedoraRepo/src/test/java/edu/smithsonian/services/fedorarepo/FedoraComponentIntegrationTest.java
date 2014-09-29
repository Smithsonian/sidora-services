package edu.smithsonian.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraClient;
import edu.smithsonian.services.fedorarepo.aggregators.PidAggregationStrategy;
import java.io.InputStream;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class FedoraComponentIntegrationTest extends CamelTestSupport
{

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
    public void testIngestWithPid() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        template.sendBody("direct:testIngestWithPid", null);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testIngest() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        template.sendBody("direct:testIngest", null);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);
        FedoraClient.purgeObject(pid).execute();
    }

    @Test
    public void testDatastream() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(2);

        template.sendBody("direct:testIngest", null);
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

        template.sendBody("direct:testIngest", null);
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

        template.sendBody("direct:testIngest", null);
        Message in = mock.getExchanges().get(0).getIn();

        String input = "Testing,input,CSV##More,test,";
        template.sendBodyAndHeaders("direct:testDatastreamCSV", input, in.getHeaders());

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(1).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));

        String pid = msg.getHeader("CamelFedoraPid", String.class);

//        GetDatastreamResponse ds = FedoraClient.getDatastream(pid, "OBJ").execute();
//        String actual = ds.getEntity(String.class);
//
//        assertEquals("Datastream content should be the same as the input", input, actual);
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
                from("direct:testIngestWithPid")
                        .to("fedora:nextPID")
                        .recipientList(simple("fedora:ingest?pid=${header.CamelFedoraPid}"))
                        .to("mock:result");
                from("direct:testIngest")
                        .to("fedora:ingest")
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
}
