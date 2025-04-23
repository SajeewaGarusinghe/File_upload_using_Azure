package org.iit.demo1.controller;

//import com.azure.messaging.eventgrid.EventGridEvent;
//import org.iit.demo1.service.EventHandlerService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/events")
//public class EventController {
//
//    private static final Logger log = LoggerFactory.getLogger(EventController.class);
//    private final EventHandlerService eventHandlerService;
//
//    @Autowired
//    public EventController(EventHandlerService eventHandlerService) {
//        this.eventHandlerService = eventHandlerService;
//    }
//
//    @PostMapping("/malware-scan")
//    public ResponseEntity<Void> handleMalwareScanEvent(@RequestBody EventGridEvent event) {
//        log.warn("scanned event returned : {}",event);
//        eventHandlerService.handleMalwareScanEvent(event);
//        return ResponseEntity.ok().build();
//    }
//}



import com.azure.messaging.eventgrid.EventGridEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iit.demo1.service.EventHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    private final EventHandlerService eventHandlerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventController(
            EventHandlerService eventHandlerService,
            ObjectMapper objectMapper) {
        this.eventHandlerService = eventHandlerService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/malware-scan")
    public ResponseEntity<?> handleEventGridNotification(@RequestBody String requestBody) {
        try {
            log.warn("scanned event returned : {}",requestBody);
            // Check if this is a validation event
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            // Handle subscription validation
            if (jsonNode.isArray() && jsonNode.size() > 0) {
                JsonNode firstEvent = jsonNode.get(0);

                // Check if this is a validation event
                if (firstEvent.has("eventType") &&
                        "Microsoft.EventGrid.SubscriptionValidationEvent".equals(firstEvent.get("eventType").asText())) {

                    // Extract validationCode and create validationResponse
                    String validationCode = firstEvent.get("data").get("validationCode").asText();
                    Map<String, String> validationResponse = Map.of("validationResponse", validationCode);

                    return ResponseEntity.ok(validationResponse);
                }
            }

            // Process regular events
            EventGridEvent[] events = objectMapper.readValue(requestBody, EventGridEvent[].class);

            for (EventGridEvent event : events) {
                if ("Microsoft.Security.MalwareScanningResult".equals(event.getEventType())) {
                    eventHandlerService.handleMalwareScanEvent(event);
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error processing event: " + e.getMessage());
        }
    }
}