package intercoach.assistant.controller;

import intercoach.assistant.dto.StudyAssistantRequest;
import intercoach.assistant.dto.StudyAssistantResponse;
import intercoach.assistant.service.StudyAssistantService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
