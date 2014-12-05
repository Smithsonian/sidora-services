/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.smithsonian.services.fedorarepo;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.response.IngestResponse;
import java.io.InputStream;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author jshingler
 */
public class SearchIntegrationTest extends FedoraComponentIntegrationTest
{

    @Test
    @Ignore
    public void testSearch() throws Exception
    {
        //1. Create Object to Ingest
        InputStream input = this.getClass().getResourceAsStream("/si_121911.xml");
        if (input == null)
        {
            throw new Exception("No input was found");
        }

        String pid = null;
        try
        {
            //2. Ingest Test Object
            IngestResponse response = FedoraClient.ingest().content(input).execute();
            pid = response.getPid();

            //3. Search for Object
            String parentPid = "si:121910";
            String expectedLabel = "Dumb Effect of consumptive and non-consumptive recreation on mid-Atlantic wildlife";
            String query = String.format("select $o $title from <#ri> where  $title <mulgara:is> '%s' and  $o <fedora-model:label> $title and <info:fedora/'%s'> <fedora-rels-ext:hasConcept> $o", expectedLabel, parentPid);

            template.sendBody("direct:testSearch", query);

            //4. Verify Search results
            Message msg = this.getMockMessage();
            String body = msg.getBody(String.class);

            //4.1 Check PIDs
            String actualPid = "";
            assertEquals(pid, actualPid);

            //4.2 Check Label
            String actualLabel = "";
            assertEquals(expectedLabel, actualLabel);

        }
        finally
        {
            if (pid != null)
            {
                //5. Delete Test Object
                FedoraClient.purgeObject(pid).execute();
            }

            input.close();
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testSearch")
                        .to("fedora:search?lang=itql&limit=1")
                        .to("mock:result")
                        .end();
            }
        };
    }
}
