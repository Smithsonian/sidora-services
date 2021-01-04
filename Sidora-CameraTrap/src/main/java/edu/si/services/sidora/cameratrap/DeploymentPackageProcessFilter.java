/*
 * Copyright 2018-2019 Smithsonian Institution.
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

package edu.si.services.sidora.cameratrap;

import org.apache.camel.PropertyInject;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * This filter is being used with the file component in camel routes and will determine whether
 * current file in the message exchange should be excluded from being processed by camel routes.
 *
 * @author parkjohn
 */
public class DeploymentPackageProcessFilter<T> implements GenericFileFilter<T> {

    private static final Logger log = LoggerFactory.getLogger(DeploymentPackageProcessFilter.class);

    @PropertyInject(value = "si.ct.uscbi.process.dir.path")
    private String processDirPath;

    @PropertyInject(value = "si.ct.uscbi.process.dir.threshold")
    private Long processDirThreshold;

    /**
     * We are checking to see if the destination directory contains
     * more than it's threshold set in the configuration file before allowing it to be processed.
     *
     * @param file file being processed by camel from the file component
     * @return boolean to indicate whether to filter the file being processed by camel route
     */
    public boolean accept(GenericFile<T> file) {

        log.debug("DeploymentPackageProcessFilter processDirPath: {}", Paths.get(processDirPath).toAbsolutePath());

        boolean isAcceptable = false;
        long directoryFileCount = 0;

        if (processDirPath == null){
            throw new IllegalArgumentException("A path to the destination directory is required for filter to function");
        }
        if (processDirThreshold == null){
            throw new IllegalArgumentException("The directory file count threshold is required for filter to function");
        }

        try (Stream<Path> files = Files.list(Paths.get(processDirPath).toAbsolutePath())) {
            directoryFileCount = files.count();

            if (directoryFileCount < processDirThreshold){
                isAcceptable = true;
            }
        } catch (IOException ex) {
            log.error("Exception occurred while trying to filter file processing: ", ex);
        }

        log.debug("File: " + file.getFileName() + " -- Permitted to move? " + isAcceptable);

        return isAcceptable;
    }
}

