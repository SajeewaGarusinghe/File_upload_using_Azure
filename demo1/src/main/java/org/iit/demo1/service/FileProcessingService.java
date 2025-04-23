package org.iit.demo1.service;

import org.iit.demo1.dto.FileDownloadResponse;
import org.iit.demo1.entity.FileMetadata;
import org.iit.demo1.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FileProcessingService {

    private final FileMetadataRepository fileMetadataRepository;
    private final AzureStorageService azureStorageService;

    @Autowired
    public FileProcessingService(
            FileMetadataRepository fileMetadataRepository,
            AzureStorageService azureStorageService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.azureStorageService = azureStorageService;
    }

    public FileMetadata uploadFile(String caseId, MultipartFile file) throws IOException {
        // Upload file to quarantine storage
        String storedFileName = azureStorageService.uploadToQuarantine(caseId, file);

        // Create and save metadata
        FileMetadata metadata = new FileMetadata();
        metadata.setCaseId(caseId);
        metadata.setFileName(storedFileName);
        metadata.setOriginalFileName(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setFileSize(file.getSize());
        metadata.setUploadDate(LocalDateTime.now());
        metadata.setScanStatus("PENDING");

        return fileMetadataRepository.save(metadata);
    }

    public void processCleanFile(String caseId, String fileName) throws IOException {
        Optional<FileMetadata> metadataOpt = fileMetadataRepository.findByCaseIdAndFileName(caseId, fileName);

        if (metadataOpt.isPresent()) {
            FileMetadata metadata = metadataOpt.get();

            // Move file from quarantine to clean storage
            azureStorageService.moveToCleanStorage(caseId, fileName, metadata.getContentType());

            // Update metadata
            metadata.setScanStatus("CLEAN");
            metadata.setScanDate(LocalDateTime.now());
            fileMetadataRepository.save(metadata);
        }
    }

    public void processMaliciousFile(String caseId, String fileName) {
        Optional<FileMetadata> metadataOpt = fileMetadataRepository.findByCaseIdAndFileName(caseId, fileName);

        if (metadataOpt.isPresent()) {
            FileMetadata metadata = metadataOpt.get();
            metadata.setScanStatus("MALICIOUS");
            metadata.setScanDate(LocalDateTime.now());
            fileMetadataRepository.save(metadata);
            // Do not move to clean storage - file remains in quarantine
        }
    }

    public FileDownloadResponse generateDownloadUrl(String caseId, String fileName) {
        Optional<FileMetadata> metadataOpt = fileMetadataRepository.findByCaseIdAndFileName(caseId, fileName);

        if (metadataOpt.isPresent() && "CLEAN".equals(metadataOpt.get().getScanStatus())) {
            FileMetadata metadata = metadataOpt.get();

            // Generate presigned URL
            String presignedUrl = azureStorageService.generatePresignedUrl(
                    caseId, fileName, metadata.getContentType());

            // Create response
            FileDownloadResponse response = new FileDownloadResponse();
            response.setPresignedUrl(presignedUrl);
            response.setFileName(metadata.getOriginalFileName());
            response.setContentType(metadata.getContentType());
            response.setExpiryTimeInSeconds(3600); // 1 hour

            return response;
        }

        return null;
    }
}