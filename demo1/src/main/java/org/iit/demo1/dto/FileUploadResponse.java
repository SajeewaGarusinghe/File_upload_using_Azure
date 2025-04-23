package org.iit.demo1.dto;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String caseId;
    private String fileName;
    private String originalFileName;
    private long fileSize;
    private String status;


}