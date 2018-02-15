package com.greytip.attachment;

public enum AttachmentStatus {
    PENDING("pending"),
    CONSUMED("consumed");


    AttachmentStatus(String text) {
        this.text = text;
    }

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

