package intercoach.controller;

import intercoach.dto.MockInterviewRequest;
import intercoach.dto.MockInterviewResponse;
import intercoach.security.UserAccessService;
import intercoach.service.MockInterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;
    private final UserAccessService userAccessService;

    public MockInterviewController(
            MockInterviewService mockInterviewService,
            UserAccessService userAccessService
    ) {
        this.mockInterviewService = mockInterviewService;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/api/users/{userId}/mock-interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public MockInterviewResponse startInterview(
            @PathVariable Long userId,
            @Valid @RequestBody MockInterviewRequest request,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return mockInterviewService.startInterview(userId, request);
    }

    @GetMapping("/api/mock-interviews/{sessionId}")
    public MockInterviewResponse getInterview(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessMockInterview(
                sessionId,
                authentication
        );

        return mockInterviewService.getInterview(sessionId);
    }

    @GetMapping("/api/users/{userId}/mock-interviews")
    public List<MockInterviewResponse> getInterviewsForUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return mockInterviewService.getInterviewsForUser(userId);
    }

    @PatchMapping("/api/mock-interviews/{sessionId}/complete")
    public MockInterviewResponse completeInterview(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessMockInterview(
                sessionId,
                authentication
        );

        return mockInterviewService.completeInterview(sessionId);
    }

    @PatchMapping("/api/mock-interviews/{sessionId}/abandon")
    public MockInterviewResponse abandonInterview(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessMockInterview(
                sessionId,
                authentication
        );

        return mockInterviewService.abandonInterview(sessionId);
    }
}
