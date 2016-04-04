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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ExtractorComponentTest extends CamelTestSupport
{

    //TODO: Add more test!!! Need to make sure that Producer finds the correct new folder if there are multiple folders initially
    @Test
    public void testTarballExtractor() throws Exception
    {
        testExtractor("p1d246-test-targz.tar.gz");
    }

    @Test
    public void testTarExtractor() throws Exception
    {
        testExtractor("p1d246-test-tar.tar");
    }

    @Test
    public void testZipExtractor() throws Exception
    {
        //testExtractor("p1d246-test-zip.zip");
        //testExtractor("p1d246-test-zip.zip", false);
        testExtractor("ECU-001-D0001.zip", false);
    }

    @Test
    public void testOverwrite() throws Exception
    {
        testExtractor("p1d246-test-targz.tar.gz", false);
        testExtractor("p1d246-test-targz.tar.gz");
    }

    public void testExtractor(String archive) throws Exception
    {
        this.testExtractor(archive, true);
    }

    public void testExtractor(String archive, boolean delete) throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(msgCount + 1);

        File file = new File(this.getClass().getResource("/" + archive).toURI());


        /**
         * TODO - Need a better test to see if the directory is there or not and handle the zip archive accordingly
         * the current zip archives only contain the deployment-manifest.xml and jpg images
         * while the other archives formats contain the deployment_manifest.xml and jpg images withing a directory
         *
         * deploymentPkg.zip
         *      - deployment_manifest.xml
         *      - image1.jpg
         *      - imageX.jpg
         *
         *  deploymentPkg.tar(.gz)
         *      - deploymentPkg_Dir
         *          - deployment_manifest.xml
         *          - image1.jpg
         *          - imageX.jpg
         *
         *  The code has been updated to correctly extract the current zip archives that we are getting without the directory
         *  However, we should have a better test to see if the directory is there or not and handle the zip archive accordingly
         *
         */

        ZipFile zipfile = new ZipFile(file);

        //zipfile.getEntry()

        int numElem = zipfile.size();

        template.sendBody("direct:start", file);

        assertMockEndpointsSatisfied();

        Message msg = mock.getExchanges().get(msgCount++).getIn();
        File body = msg.getBody(File.class);

        try
        {
            assertNotNull("Results should not be null", body);
            assertTrue("Results should be a directory", body.isDirectory());
            assertEquals("Parent directory should be 'TestCameraTrapData'", "TestCameraTrapData", body.getParentFile().getName());
            assertEquals("Directory should contain " + numElem + " elements", numElem, body.list().length);
        }
        catch (Exception ex)
        {
            //delete = true;
            throw ex;
        }
        finally
        {
            if (delete && body != null && body.isDirectory())
            {
                //FileUtils.deleteDirectory(body);
            }
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

    /*@AfterClass
    public static void afterClass()
    {
        try
        {
            FileUtils.deleteDirectory(new File("TestCameraTrapData"));
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExtractorComponentTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/

    @Override
    public void setUp() throws Exception {
        //deleteDirectory("TestCameraTrapData");
        super.setUp();
        template.sendBodyAndHeader("file://target/files", "Hello World", Exchange.FILE_NAME, "ECU-001-D0001.zip");
        //template.sendBodyAndHeader("file://target/files", "Bye World", Exchange.FILE_NAME, "report2.txt");
        //template.sendBodyAndHeader("file://target/files/2008", "2008 Report", Exchange.FILE_NAME, "report2008.txt");
    }
}
