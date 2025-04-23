package org.iit.demo1.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class FileDownloadRequest {
    @NotEmpty(message = "Case ID is required")
    private String caseId;

    @NotEmpty(message = "File name is required")
    private String fileName;


}