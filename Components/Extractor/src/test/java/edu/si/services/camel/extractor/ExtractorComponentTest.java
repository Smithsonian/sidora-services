/*
 * Copyright 2015-2016 Smithsonian Institution.
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

package edu.si.services.camel.extractor;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.rauschig.jarchivelib.ArchiveEntry;
import org.rauschig.jarchivelib.ArchiveStream;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


/**
 * @author jshingler
 * @author jbirkhimer
 */
public class ExtractorComponentTest extends CamelTestSupport
{
    private static final Logger LOG = LoggerFactory.getLogger(ExtractorComponentTest.class);
    private StringBuilder expectedExceptionMsg;

    //TODO: Add more test!!! Need to make sure that Producer finds the correct new folder if there are multiple folders initially
    //currently we assume that an archive contains one or no compressed directory.
    @Test
    public void testTarballExtractor() throws Exception
    {
        testExtractor("test-targz-with-directory.tar.gz");
    }

    @Test
    public void testTarExtractor() throws Exception
    {
        testExtractor("test-tar-with-directory.tar");
    }

    @Test
    public void testZipExtractor() throws Exception
    {
        testExtractor("test-zip-without-directory.zip");
    }

    @Test
    public void testZipOverwriteWithoutDirectory() throws Exception
    {
        //test zip without compressed directory
        testExtractor("test-zip-without-directory.zip", false);
        testExtractor("test-zip-without-directory.zip");
    }

    @Test
    public void testOverwrite() throws Exception
    {
        testExtractor("test-targz-with-directory.tar.gz", false);
        testExtractor("test-targz-with-directory.tar.gz");
    }

    @Test
    public void testWithDirException() throws Exception
    {
        //zip with compressed directory
        testExtractor("test-zip-with-directory.zip");
    }

    @Test
    public void testFileExtException() throws Exception {
        testExtractor("test-no-file-ext");
    }

    public void testExtractor(String archive) throws Exception
    {
        this.testExtractor(archive, true);
    }

    public void testExtractor(String archive, boolean delete) throws Exception {
        File file = new File(this.getClass().getResource("/" + archive).toURI());

        boolean isNoFileExtTest = file.getName().contains(".") ? false : true;
        LOG.debug("Testing For File Ext: {}", isNoFileExtTest);

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(msgCount + 1);

        File body = null;
        try {
            template.sendBody("direct:start", file);

            Message msg = mock.getExchanges().get(msgCount++).getIn();
            body = msg.getBody(File.class);

            int numFiles = countFilesInArchive(file);

            assertNotNull("Results should not be null", body);
            assertTrue("Results should be a directory", body.isDirectory());
            assertEquals("Parent directory should be 'TestCameraTrapData'", "TestCameraTrapData", body.getParentFile().getName());
            assertEquals("Directory should contain " + numFiles + " elements", numFiles, body.list().length);

            LOG.debug("Result body is not null: {}", (body != null));
            LOG.debug("Result body should be a directory: {}", body.isDirectory());
            LOG.debug("Result body parent File is: {}", body.getParentFile().getName());
            LOG.debug("Num of files in archive: {}, Num of files in result body: {}", numFiles, body.list().length);
        } catch (CamelExecutionException e) {
            assertTrue("Exception should be ExtractorException", e.getExchange().getException() instanceof ExtractorException);

            expectedExceptionMsg = new StringBuilder();

            if (isNoFileExtTest) {
                expectedExceptionMsg.append("Improperly formatted file. No, file extension found for " + file.getName());
            } else {
                expectedExceptionMsg.append("Extracting archive '" + file.getName() + "' failed! ");
                expectedExceptionMsg.append("Directory '" + getDirName(file) + "' found in archive!");
            }

            assertEquals("Exception messages are equal", expectedExceptionMsg.toString(), e.getExchange().getException().getMessage());

            mock.setMinimumExpectedMessageCount(msgCount);
            LOG.debug("Testing Exchange Failed! Exception thrown was: '{}'", e.getExchange().getException().toString());
        } catch (Exception ex) {
            delete = true;
            throw ex;
        } finally {
            if (delete && body != null && body.isDirectory()) {
                FileUtils.deleteDirectory(body);
            }
        }

        assertMockEndpointsSatisfied();
    }

    private int countFilesInArchive(File archiveTestFile) {
        int numFiles = 0;

        Archiver archiver = ArchiverFactory.createArchiver(archiveTestFile);
        LOG.debug("Testing Archive type: {}", archiver.getFilenameExtension());

        try (ArchiveStream archiveStream = archiver.stream(archiveTestFile)) {
            ArchiveEntry entry;

            //Cont the number of files filtering out directories
            while ((entry = archiveStream.getNextEntry()) != null) {
                //don't count directories
                if (!entry.isDirectory()) {
                    numFiles++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numFiles;
    }

    public String getDirName(File archiveTestFile) {
        String dirName = null;

        Archiver archiver = ArchiverFactory.createArchiver(archiveTestFile);
        LOG.debug("Testing Archive type: {}", archiver.getFilenameExtension());

        try (ArchiveStream archiveStream = archiver.stream(archiveTestFile)) {
            ArchiveEntry entry;

            //get directory name
            while ((entry = archiveStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    dirName = entry.getName();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dirName;
    }


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from("direct:start")
                        .to("log:edu.si.ctingest?level=DEBUG&showHeaders=true")
                        .to("extractor:extract?location=TestCameraTrapData")
                        .to("log:edu.si.ctingest?level=DEBUG&showHeaders=true")
                        .to("mock:result");
            }
        };
    }

    private int msgCount = 0;

    @Before
    public void beforeTest()
    {
        msgCount = 0;
    }

    @AfterClass
    public static void afterClass()
    {
        try
        {
            FileUtils.deleteDirectory(new File("TestCameraTrapData"));
        }
        catch (IOException ex)
        {
            LOG.error(null, ex);
        }
    }
}
