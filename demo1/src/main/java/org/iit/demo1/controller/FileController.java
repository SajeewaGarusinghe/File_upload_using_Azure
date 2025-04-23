package org.iit.demo1.controller;


import jakarta.validation.Valid;
import org.iit.demo1.dto.FileDownloadRequest;
import org.iit.demo1.dto.FileDownloadResponse;
import org.iit.demo1.dto.FileUploadResponse;
import org.iit.demo1.entity.FileMetadata;
import org.iit.demo1.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileProcessingService fileProcessingService;

    @Autowired
    public FileController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping(value="/upload/{caseId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @PathVariable String caseId,
            @RequestPart("file") MultipartFile file
//            @RequestParam("file") MultipartFile file
    ) {

        try {
            // Check file size (additional check beyond Spring's multipart config)
            if (file.getSize() > 30 * 1024 * 1024) { // 30MB
                return ResponseEntity.badRequest().build();
            }


            long startTime = System.currentTimeMillis();

            // Process file upload
            FileMetadata metadata = fileProcessingService.uploadFile(caseId, file);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("File upload processing time :{} s",duration/1000);
//            System.out.println("File upload processing time: " + duration + " ms");

            // Create response
            FileUploadResponse response = new FileUploadResponse();
            response.setCaseId(metadata.getCaseId());
            response.setFileName(metadata.getFileName());
            response.setOriginalFileName(metadata.getOriginalFileName());
            response.setFileSize(metadata.getFileSize());
            response.setStatus("UPLOADED");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/download")
    public ResponseEntity<FileDownloadResponse> getDownloadUrl(
            @Valid @RequestBody FileDownloadRequest request) {

        try {
            FileDownloadResponse response = fileProcessingService.generateDownloadUrl(
                    request.getCaseId(), request.getFileName());

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}