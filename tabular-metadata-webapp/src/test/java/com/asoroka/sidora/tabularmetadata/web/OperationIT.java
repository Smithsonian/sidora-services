
package com.asoroka.sidora.tabularmetadata.web;

import static com.google.common.base.Charsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import com.google.common.io.Resources;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-xml/default-operation.xml")
public class OperationIT {

    private static final Logger log = getLogger(OperationIT.class);

    private static final String properResponse;
    static {
        try {
            properResponse =
                    Resources.toString(new File("src/test/resources/OperationITData")
                            .toURI().toURL(), UTF_8);
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    @Inject
    private WebClient client;

    @Test
    public void testUrlOperation() throws IOException, SAXException {
        final Path dataFileLocation = createTempFile("testUrlOperation", "-" + randomUUID());
        final List<String> lines = asList("NAME,RANK,SERIAL NUMBER", "Kirk,Admiral,002", "McCoy,Doctor,567");
        write(dataFileLocation, lines, UTF_8);
        final Response r = client.query("url", "file://" + dataFileLocation.toAbsolutePath()).get();
        assertEquals(OK.getStatusCode(), r.getStatus());
        final String text = r.readEntity(String.class);
        log.trace("Got response: {}", text);
        assertXMLEqual(properResponse, text);
    }
}
