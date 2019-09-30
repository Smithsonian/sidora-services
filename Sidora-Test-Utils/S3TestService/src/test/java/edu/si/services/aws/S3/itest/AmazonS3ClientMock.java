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

package edu.si.services.S3.itest;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AmazonS3ClientMock for testing aws-s3 endpoints.
 * Modified from https://github.com/apache/camel/blob/master/components/camel-aws/src/test/java/org/apache/camel/component/aws/s3/AmazonS3ClientMock.java
 *
 * @author jbirkhimer
 */
public class AmazonS3ClientMock extends AmazonS3Client {

    private static final Logger log = LoggerFactory.getLogger(AmazonS3ClientMock.class);

    List<S3Object> objects = new CopyOnWriteArrayList<S3Object>();
    List<Bucket> buckets = new CopyOnWriteArrayList<Bucket>();
    List<PutObjectRequest> putObjectRequests = new CopyOnWriteArrayList<PutObjectRequest>();

    private boolean nonExistingBucketCreated;

    private int maxCapacity = 100;

    public AmazonS3ClientMock(String accessKey, String secretKey) {
        super(new BasicAWSCredentials(accessKey, secretKey));
    }

    @Override
    public VersionListing listNextBatchOfVersions(VersionListing previousVersionListing) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix, String keyMarker, String versionIdMarker, String delimiter, Integer maxKeys)
            throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionListing listVersions(ListVersionsRequest listVersionsRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectListing listObjects(String bucketName) throws AmazonClientException, AmazonServiceException {
        //throw new UnsupportedOperationException();
        return listObjects(new ListObjectsRequest(bucketName, null, null, null, null));
    }

    @Override
    public ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws AmazonClientException, AmazonServiceException {
        if ("nonExistingBucket".equals(listObjectsRequest.getBucketName()) && !nonExistingBucketCreated) {
            AmazonServiceException ex = new AmazonServiceException("Unknown bucket");
            ex.setStatusCode(404);
            throw ex;
        }
        ObjectListing objectListing = new ObjectListing();
        for (int index = 0; index < objects.size(); index++) {
            if (objects.get(index).getBucketName().equals(listObjectsRequest.getBucketName())) {
                S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
                s3ObjectSummary.setBucketName(objects.get(index).getBucketName());
                s3ObjectSummary.setKey(objects.get(index).getKey());

                objectListing.getObjectSummaries().add(s3ObjectSummary);
            }
        }

        return objectListing;
    }

    @Override
    public ObjectListing listNextBatchOfObjects(ObjectListing previousObjectListing) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Owner getS3AccountOwner() throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Bucket> listBuckets() throws AmazonClientException, AmazonServiceException {
        return buckets;
    }

    @Override
    public String getBucketLocation(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bucket createBucket(String bucketName) throws AmazonClientException, AmazonServiceException {
        //throw new UnsupportedOperationException();
        return createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public Bucket createBucket(String bucketName, Region region) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bucket createBucket(String bucketName, String region) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bucket createBucket(CreateBucketRequest createBucketRequest) throws AmazonClientException, AmazonServiceException {
        if ("nonExistingBucket".equals(createBucketRequest.getBucketName())) {
            nonExistingBucketCreated = true;
        }

        Bucket bucket = new Bucket();
        bucket.setName(createBucketRequest.getBucketName());
        bucket.setCreationDate(new Date());
        bucket.setOwner(new Owner("c2efc7302b9011ba9a78a92ac5fd1cd47b61790499ab5ddf5a37c31f0638a8fc ", "Christian Mueller"));
        buckets.add(bucket);
        return bucket;
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key, String versionId) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectAcl(String bucketName, String key, AccessControlList acl) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectAcl(String bucketName, String key, String versionId, AccessControlList acl) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectAcl(String bucketName, String key, String versionId, CannedAccessControlList acl) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAccessControlList acl) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest getObjectMetadataRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }


    @Override
    public S3Object getObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
        for (S3Object s3Object : objects) {
            if (bucketName.equals(s3Object.getBucketName()) && key.equals(s3Object.getKey())) {
                return s3Object;
            }
        }

        return null;
    }

    @Override
    public boolean doesBucketExist(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeObjectStorageClass(String bucketName, String key, StorageClass newStorageClass) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public S3Object getObject(GetObjectRequest getObjectRequest) throws AmazonClientException, AmazonServiceException {
        return getObject(getObjectRequest.getBucketName(), getObjectRequest.getKey());
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteBucket(String bucketName) throws AmazonClientException, AmazonServiceException {
        // noop
    }

    @Override
    public void deleteBucket(DeleteBucketRequest deleteBucketRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("resource")
    @Override
    public PutObjectResult putObject(PutObjectRequest putObjectRequest) throws AmazonClientException, AmazonServiceException {
        putObjectRequests.add(putObjectRequest);

        S3Object s3Object = new S3Object();
        s3Object.setBucketName(putObjectRequest.getBucketName());
        s3Object.setKey(putObjectRequest.getKey());
        if (putObjectRequest.getFile() != null) {
            try {
                s3Object.setObjectContent(new FileInputStream(putObjectRequest.getFile()));
            } catch (FileNotFoundException e) {
                throw new AmazonServiceException("Cannot store the file object.", e);
            }
        } else {
            s3Object.setObjectContent(putObjectRequest.getInputStream());
        }
        objects.add(s3Object);

        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setETag("3a5c8b1ad448bca04584ecb55b836264");
        return putObjectResult;
    }

    @Override
    public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest) throws AmazonClientException, AmazonServiceException {
        CopyObjectResult copyObjectResult = new CopyObjectResult();
        copyObjectResult.setETag("3a5c8b1ad448bca04584ecb55b836264");
        copyObjectResult.setVersionId("11192828ahsh2723");
        return copyObjectResult;
    }

    @Override
    public void deleteObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
        log.debug("AmazonS3Client Object list Before Delete = {}", Arrays.toString(objects.toArray()));

        for (S3Object s3object :objects) {
            if (s3object.getKey().equals(key)) {
                objects.remove(s3object);
            }
        }
        log.debug("AmazonS3Client Object list After Delete = {}", Arrays.toString(objects.toArray()));
    }

    @Override
    public void deleteObject(DeleteObjectRequest deleteObjectRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVersion(String bucketName, String key, String versionId) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVersion(DeleteVersionRequest deleteVersionRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBucketNotificationConfiguration(String bucketName, BucketNotificationConfiguration bucketNotificationConfiguration) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BucketNotificationConfiguration getBucketNotificationConfiguration(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BucketPolicy getBucketPolicy(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText) throws AmazonClientException, AmazonServiceException {
        Assert.assertEquals("nonExistingBucket", bucketName);
        Assert.assertEquals("xxx", policyText);
    }

    @Override
    public void deleteBucketPolicy(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws AmazonClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethod method) throws AmazonClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws AmazonClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest abortMultipartUploadRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest completeMultipartUploadRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest initiateMultipartUploadRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest listMultipartUploadsRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PartListing listParts(ListPartsRequest listPartsRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public S3ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        throw new UnsupportedOperationException();
    }
}