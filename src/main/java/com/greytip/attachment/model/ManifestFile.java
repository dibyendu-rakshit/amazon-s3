package com.greytip.attachment.model;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public class ManifestFile {
    private String fileId;
    private String contentType;
    private String fileName;
    private long contentLength;

    public ManifestFile(File file, String contentType) {
        setFileId(UUID.randomUUID().toString());
        setFileName(file.getName());
        setContentType(contentType);
        setContentLength(file.length());
	}

	public ManifestFile() {
	}

	public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!ManifestFile.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final ManifestFile file = (ManifestFile) obj;
        return Objects.equals(fileId, file.getFileId());
    }
}
