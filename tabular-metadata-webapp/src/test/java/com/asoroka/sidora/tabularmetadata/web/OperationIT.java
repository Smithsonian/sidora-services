
package com.asoroka.sidora.tabularmetadata.web;

import static com.google.common.base.Charsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
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

import com.google.common.io.ByteStreams;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-xml/default-operation.xml")
public class OperationIT {

    @Inject
    private WebClient client;

    private static final Logger log = getLogger(OperationIT.class);

    @Test
    public void testUrlOperation() throws IOException {
        final Path dataFileLocation = createTempFile("testUrlOperation-", randomUUID().toString());
        final List<String> lines = asList("NAME,RANK,SERIAL NUMBER", "Kirk,Admiral,002", "McCoy,Doctor,567");
        write(dataFileLocation, lines, UTF_8);
        final Response r = client.query("url", "file://" + dataFileLocation.toAbsolutePath()).get();
        assertEquals(OK.getStatusCode(), r.getStatus());
        final String text = new String(ByteStreams.toByteArray((InputStream) r.getEntity()));
        log.trace("Received {}", text);
    }

}
