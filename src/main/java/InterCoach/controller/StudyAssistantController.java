package InterCoach.controller;

import InterCoach.dto.StudyAssistantRequest;
import InterCoach.dto.StudyAssistantResponse;
import InterCoach.service.StudyAssistantService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study-assistant")
public class StudyAssistantController {

    private final StudyAssistantService studyAssistantService;

    public StudyAssistantController(StudyAssistantService studyAssistantService) {
        this.studyAssistantService = studyAssistantService;
    }

    @PostMapping("/ask")
    public StudyAssistantResponse askQuestion(
            @Valid @RequestBody StudyAssistantRequest request,
            Authentication authentication
    ) {
        return studyAssistantService.askQuestionForUser(
                request.getQuestion(),
                authentication.getName()
        );
    }
}
