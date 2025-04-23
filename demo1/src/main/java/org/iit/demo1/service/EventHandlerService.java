package org.iit.demo1.service;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iit.demo1.dto.MalwareScanningEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class EventHandlerService {

    private final FileProcessingService fileProcessingService;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventHandlerService(
            FileProcessingService fileProcessingService,
            ObjectMapper objectMapper) {
        this.fileProcessingService = fileProcessingService;
        this.objectMapper = objectMapper;
    }

    public void handleMalwareScanEvent(EventGridEvent event) {
        try {
            // Parse event data
            MalwareScanningEventData eventData = objectMapper.convertValue(
                    event.getData(), MalwareScanningEventData.class);

            // Extract file path from blob URL
            String blobPath = extractBlobPathFromUrl(eventData.getUrl());
            String[] pathParts = blobPath.split("/", 2);

            if (pathParts.length == 2) {
                String caseId = pathParts[0];
                String fileName = pathParts[1];

                // Process based on scan result
                if ("Clean".equals(eventData.getScanResult())) {
                    fileProcessingService.processCleanFile(caseId, fileName);
                } else {
                    fileProcessingService.processMaliciousFile(caseId, fileName);
                }
            }
        } catch (Exception e) {
            // Handle error
            e.printStackTrace();
        }
    }

    private String extractBlobPathFromUrl(String url) {
        // Remove storage account URL prefix and extract blob path
        // Example URL: https://quarantinestorage.blob.core.windows.net/quarantine-files/case123/file.pdf

        int containerNameIndex = url.indexOf("quarantine-files/");
        if (containerNameIndex != -1) {
            return url.substring(containerNameIndex + "quarantine-files/".length());
        }
        return "";
    }
}