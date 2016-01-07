/*
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

package edu.si.services.beans.cameratrap;

import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The CameraTrap deployment package validator to verify whether the resource counts are matching between
 * the deployment_manifest.xml and the actual resource files included in the tar.gz.  This validator will check even
 * when there are extra resource files are included in the deployment package than what's described in the manifest.
 *
 * @author parkjohn
 */
public class DeploymentPackageValidator {
    private static final Logger log = LoggerFactory.getLogger(DeploymentPackageValidator.class);

    /**
     * fsResourceCount() is a private method that counts the number of files available
     * given path for the deployment package and the referenced file types.
     *
     * @param manifestPath The absolute path to the deployment package's manifest file.
     *                     This path is used to find the root directory of the deployment package.
     * @param resourceFileType The resource file type for the validator to count the number of files for.  This is a comma separated
     *                          values (i.e. jpg, jpeg)
     * @return Returns the count of the referenced files (i.e. number of image files found)
     * @throws IOException FileNotFoundException thrown when the deployment root directory doesn't exists
     */
    private int fsResourceCount(String manifestPath, String resourceFileType) throws IOException {

        //DirectoryStream filter to only count the files that are relevant (i.e. image files).
        DirectoryStream.Filter<Path> resourceFilter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                Path filename = entry.getFileName();
                boolean isHiddenFile = entry.toFile().isHidden();
                boolean isResourceFile = false;

                String[] fileTypes = resourceFileType.split(",");
                // checking if the current filename contains the acceptable resource file extension
                for (String fileType : fileTypes){
                    isResourceFile = filename.toString().toLowerCase().contains(fileType.toLowerCase().trim());
                    if (isResourceFile){
                        break;
                    }
                }

                log.trace("The boolean value for isHiddenFile is: " + isHiddenFile + " and boolean value for isResourceFile is: " + isResourceFile);

                //only include files that are not hidden and accepted file extensions for the resource files
                return !isHiddenFile && isResourceFile;
            }
        };

        int totalResourceFound = 0;

        //this is the root directory of the deployment package to process where the resource files will be located on the file system
        final Path deploymentPkgDir = Paths.get(manifestPath).getParent();
        if (Files.exists(deploymentPkgDir)) {
            //open the directory stream and apply the resource filter to list only the number of files that we want to count
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(deploymentPkgDir, resourceFilter)) {
                for (Path path : directoryStream) {
                    log.trace("Listing filename: " + path.toString());
                    totalResourceFound++;
                    log.debug("The resource file counter incremented: " + totalResourceFound);
                }

            } catch (IOException ex) {
                log.error("Exception occurred while processing directoryStream in fsDeploymentResourceCount(): ", ex);
            }
        } else {
            throw new FileNotFoundException("The deployment package directory path not found: " + deploymentPkgDir.toAbsolutePath().toString());
        }

        return totalResourceFound;
    }

    /**
     * validateResourceCount() is the entry point for the Camera Trap route to invoke to validate the referenced object
     * counts between the manifest xml and the actual files available in the deployment package.  The method throws IllegalArgumentException if
     * invalid parameters are passed in.
     *
     * @param manifestPath (required) The absolute path to the deployment package's manifest file.
     *                     This value is passed in from the message header.  It has to be a non-null/non-empty String value.
     * @param resourceFileType (required) The resource file type for the validator to count the number of files for.  This is a comma separated
     *                          values (i.e. jpg, jpeg) This value is passed in from the message header and has to be a non-null/non-empty String value.
     * @param manifestResourceCount (required) The total resource count value from the deployment package's manifest file. (i.e. image file count)
     *                           This value is passed in from the message header.  It has to be a non-null Integer value.
     * @return returns integer value 0 for extra images found in the deployment package; -1 for less images found;
     *          1 for exact count match between the deployment package and the manifest file.
     * @throws IOException
     * @throws IllegalArgumentException when invalid arguments passed in (i.e null passed in for a required field)
     */
    public int validateResourceCount(@Header(value="CamelFileAbsolutePath") String manifestPath, @Header(value="ResourceFileType") String resourceFileType,
                                        @Header(value="ResourceCount") Integer manifestResourceCount) throws IOException {

        log.debug("CamelFileAbsolutePath for the currently deployment package is: " +manifestPath);
        log.debug("The manifest resource count value is: " + manifestResourceCount);
        log.debug("The resource file type value is: " + resourceFileType);

        //flag for holding the result of the validation check
        int returnCode;

        //check if manifest resource count was properly passed in
        if (manifestResourceCount == null){
            throw new IllegalArgumentException("The manifest resource count not found");
        }
        //check if absolute path to the deployment package manifest was properly passed in
        if (manifestPath == null || manifestPath.length()==0){
            throw new IllegalArgumentException("The camel absolute path for manifest file argument error");
        }
        //check if resource file type was properly passed in
        if (resourceFileType == null || resourceFileType.length()==0){
            throw new IllegalArgumentException("The resource file type not found");
        }

        //run the file system resource counts from the deployment package
        int fsResourceCount = fsResourceCount(manifestPath, resourceFileType);

        Path deploymentPkgDir = Paths.get(manifestPath).getParent();
        String deploymentPkg = deploymentPkgDir.getFileName().toString();

        if (fsResourceCount == manifestResourceCount){
            log.info("The resource counts match in deployment: " + deploymentPkg + " the count is: " + fsResourceCount);
            returnCode = 1;
        } else if (fsResourceCount > manifestResourceCount) {
            log.error("Extra resource(s) found in deployment: " + deploymentPkg + " the manifest count - " + manifestResourceCount
                    + " and the fs count - " + fsResourceCount);
            returnCode = 0;
        } else {
            log.error("Less resource(s) found in deployment: " + deploymentPkg + " the manifest count - " + manifestResourceCount
                    + " and the fs count - " + fsResourceCount);
            returnCode = -1;
        }
        return returnCode;
    }
}