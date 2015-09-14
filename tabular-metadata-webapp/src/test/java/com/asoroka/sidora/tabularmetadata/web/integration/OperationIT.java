
package com.asoroka.sidora.tabularmetadata.web.integration;

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

	private static final String properResponse1, properResponse2;

	static {
		try {
			properResponse1 = Resources.toString(new File("src/test/resources/OperationITData1").toURI().toURL(),
					UTF_8);
			properResponse2 = Resources.toString(new File("src/test/resources/OperationITData2").toURI().toURL(),
					UTF_8);
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
		final Response r = client.resetQuery().query("url", "file://" + dataFileLocation.toAbsolutePath()).get();
		assertEquals(OK.getStatusCode(), r.getStatus());
		final String text = r.readEntity(String.class);
		log.trace("Got response: {}", text);
		assertXMLEqual(properResponse1, text);
		dataFileLocation.toFile().delete();
	}

	@Test
	public void testUrlOperationWithHeaderDeclaration() throws IOException, SAXException {
		final Path dataFileLocation = createTempFile("testUrlOperationWithHeaderDeclaration", "-" + randomUUID());
		final List<String> lines = asList("1,2,3", "Kirk,Admiral,002", "McCoy,Doctor,567");
		write(dataFileLocation, lines, UTF_8);
		log.debug("Parsing file: {}", dataFileLocation);
		final Response r = client.resetQuery().query("url", "file://" + dataFileLocation.toAbsolutePath())
				.query("hasHeaders", "true").get();
		assertEquals(OK.getStatusCode(), r.getStatus());
		final String text = r.readEntity(String.class);
		log.trace("Got response: {}", text);
		assertXMLEqual(properResponse2, text);
		dataFileLocation.toFile().delete();
	}
}
