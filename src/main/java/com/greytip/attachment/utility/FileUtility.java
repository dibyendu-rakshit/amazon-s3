package com.greytip.attachment.utility;

import org.apache.commons.io.IOUtils;

import java.io.*;

public class FileUtility {

    public static String getFileContentAsString(final InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream);
    }

    public static byte[] getFileContentAsByte(final InputStream inputStream) throws IOException {
       return IOUtils.toByteArray(inputStream);
    }
}
