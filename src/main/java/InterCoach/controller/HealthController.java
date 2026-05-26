package InterCoach.controller;

// Simple health-check endpoint used to confirm the API is running.

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "running",
                "service", "CodeCoach API",
                "timestamp", Instant.now().toString()
        );
    }
}