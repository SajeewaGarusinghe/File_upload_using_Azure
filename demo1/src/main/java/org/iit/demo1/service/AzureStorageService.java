package org.iit.demo1.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AzureStorageService {

    private final BlobContainerClient quarantineContainerClient;
    private final BlobContainerClient cleanContainerClient;

    public AzureStorageService(
            @Value("${azure.storage.quarantine.connection-string}") String quarantineConnectionString,
            @Value("${azure.storage.quarantine.container-name}") String quarantineContainerName,
            @Value("${azure.storage.clean.connection-string}") String cleanConnectionString,
            @Value("${azure.storage.clean.container-name}") String cleanContainerName) {

        BlobServiceClient quarantineServiceClient = new BlobServiceClientBuilder()
                .connectionString(quarantineConnectionString)
                .buildClient();

        BlobServiceClient cleanServiceClient = new BlobServiceClientBuilder()
                .connectionString(cleanConnectionString)
                .buildClient();

        this.quarantineContainerClient = quarantineServiceClient.getBlobContainerClient(quarantineContainerName);
        this.cleanContainerClient = cleanServiceClient.getBlobContainerClient(cleanContainerName);
    }

    public String uploadToQuarantine(String caseId, MultipartFile file) throws IOException {
        // Create a unique file name to prevent collisions
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + fileExtension;
        String blobPath = caseId + "/" + fileName;

        BlobClient blobClient = quarantineContainerClient.getBlobClient(blobPath);

        // Set content type
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType());

        // Upload file
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        blobClient.setHttpHeaders(headers);

        return fileName;
    }

    public void moveToCleanStorage(String caseId, String fileName, String contentType) throws IOException {
        // Source blob in quarantine
        String sourceBlobPath = caseId + "/" + fileName;
        BlobClient sourceBlobClient = quarantineContainerClient.getBlobClient(sourceBlobPath);

        // Destination blob in clean storage
        String destinationBlobPath = caseId + "/" + fileName;
        BlobClient destinationBlobClient = cleanContainerClient.getBlobClient(destinationBlobPath);

        // Set content type
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(contentType);

        // Copy blob from quarantine to clean storage
        destinationBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl());
        destinationBlobClient.setHttpHeaders(headers);

        // Delete from quarantine after successful copy
        sourceBlobClient.delete();
    }

    public String generatePresignedUrl(String caseId, String fileName, String contentType) {
        String blobPath = caseId + "/" + fileName;
        BlobClient blobClient = cleanContainerClient.getBlobClient(blobPath);

        // Define SAS token permissions and expiry
        BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true);

        OffsetDateTime expiryTime = OffsetDateTime.now().plus(1, ChronoUnit.HOURS);

        // Generate SAS token
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission)
                .setContentType(contentType);

        // Create the presigned URL
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}