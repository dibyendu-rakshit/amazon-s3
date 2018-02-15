package com.greytip.attachment;

import java.io.File;

public class Test {

    static final String ACCESS_KEY = "AKIAIGQ5V4ZCGCUNUQGQ";
    static final String ACCESS_SECRET = "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";
    static final String S3_BUCKET = "gt-dib-attachment";
    static final String ACCESS_ID = "gt_demo_masterdemo21";
    static final String REGION = "ap-south-1";

    public static void main(String[] args) {

        AttachmentService service = new AttachmentService(ACCESS_KEY,ACCESS_SECRET,REGION);

       String attachmentId = "documents/4cd65bc9-cd49-469e-a791-6cdb819edf58";
               //service.createAttachment(S3_BUCKET,ACCESS_ID,"documents","5");
       System.out.println("attachment id "+ attachmentId);
     //  service.addAttachmentFile(S3_BUCKET,ACCESS_ID,attachmentId,new File("E:\\aadhar_card.pdf"));
        service.deleteAttachment(S3_BUCKET,ACCESS_ID,attachmentId);

    }
}
