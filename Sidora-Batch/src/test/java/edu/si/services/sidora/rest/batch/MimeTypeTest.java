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

package edu.si.services.sidora.rest.batch;

import com.j256.simplemagic.ContentInfoUtil;
import edu.si.services.sidora.rest.batch.beans.BatchRequestControllerBean;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.junit.Test;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author jbirkhimer
 */
public class MimeTypeTest {

    private String format = "|%1$-20s|%2$-30s|%3$-30s|%4$-30s|%5$-30s|%6$-30s|\n";

    @Test
    public void mimeTypeTest() throws URISyntaxException {
        String string = "test-data/mimetype-test-files/Canon5DMkII_50mm_100_f4_001.dng";

        URI path2 = new URI(string);

        System.out.println(FilenameUtils.getName(path2.getPath()) + " || MIME=" + new Tika().detect(path2.getPath()));

    }

    @Test
    public void mimeTypeDetectionComparison() throws IOException {
        File path = new File("src/test/resources/test-data/mimetype-test-files");

        String[] row = new String[] {"File Name", "ProbeContentType", "MimetypesFileTypeMap", "MimeUtil", "SimpleMagic", "Tika"};
        printRow(row);
        String line = "------------------------------";
        row = new String[]{"--------------------", line, line, line, line, line};
        printRow(row);

        File [] files = path.listFiles();

        for (int i = 0; i < files.length; i++){
            if (files[i].isFile()){ //this line weeds out other directories/folders
                row[0] = files[i].getName().toString();
                row[1] = Files.probeContentType(files[i].toPath());
                row[2] = String.valueOf(new MimetypesFileTypeMap().getContentType(files[i]));
                row[3] = String.valueOf(MimeUtil.getMimeTypes(files[i]));
                if (!files[i].getName().contains("mkv") && !files[i].getName().contains("csv")) {
                    ContentInfoUtil util = new ContentInfoUtil();
                    row[4] = util.findMatch(files[i]).getMimeType();
                }
                row[5] = new Tika().detect(files[i]);

                printRow(row);
            }
        }
    }

    public void printRow(String[] row) {
        System.out.format(format, row[0], row[1], row[2], row[3], row[4], row[5]);
    }

}
