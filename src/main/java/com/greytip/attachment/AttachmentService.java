package com.greytip.attachment;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.google.gson.Gson;
import com.greytip.attachment.model.AttachmentContext;
import com.greytip.attachment.model.Manifest;
import com.greytip.attachment.model.ManifestFile;
import com.greytip.attachment.utility.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class AttachmentService {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);
    private static final long SECOND_IN_MILLIS = 1000;
    private static final String SUFFIX = "/";
    private static final String META_FILE_NAME = "MANIFEST.MF";
	private static final long EXPIRY_TIME_SECONDS = 30;
    private AmazonS3 amazonS3Client;

    /**
     * Initialize the attachment service and amazon s3 client.
     *
     * @param accessKey
     * @param accessSecretKey
     * @param region
     */
    public AttachmentService(String accessKey, String accessSecretKey, String region) {
        GtAwsS3Client gtAwsS3Client = GtAwsS3Client.GT_AWS_S3_CLIENT;
        this.amazonS3Client = gtAwsS3Client.createAndReturnAmazonS3Client(accessKey, accessSecretKey, region);
    }

    /**
     * This API will be used to create an attachment folder in s3 bucket. Before uploading a document
     * this api should be invoked.
     *
     * @param bucketName
     * @param accessId
     * @param userId
     * @param prefix     is optional
     * @return {{@link Optional<String>}}
     */
    public String createAttachment(String bucketName, String accessId, String prefix, String userId) {
        String attachmentId = UUID.randomUUID().toString();
        try {
            if (prefix != null) {
                attachmentId = String.format("%s/%s", prefix, attachmentId);
            }

            Manifest manifest = new Manifest();

            AttachmentContext context = new AttachmentContext();
            context.setAccessId(accessId);
            context.setBucket(bucketName);
            manifest.setContext(context);
            manifest.setAttachmentId(attachmentId);
            manifest.setUserId(userId);
            manifest.setStatus(AttachmentStatus.PENDING.getText());
            manifest.setCreatedDate(System.currentTimeMillis());
            saveManifest(bucketName, accessId, attachmentId, manifest);
            
            return attachmentId;
        } catch (SdkClientException e) {
            logger.error("Unable to create attachment {} ", attachmentId, e);
            throw new RuntimeException(e);
        }
    }

    
    private ObjectMetadata getFileMetadata(File file) {
        Path source = Paths.get(file.getAbsolutePath());
    	ObjectMetadata metadata = new ObjectMetadata();
        String contentType = null;
        try {
            contentType = Files.probeContentType(source);
        } catch (IOException e) {
            logger.error("error came while get the content type ");
            throw new RuntimeException(e);
        }

        metadata.setContentType(contentType);
        metadata.addUserMetadata("file-name", file.getName());
        metadata.setContentDisposition("attachment;filename='" + file.getName() + "'");
        return metadata;
    }

    /**
     * This API will be invoked while uploading a document into s3.
     *
     * @param bucketName
     * @param accessId
     * @param attachmentId
     * @param file
     * @return {{@link Optional<String>}}
     */
    public Optional<String> addAttachmentFile(String bucketName, String accessId, String attachmentId, File file) {
        String folderName = getFolderName(accessId, attachmentId);

        ObjectMetadata metadata = getFileMetadata(file);

        Manifest manifest = loadManifest(bucketName, accessId, attachmentId);
        ManifestFile manifestFile = new ManifestFile(file, metadata.getContentType());
        manifest.addFile(manifestFile);

        String filePath = folderName + SUFFIX + manifestFile.getFileId();
        try (InputStream inputStream = new FileInputStream(file)) {
        	PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                    filePath, inputStream, metadata);
            this.amazonS3Client.putObject(putObjectRequest);
            this.updateManifest(bucketName, accessId, attachmentId, manifest);
            return Optional.of(manifestFile.getFileId());
        } catch (IOException e) {
            logger.error("Error when uploading file to S3: {} ", filePath, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * If attachment folder contains several documents, this api will be used to get the
     * byte of the first document.
     *
     * @param bucketName
     * @param accessId
     * @param attachmentId
     * @return Optional<byte[]>
     */

    public Optional<byte[]> getFirstAttachmentFileBytes(String bucketName, String accessId, String attachmentId) {
        Optional<byte[]> results = Optional.empty();

        Manifest manifest = loadManifest(bucketName, accessId, attachmentId);

        if (!manifest.getFiles().isEmpty()) {
            String firstFileId = manifest.getFiles().get(0).getFileId();
            String key = String.format("%s/%s/%s", accessId, attachmentId, firstFileId);
            S3Object s3Object = this.amazonS3Client.getObject(new GetObjectRequest(bucketName, key));

            try {
                results = Optional.ofNullable(FileUtility.getFileContentAsByte(s3Object.getObjectContent()));
            } catch (IOException e) {
                logger.error("Unable to read contents of file path:  {}", key, e);
            }
        }
        
        return results;
    }

    /**
     * Delete the attachment folder or file.
     *
     * @param bucketName
     * @param accessId
     * @param attachmentId
     */
    public void deleteAttachment(String bucketName, String accessId, String attachmentId) {
    	deleteS3Object(bucketName, accessId, attachmentId);
    }
    
    private void deleteS3Object(String bucketName, String accessId, String path) {
        String deletedKey = getFolderName(accessId, path);
        logger.debug("going to delete file {} ", deletedKey);

        final ObjectListing items = this.amazonS3Client.listObjects(bucketName, deletedKey);

        if (items == null) {
            logger.warn("Cannot find any file with file key = {}", deletedKey);
            return;
        }
        items.getObjectSummaries().forEach(s3ObjectSummary ->
                this.amazonS3Client.deleteObject(bucketName, s3ObjectSummary.getKey()));
    }

    /**
     * This API is invoked to delete a particular file from a attachment folder.
     *
     * @param bucketName
     * @param accessId
     * @param attachmentId
     * @param fileId
     */
    public void deleteAttachmentFile(String bucketName, String accessId, String attachmentId, String fileId) {

        String folderName = getFolderName(accessId, attachmentId);

        String deletedKey = folderName;

        if (fileId != null) {
            deletedKey += SUFFIX + fileId;
        }

        logger.debug("going to delete file {} ", deletedKey);
        final ObjectListing objectListing = this.amazonS3Client.listObjects(bucketName, deletedKey);

        if (objectListing == null) {
            logger.warn("can not find any file with file key = {}", deletedKey);
            return;
        }

        objectListing.getObjectSummaries().forEach(s3ObjectSummary ->
                this.amazonS3Client.deleteObject(bucketName, s3ObjectSummary.getKey()));

        if (fileId != null) {
            Manifest manifest = loadManifest(bucketName, accessId, attachmentId);

            ManifestFile deletedFile = new ManifestFile();
            deletedFile.setFileId(fileId);

            manifest.removeFile(deletedFile);
            this.updateManifest(bucketName, accessId, attachmentId, manifest);
        }
    }

    public Optional<String> getFirstAttachmentFileUrl(String bucketName, String accessId, String attachmentId, long validFor) {
        Manifest manifest = loadManifest(bucketName, accessId, attachmentId);
        if (manifest.getFiles() != null && !manifest.getFiles().isEmpty()) {
            String firstFileId = manifest.getFiles().get(0).getFileId();
            return Optional.of(getAttachmentFileUrl(bucketName, accessId, attachmentId, firstFileId, validFor));
        }
        return Optional.empty();
    }

    public String getAttachmentFileUrl(String bucketName, String accessId, String attachmentId, String fileKey) {
        return this.getAttachmentFileUrl(bucketName, accessId, attachmentId, fileKey, EXPIRY_TIME_SECONDS);
    }

    public String getAttachmentFileUrl(String bucketName, String accessId, String attachmentId, String fileKey, long validFor) {
        String url = null;

        String folderName = getFolderName(accessId, attachmentId);
        String downloadedFile = String.format("%s/%s", folderName, fileKey);

        logger.debug("going to download file {} ", downloadedFile);

        long t = System.currentTimeMillis();

        Date validTime = new Date(t + (validFor * SECOND_IN_MILLIS));

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, downloadedFile);
        request.withExpiration(validTime);
        url = this.amazonS3Client.generatePresignedUrl(request).toString();
        return url;
    }

    private void saveManifest(String bucketName, String accessId, String attachmentId, Manifest manifest) {
        ObjectMetadata metadata = new ObjectMetadata();

        String jsonStr = new Gson().toJson(manifest);
        logger.debug("meta data = {} ", jsonStr);

        String folderName = getFolderName(accessId, attachmentId);
        logger.debug("folderName = {} ", folderName);

        // create content
        InputStream content = new ByteArrayInputStream(jsonStr.getBytes());
        // create a PutObjectRequest passing the folder name suffixed by /
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                folderName + SUFFIX + META_FILE_NAME, content, metadata);

        // send request to S3 to create folder
        this.amazonS3Client.putObject(putObjectRequest);
    }

    private String getFolderName(String accessId, String attachmentId) {
        return String.format("%s/%s", accessId, attachmentId);
    }

    private Manifest loadManifest(String bucketName, String accessId, String attachmentId) {
        Manifest menifest = new Manifest();
        try {
            String folderName = getFolderName(accessId, attachmentId);
            S3Object s3object = this.amazonS3Client.getObject(bucketName, folderName + SUFFIX + META_FILE_NAME);
            final S3ObjectInputStream objectContent = s3object.getObjectContent();
            String metaData = FileUtility.getFileContentAsString(objectContent);
            logger.debug("loaded meta data = {} ", metaData);
            Gson gson = new Gson();
            menifest = gson.fromJson(metaData, Manifest.class);

        } catch (IOException e) {
            logger.error("could not able to load meta data {} ", e);
        }
        return menifest;
    }

    /**
     * This API will be called whenever any meta data will be changed (e.g., add a file or delete
     * a file ) in attachment folder.
     *
     * @param bucketName
     * @param accessId
     * @param attachmentId
     * @param manifest
     */
    private void updateManifest(String bucketName, String accessId, String attachmentId, Manifest manifest) {
        manifest.setUpdatedDate(System.currentTimeMillis());
        deleteManifest(bucketName, accessId, attachmentId);
        saveManifest(bucketName, accessId, attachmentId, manifest);
    }
    
	private void deleteManifest(String bucketName, String accessId, String attachmentId) {
		String fileKey = String.format("%s/%s", attachmentId, META_FILE_NAME);
        this.deleteS3Object(bucketName, accessId, fileKey);
	}

    /**
     * Updates the status of attachment as consumed
     *
     * @param bucketName
     * @param accessId
     * @param attachmentId
     */
    public void consume(String bucketName, String accessId, String attachmentId) {
        Manifest manifest = loadManifest(bucketName, accessId, attachmentId);
        manifest.setStatus(AttachmentStatus.CONSUMED.getText());
        this.updateManifest(bucketName, accessId, attachmentId, manifest);
    }
}
