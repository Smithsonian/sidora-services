package edu.si.services.beans.cameratrap;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by parkjohn on 1/5/16.
 */
public class DeploymentPackageValidatorTest extends CamelTestSupport {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private DeploymentPackageValidator validator = new DeploymentPackageValidator();

    @Test
    public void testValidateImageCountIllegalArgument() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("count not found");

        validator.validateImageCount("/opt/sidora/smx/somethinghere...",null);
    }

    @Test
    public void testValidateAbsolutePathIllegalArgument() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("absolute path");

        validator.validateImageCount("",23);
    }

    @Test
    public void testValidateAbsolutePathNotFound() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("path not found");

        validator.validateImageCount("/opt/sidora/smx/CameraTrapData/00000000/manifest.xml",2);
    }

    @Test
    public void testValidateExtraImageFilesFound() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("do not match");
        validator.validateImageCount("src/test/resources/extraImagesDeploymentPkg/deployment_manifest.xml",2);
    }

    @Test
    public void testValidateImageFilesMissing() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("do not match");
        validator.validateImageCount("src/test/resources/missingImagesDeploymentPkg/deployment_manifest.xml",2);
    }

    @Test
    public void testValidateCorrectImageFilesFound() throws IOException {
        validator.validateImageCount("src/test/resources/validDeploymentPkg/deployment_manifest.xml",2);
    }
}
