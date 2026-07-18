package InterCoach.controller;

import InterCoach.dto.MockInterviewRequest;
import InterCoach.dto.MockInterviewResponse;
import InterCoach.service.MockInterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    public MockInterviewController(
            MockInterviewService mockInterviewService
    ) {
        this.mockInterviewService = mockInterviewService;
    }

    @PostMapping("/api/users/{userId}/mock-interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public MockInterviewResponse startInterview(
            @PathVariable Long userId,
            @Valid @RequestBody MockInterviewRequest request
    ) {
        return mockInterviewService.startInterview(userId, request);
    }

    @GetMapping("/api/mock-interviews/{sessionId}")
    public MockInterviewResponse getInterview(
            @PathVariable Long sessionId
    ) {
        return mockInterviewService.getInterview(sessionId);
    }

    @GetMapping("/api/users/{userId}/mock-interviews")
    public List<MockInterviewResponse> getInterviewsForUser(
            @PathVariable Long userId
    ) {
        return mockInterviewService.getInterviewsForUser(userId);
    }

    @PatchMapping("/api/mock-interviews/{sessionId}/complete")
    public MockInterviewResponse completeInterview(
            @PathVariable Long sessionId
    ) {
        return mockInterviewService.completeInterview(sessionId);
    }

    @PatchMapping("/api/mock-interviews/{sessionId}/abandon")
    public MockInterviewResponse abandonInterview(
            @PathVariable Long sessionId
    ) {
        return mockInterviewService.abandonInterview(sessionId);
    }
}