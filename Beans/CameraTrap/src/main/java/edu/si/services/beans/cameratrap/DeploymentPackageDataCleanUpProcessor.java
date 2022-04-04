/*
 * Copyright 2019-2020 Smithsonian Institution.
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

package edu.si.services.beans.cameratrap;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

/**
 * @author jbirkhimer
 */
public class DeploymentPackageDataCleanUpProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(DeploymentPackageDataCleanUpProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Message out = exchange.getIn();
        String deploymentDataDir = out.getHeader("deploymentDataDir", String.class);

        if (deploymentDataDir == null) {
            deploymentDataDir = exchange.getContext().resolvePropertyPlaceholders("{{si.ct.uscbi.data.dir.path}}") + "/" + FilenameUtils.getBaseName(out.getHeader("CamelFileAbsolutePath", String.class));
        }

        if (Files.exists(new File(deploymentDataDir).toPath())) {
            log.info("CLEANING UP DEPLOYMENT DATA DIR = {}", deploymentDataDir);
            FileUtils.deleteDirectory(new File(deploymentDataDir));
        } else {
            log.error("Camera Trap Deployment Data Dir '{}' does not exist", deploymentDataDir);
        }
    }
}
