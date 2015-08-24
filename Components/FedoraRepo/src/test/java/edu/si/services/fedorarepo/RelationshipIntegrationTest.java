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

import com.yourmediashelf.fedora.client.FedoraClient;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

/**
 * Tests SIdora relationship handling functions.
 *
 * @author davisda
 * @author jshingler
 */
public class RelationshipIntegrationTest extends FedoraComponentIntegrationTest
{
    @Test
    public void testRelationship() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(3);
        //Create Parent

        template.sendBody("direct:testCreate", null);
        Message in = this.getMockMessage();

        String parentPid = in.getHeader(Headers.PID, String.class);

        // Add RELS-EXT to the parent.
        String input = getEmptyRELS_EXT(parentPid);
        template.sendBodyAndHeaders("direct:testDatastream", input, in.getHeaders());

        in.setHeader("FedoraParentConcept", parentPid);

        // Create a Child.
        template.sendBodyAndHeaders("direct:testCreateNoPid", null, in.getHeaders());

        in = this.getMockMessage(1);
        String childPid = in.getHeader(Headers.PID, String.class);

        try
        {
            assertNotEquals("Parent and Child PIDs should not be the same", parentPid, childPid);
            assertEquals("Parent PID should be the same", parentPid, in.getHeader("FedoraParentConcept", String.class));

            // Add hasRelationship from Child to Parent.
            template.sendBodyAndHeaders("direct:testHasRelationship", null, in.getHeaders());
//            in = this.getMockMessage(2);

            //Verify
            assertMockEndpointsSatisfied();
            //TODO: Add more verification

        }
        finally
        {
            // Clean up
            if (parentPid != null)
            {
                FedoraClient.purgeObject(parentPid).execute();
            }
            if (childPid != null)
            {
                FedoraClient.purgeObject(childPid).execute();
            }
        }
    }

    @Test
    public void testConcept() throws Exception
    {
        mockEnpoint.expectedMinimumMessageCount(3);
        //Create Parent

        template.sendBody("direct:testCreate", null);
        Message in = this.getMockMessage();

        String parentPid = in.getHeader(Headers.PID, String.class);

        // Add RELS-EXT to the parent.
        String input = getEmptyRELS_EXT(parentPid);
        template.sendBodyAndHeaders("direct:testDatastream", input, in.getHeaders());

        in.setHeader("FedoraParentConcept", parentPid);

        // Create a Child.
        template.sendBodyAndHeaders("direct:testCreateNoPid", null, in.getHeaders());

        in = this.getMockMessage(1);
        String childPid = in.getHeader(Headers.PID, String.class);

        try
        {
            assertNotEquals("Parent and Child PIDs should not be the same", parentPid, childPid);
            assertEquals("Parent PID should be the same", parentPid, in.getHeader("FedoraParentConcept", String.class));

            // Add hasRelationship from Child to Parent.
            template.sendBodyAndHeaders("direct:testHasConcept", null, in.getHeaders());
//            in = this.getMockMessage(2);

            //Verify
            assertMockEndpointsSatisfied();
            //TODO: Add more verification

        }
        finally
        {
            // Clean up.
            if (parentPid != null)
            {
                FedoraClient.purgeObject(parentPid).execute();
            }
            if (childPid != null)
            {
                FedoraClient.purgeObject(childPid).execute();
            }
        }
    }

    protected String getEmptyRELS_EXT(String pid)
    {
        return "<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:default=\"http://islandora.org/ontologies/metadata#\" xmlns:fedora=\"info:fedora/fedora-system:def/relations-external#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:islandora=\"http://islandora.ca/ontology/relsext#\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
                + "    <rdf:Description rdf:about=\"info:fedora/" + pid + "\">\n"
                + "        <fedora-model:hasModel rdf:resource=\"info:fedora/si:cameraTrapCModel\"/>\n"
                + "        <fedora-model:hasModel rdf:resource=\"info:fedora/si:resourceCModel\"/>\n"
                + "        <default:orginal_metadata xmlns=\"http://islandora.org/ontologies/metadata#\">TRUE</default:orginal_metadata>\n"
                + "    </rdf:Description>\n"
                + "</rdf:RDF>";
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:testHasRelationship")
                        .to("fedora:hasRelationship?parentPid=${header.FedoraParentConcept}&childPid=${header.CamelFedoraPid}")
                        .to("mock:result");

                from("direct:testHasConcept")
                        .to("fedora:hasConcept?parentPid=${header.FedoraParentConcept}&childPid=${header.CamelFedoraPid}")
                        .to("mock:result");

                // Helper routes
                from("direct:testCreate")
                        .to("fedora:create")
                        .to("mock:result");

                from("direct:testCreateNoPid")
                        .to("fedora:create?pid=null")
                        .to("mock:result");

                from("direct:testDatastream")
                        .to("fedora:addDatastream?name=RELS-EXT");
            }
        };
    }
}
