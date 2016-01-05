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
 * The CameraTrap deployment package validator to verify whether the image counts are matching between
 * the deployment_manifest.xml and the actual image files included in the tar.gz.  This validator will check even
 * when there are extra image files are included in the deployment package than what's described in the manifest.
 *
 * @author parkjohn
 * @version 1.0
 */
public class DeploymentPackageValidator {
    private static final Logger log = LoggerFactory.getLogger(DeploymentPackageValidator.class);

    private int fsDeploymentImageCount(String absolutePath) throws IOException {

        //DirectoryStream filter to only count the files that are relevent (image files).
        DirectoryStream.Filter<Path> imageFilter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                Path filename = entry.getFileName();
                boolean isHiddenFile = entry.toFile().isHidden();
                //accepting jpg or jpeg extensions
                boolean isImageFile = filename.toString().toLowerCase().contains("jpg") || filename.toString().toLowerCase().contains("jpeg");

                log.debug("The boolean value for isHiddenFile is: " + isHiddenFile + " and boolean value forr isImageFile is: " + isImageFile);

                //only include files that are not hidden and accepted file extensions for image files
                return !isHiddenFile && isImageFile;
            }
        };

        int totalImageFound = 0;

        //this is the root directory of the deployment package to process where the image files will be located on the file system
        final Path deploymentPkgDir = Paths.get(absolutePath).getParent();
        if (Files.exists(deploymentPkgDir)) {
            //open the directory stream and apply the image filter to list only the number of files that we want to count
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(deploymentPkgDir, imageFilter)) {
                for (Path path : directoryStream) {
                    log.trace("Listing filename: " + path.toString());
                    totalImageFound++;
                    log.debug("The image files counter incremented: " + totalImageFound);
                }

            } catch (IOException ex) {
                log.error("Exception occured while processing directoryStream in fsDeploymentImageCount(): ", ex);
            }
        } else {
            throw new FileNotFoundException("The deployment package directory path not found: " + deploymentPkgDir.toAbsolutePath().toString());
        }

        return totalImageFound;
    }

    public void validateImageCount(@Header(value="CamelFileAbsolutePath") String manifestPath,
                                        @Header(value="ImageCount") Integer manifestImageCount) throws IOException {
        log.debug("CamelFileAbsolutePath for the currently deployment package is: " +manifestPath);

        //check if manifest image count was properly passed in
        if (manifestImageCount == null){
            log.debug("The manifest image count is: " + manifestImageCount);
            throw new IllegalArgumentException("The manifest image count not found");
        }
        //check if absolute path to the deployment package manifest was properly passed in
        if (manifestPath == null || manifestPath.length()==0){
            throw new IllegalArgumentException("The camel absolute path for manifest file argument error");
        }

        //run the file system image counts from the deployment package
        int fsImageCount = fsDeploymentImageCount(manifestPath);

        if (fsImageCount == manifestImageCount){
            log.info("The manifest image count and file system image count matches - count: " + fsImageCount);
        } else {
            throw new FileNotFoundException("The image count from manifest and file system do not match - flagging this deployment package for review");
        }
    }
}