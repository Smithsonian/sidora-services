/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
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
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.sidora.tabularmetadata.web.integration;

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
