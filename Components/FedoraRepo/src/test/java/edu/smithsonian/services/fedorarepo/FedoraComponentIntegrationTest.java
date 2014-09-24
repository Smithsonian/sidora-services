package edu.smithsonian.services.fedorarepo;

import java.io.InputStream;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class FedoraComponentIntegrationTest extends CamelTestSupport
{

    //TODO: Add purge to end of all routes to remove test objects
//    @Test
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

//    @Test
    public void testIngestWithPid() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        template.sendBody("direct:testIngestWithPid", null);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));
    }

//    @Test
    public void testIngest() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        template.sendBody("direct:testIngest", null);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(0).getIn();

        assertEquals("Ingest Status should have been 201", 201, msg.getHeader("CamelFedoraStatus"));
    }

    //FedoraClient.addDatastream(pid, "OBJ").content(new FileInputStream(image)).mimeType("image/jpeg").controlGroup("M").execute(client);
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
            }
        };
    }
}
