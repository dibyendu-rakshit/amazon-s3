package com.greytip.attachment;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public enum GtAwsS3Client {
    GT_AWS_S3_CLIENT;


    AmazonS3 createAndReturnAmazonS3Client(String accessKey, String accessSecretKey, String region) {
        AmazonS3 amazonS3Client = null;
        if (accessKey != null && accessSecretKey != null) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, accessSecretKey);
            amazonS3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
        }
        return amazonS3Client;
    }


}
