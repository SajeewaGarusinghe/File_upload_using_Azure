package org.iit.demo1.dto;

import lombok.Data;

@Data
public class FileDownloadResponse {
    private String presignedUrl;
    private String fileName;
    private String contentType;
    private long expiryTimeInSeconds;



    public void setExpiryTimeInSeconds(long expiryTimeInSeconds) {
        this.expiryTimeInSeconds = expiryTimeInSeconds;
    }
}