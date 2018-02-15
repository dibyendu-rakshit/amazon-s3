package com.greytip.attachment.model;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Manifest {

    private String attachmentId;
    private AttachmentContext context;
    private String userId;
    private long createdDate;
    private long updatedDate;
    private String status;
    private String businessKey;
    private List<ManifestFile> files;

    public Manifest() {
        this.files = new LinkedList<>();
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ManifestFile> getFiles() {
        return files;
    }

    public void setFiles(List<ManifestFile> files) {
        this.files = files;
    }

    public AttachmentContext getContext() {
        return context;
    }

    public void setContext(AttachmentContext context) {
        this.context = context;
    }

    public void addFile(ManifestFile file) {
        this.files.add(file);
    }

    public void removeFile(ManifestFile file) {
        this.files.remove(file);
    }

    public long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(long updatedDate) {
        this.updatedDate = updatedDate;
    }
}
