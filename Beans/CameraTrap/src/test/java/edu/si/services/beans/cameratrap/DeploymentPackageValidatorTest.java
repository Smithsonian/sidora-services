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

package edu.si.services.beans.cameratrap;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Unit testing for the DeploymentPackageValidator class.
 *
 * @author parkjohn
 */
public class DeploymentPackageValidatorTest extends CamelTestSupport {

    /**
     * Used to validate methods that throw expected exceptions
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private final DeploymentPackageValidator validator = new DeploymentPackageValidator();

    /**
     * Testing illegal argument exception when null passed in for the manifest resource count value, which is required by the validator
     *
     * @throws IOException
     */
    @Test
    public void testValidatResourceCountIllegalArgument() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("count not found");

        validator.validateResourceCount("/opt/sidora/smx/CameraTrapData/path...","jpg,jpeg",null);
    }

    /**
     * Testing illegal argument exception when null passed in for the manifest absolute path, which is required by the validator
     *
     * @throws IOException
     */
    @Test
    public void testValidateAbsolutePathIllegalArgument() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("absolute path");

        validator.validateResourceCount("","jpg,jpeg",23);
    }

    /**
     * Testing illegal argument exception when path to the deployment package manifest doesn't exist
     *
     * @throws IOException
     */
    @Test
    public void testValidateAbsolutePathNotFound() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("path not found");

        validator.validateResourceCount("/opt/sidora/smx/CameraTrapData/00000000/manifest.xml","jpg,jpeg",2);
    }

    /**
     * Test validator when the deployment package contains more resources than the count found from the manifest.
     * The test deployment package set up for this test can be found under the resources directory
     *
     * @throws IOException
     */
    @Test
    public void testValidateExtraResourceFilesFound() throws IOException {
        int result = validator.validateResourceCount("src/test/resources/extraImagesDeploymentPkg/deployment_manifest.xml","jpg,jpeg",2);
        assertEquals(result, 0);
    }

    /**
     * Test validator when the deployment package is contains less resource counts than the the manifest's resource count.
     * The test deployment package set up for this test can be found under the resources directory
     *
     * @throws IOException
     */
    @Test
    public void testValidateResourceFilesMissing() throws IOException {
        int result = validator.validateResourceCount("src/test/resources/missingImagesDeploymentPkg/deployment_manifest.xml","jpg,jpeg",2);
        assertEquals(result, -1);
    }

    /**
     * Test validator when the deployment package contains the same number of resource files as the manifest file.
     *
     * @throws IOException
     */
    @Test
    public void testValidDeploymentPackage() throws IOException {
        int result = validator.validateResourceCount("src/test/resources/validDeploymentPkg/deployment_manifest.xml","jpg,jpeg",2);
        assertEquals(result, 1);
    }
}
