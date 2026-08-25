package intercoach.controller;

import intercoach.dto.CodeExecutionOperationsResponse;
import intercoach.security.UserAccessService;
import intercoach.service.CodeExecutionOperationsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/execution")
public class CodeExecutionOperationsController {

    private final CodeExecutionOperationsService operationsService;
    private final UserAccessService userAccessService;

    public CodeExecutionOperationsController(
            CodeExecutionOperationsService operationsService,
            UserAccessService userAccessService
    ) {
        this.operationsService = operationsService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/status")
    public CodeExecutionOperationsResponse getOperationsStatus(
            Authentication authentication
    ) {
        userAccessService.assertAdmin(authentication);

        return operationsService.getOperationsStatus();
    }
}
