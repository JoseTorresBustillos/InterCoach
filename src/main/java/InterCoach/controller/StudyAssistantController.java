package InterCoach.controller;

import InterCoach.dto.StudyAssistantRequest;
import InterCoach.dto.StudyAssistantResponse;
import InterCoach.service.StudyAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study-assistant")
public class StudyAssistantController {

    private final StudyAssistantService studyAssistantService;

    public StudyAssistantController(StudyAssistantService studyAssistantService) {
        this.studyAssistantService = studyAssistantService;
    }

    @PostMapping("/ask")
    public StudyAssistantResponse askQuestion(@Valid @RequestBody StudyAssistantRequest request) {
        return studyAssistantService.askQuestion(request.getQuestion());
    }
}