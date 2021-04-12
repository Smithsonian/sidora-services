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

package edu.si.sidora.tabular.metadata.excel2tabular.integration;

import static edu.si.sidora.tabular.metadata.excel2tabular.integration.IntegrationTestUtilities.compareLines;
import static edu.si.sidora.tabular.metadata.excel2tabular.integration.IntegrationTestUtilities.readLines;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import edu.si.sidora.tabular.metadata.excel2tabular.ExcelToTabular;

public class SIdoraData1IT {

    private final ExcelToTabular testExcel2Csv = new ExcelToTabular();

    @Test
    public void test() throws IOException {
        final URL inputUrl = new File("src/test/resources/excel2tabular/xls/cjd-master-op.3-huesos.xls").toURI().toURL();
        final URL result = testExcel2Csv.process(inputUrl).get(0).toURI().toURL();
        final URL checkFile = new File("src/test/resources/excel2tabular/tabular/cjd-master-op.3-huesos.csv").toURI().toURL();
        final List<String> resultLines = readLines(result);
        final List<String> checkLines = readLines(checkFile);
        assertEquals(checkLines.size(), resultLines.size());
        compareLines(checkLines, resultLines);
    }
}
